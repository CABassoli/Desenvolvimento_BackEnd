package com.ecommerce.service;

import com.ecommerce.domain.*;
import com.ecommerce.dto.DadosCartaoDTO;
import com.ecommerce.dto.SimulacaoPagamentoRequestDTO;
import com.ecommerce.dto.SimulacaoPagamentoResponseDTO;
import com.ecommerce.dto.request.PagamentoRequestDTO;
import com.ecommerce.dto.response.PagamentoResponseDTO;
import com.ecommerce.integration.*;
import com.ecommerce.mapper.PagamentoMapper;
import com.ecommerce.repository.PagamentoRepository;
import com.ecommerce.repository.PedidoRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para processamento de pagamentos
 * Suporta PIX, Cartão de Crédito e Boleto
 */
public class PagamentoService {
    
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final PagamentoMapper pagamentoMapper;
    private final NotificacaoService notificacaoService;
    private StripePaymentService stripePaymentService; // Lazy initialization
    
    public PagamentoService(PagamentoRepository pagamentoRepository,
                           PedidoRepository pedidoRepository,
                           PagamentoMapper pagamentoMapper,
                           NotificacaoService notificacaoService) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pagamentoMapper = pagamentoMapper;
        this.notificacaoService = notificacaoService;
        // StripePaymentService inicializado apenas quando necessário
    }
    
    /**
     * Obtém instância do StripePaymentService com lazy loading
     */
    private StripePaymentService getStripePaymentService() {
        if (stripePaymentService == null) {
            stripePaymentService = new StripePaymentService();
        }
        return stripePaymentService;
    }
    
    /**
     * Processa pagamento PIX
     */
    public PagamentoResponseDTO processarPagamentoPix(UUID pedidoId, PagamentoRequestDTO requestDTO) {
        // Busca e valida pedido
        Pedido pedido = validarPedidoParaPagamento(pedidoId, requestDTO.getValor());
        
        // Cria pagamento PIX
        PagamentoPix pagamento = new PagamentoPix();
        pagamento.setId(UUID.randomUUID());
        pagamento.setPedido(pedido);
        pagamento.setValor(requestDTO.getValor());
        
        // Processa pagamento PIX via Stripe
        StripePixPaymentRequest stripeRequest = StripePixPaymentRequest.from(
            pedido.getId(), 
            pedido.getCliente().getId(), 
            requestDTO.getValor()
        );
        
        StripePaymentResult stripeResult = getStripePaymentService().processPixPayment(stripeRequest);
        boolean pagamentoAprovado = stripeResult.isSuccess();
        
        if (pagamentoAprovado) {
            String txid = stripeResult.getTransactionId() != null ? 
                stripeResult.getTransactionId() : gerarTxidSimulado();
            pagamento.setTxid(txid);
        }
        
        if (pagamentoAprovado) {
            // Salva pagamento
            Pagamento savedPagamento = pagamentoRepository.save(pagamento);
            
            // Atualiza status do pedido para PAGO
            pedido.setStatus(StatusPedido.PAGO);
            pedidoRepository.save(pedido);
            
            // Envia notificação
            notificacaoService.criarNotificacaoStatus(pedido.getCliente(), pedido, StatusPedido.PAGO);
            
            return pagamentoMapper.toResponseDTO(savedPagamento);
        } else {
            throw new RuntimeException("Pagamento PIX rejeitado");
        }
    }
    
    
    /**
     * Processa pagamento com boleto bancário
     */
    public PagamentoResponseDTO processarPagamentoBoleto(UUID pedidoId, PagamentoRequestDTO requestDTO) {
        // Busca e valida pedido
        Pedido pedido = validarPedidoParaPagamento(pedidoId, requestDTO.getValor());
        
        // Cria pagamento boleto
        PagamentoBoleto pagamento = new PagamentoBoleto();
        pagamento.setId(UUID.randomUUID());
        pagamento.setPedido(pedido);
        pagamento.setValor(requestDTO.getValor());
        
        // Gera boleto via Stripe
        StripeBoletoRequest stripeRequest = StripeBoletoRequest.from(
            pedido.getId(),
            pedido.getCliente().getId(),
            requestDTO.getValor(),
            pedido.getCliente().getNome(),
            pedido.getCliente().getEmail()
        );
        
        StripePaymentResult stripeResult = getStripePaymentService().generateBoleto(stripeRequest);
        String linhaDigitavel = stripeResult.getBoletoLine() != null ? 
            stripeResult.getBoletoLine() : gerarLinhaDigitavelSimulada();
        pagamento.setLinhaDigitavel(linhaDigitavel);
        
        // Salva pagamento (boleto não é processado imediatamente)
        Pagamento savedPagamento = pagamentoRepository.save(pagamento);
        
        // Pedido permanece PROCESSANDO até confirmação do boleto
        // Envia notificação com informações do boleto
        // TODO: Implementar notificação específica para boleto
        
        return pagamentoMapper.toResponseDTO(savedPagamento);
    }
    
    /**
     * Confirma pagamento de boleto (simulação de webhook bancário)
     */
    public PagamentoResponseDTO confirmarPagamentoBoleto(String linhaDigitavel) {
        // Busca pagamento por linha digitável
        Optional<PagamentoBoleto> pagamentoOpt = pagamentoRepository.findBoletoByLinhaDigitavel(linhaDigitavel);
        if (pagamentoOpt.isEmpty()) {
            throw new RuntimeException("Boleto não encontrado");
        }
        
        PagamentoBoleto pagamento = pagamentoOpt.get();
        Pedido pedido = pagamento.getPedido();
        
        // Verifica se pedido ainda está processando
        if (pedido.getStatus() != StatusPedido.PROCESSANDO) {
            throw new RuntimeException("Pedido já foi processado ou cancelado");
        }
        
        // Atualiza status do pedido para PAGO
        pedido.setStatus(StatusPedido.PAGO);
        pedidoRepository.save(pedido);
        
        // Envia notificação
        notificacaoService.criarNotificacaoStatus(pedido.getCliente(), pedido, StatusPedido.PAGO);
        
        return pagamentoMapper.toResponseDTO(pagamento);
    }
    
    /**
     * Busca pagamento por ID
     */
    public Optional<PagamentoResponseDTO> findById(UUID id) {
        return pagamentoRepository.findById(id)
                .map(pagamentoMapper::toResponseDTO);
    }
    
    /**
     * Busca pagamento por pedido
     */
    public Optional<PagamentoResponseDTO> findByPedido(UUID pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId)
                .map(pagamentoMapper::toResponseDTO);
    }
    
    /**
     * Lista todos os pagamentos
     */
    public List<PagamentoResponseDTO> findAll() {
        return pagamentoRepository.findAll().stream()
                .map(pagamentoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista pagamentos PIX
     */
    public List<PagamentoResponseDTO> findAllPix() {
        return pagamentoRepository.findAllPix().stream()
                .map(pagamento -> pagamentoMapper.toResponseDTO(pagamento))
                .toList();
    }
    
    /**
     * Lista pagamentos Cartão
     */
    public List<PagamentoResponseDTO> findAllCartao() {
        return pagamentoRepository.findAllCartao().stream()
                .map(pagamento -> pagamentoMapper.toResponseDTO(pagamento))
                .toList();
    }
    
    /**
     * Lista pagamentos Boleto
     */
    public List<PagamentoResponseDTO> findAllBoleto() {
        return pagamentoRepository.findAllBoleto().stream()
                .map(pagamento -> pagamentoMapper.toResponseDTO(pagamento))
                .toList();
    }
    
    /**
     * Calcula total de pagamentos
     */
    public BigDecimal calcularTotalPagamentos() {
        return pagamentoRepository.sumTotal();
    }
    
    /**
     * Conta total de pagamentos
     */
    public long count() {
        return pagamentoRepository.count();
    }
    
    // Métodos privados auxiliares
    
    /**
     * Valida pedido para pagamento
     */
    private Pedido validarPedidoParaPagamento(UUID pedidoId, BigDecimal valor) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (pedidoOpt.isEmpty()) {
            throw new RuntimeException("Pedido não encontrado");
        }
        
        Pedido pedido = pedidoOpt.get();
        
        // Verifica se pedido está no status correto
        if (pedido.getStatus() != StatusPedido.PROCESSANDO) {
            throw new RuntimeException("Pedido não está aguardando pagamento");
        }
        
        // Verifica se já existe pagamento para o pedido
        Optional<Pagamento> pagamentoExistente = pagamentoRepository.findByPedidoId(pedidoId);
        if (pagamentoExistente.isPresent()) {
            throw new RuntimeException("Pedido já possui pagamento processado");
        }
        
        // Valida valor
        if (valor.compareTo(pedido.getValorTotal()) != 0) {
            throw new RuntimeException("Valor do pagamento não confere com valor do pedido");
        }
        
        return pedido;
    }
    
    
    /**
     * Simula processamento PIX (sempre aprova em desenvolvimento)
     */
    private boolean simularProcessamentoPix(String txid, BigDecimal valor) {
        // Simula latência da API PIX
        try {
            Thread.sleep(1000); // 1 segundo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Em desenvolvimento, sempre aprova PIX
        return true;
    }
    
    
    /**
     * Gera TXID simulado para PIX
     */
    private String gerarTxidSimulado() {
        return "E" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Gera token simulado para cartão
     */
    private String gerarTokenCartaoSimulado() {
        return "TK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
    
    /**
     * Gera linha digitável simulada para boleto
     */
    private String gerarLinhaDigitavelSimulada() {
        // Formato simplificado: BBBBBNNNNNNNNNNDDDDDDDDDDDVVVVVVVVVVVVVVV
        long timestamp = System.currentTimeMillis();
        return String.format("03399%010d1%014d", timestamp % 10000000000L, timestamp);
    }
    
    /**
     * Simula pagamento com novos DTOs - aceita dadosCartao para tokenização
     */
    public SimulacaoPagamentoResponseDTO simularPagamento(SimulacaoPagamentoRequestDTO request, UUID userId) {
        SimulacaoPagamentoResponseDTO response = new SimulacaoPagamentoResponseDTO();
        response.setValor(request.getValor());
        response.setDataProcessamento(Instant.now());
        response.setNsu(gerarNSU());
        
        // Converter método para enum
        MetodoPagamento metodo;
        try {
            metodo = MetodoPagamento.valueOf(request.getMetodo().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Método de pagamento inválido: " + request.getMetodo());
        }
        response.setMetodo(metodo);
        
        if (metodo == MetodoPagamento.CARTAO) {
            if (request.getDadosCartao() == null) {
                throw new IllegalArgumentException("Dados do cartão são obrigatórios para método CARTAO");
            }
            
            DadosCartaoDTO dadosCartao = request.getDadosCartao();
            
            // Validar dados básicos do cartão
            validarDadosCartaoBasicos(dadosCartao);
            
            // Gerar token baseado nos dados do cartão (SHA-256)
            String tokenCartao = gerarTokenCartao(dadosCartao);
            
            // Obter últimos 4 dígitos do cartão
            String ultimosDigitos = obterUltimosDigitos(dadosCartao.getNumero());
            
            // Simulação: 90% aprovado, 10% rejeitado
            long seed = userId.hashCode() + System.currentTimeMillis() / 1000;
            boolean aprovado = (seed % 10) != 0; // 10% de chance de ser múltiplo de 10
            
            if (aprovado) {
                response.setStatus(StatusPagamento.APROVADO);
                response.setTokenCartao(tokenCartao);
                response.setBandeira(dadosCartao.getBandeira().toUpperCase());
                response.setUltimosDigitosCartao(ultimosDigitos);
                response.setMensagem("Pagamento autorizado");
                response.setTransacaoId("TXN" + System.currentTimeMillis());
            } else {
                response.setStatus(StatusPagamento.RECUSADO);
                response.setMensagem("Pagamento recusado pela operadora");
                response.setBandeira(dadosCartao.getBandeira().toUpperCase());
                response.setUltimosDigitosCartao(ultimosDigitos);
            }
            
        } else if (metodo == MetodoPagamento.BOLETO) {
            // Boleto sempre pendente
            response.setStatus(StatusPagamento.PENDENTE);
            response.setLinhaDigitavel(gerarLinhaDigitavel(request.getValor()));
            response.setMensagem("Boleto gerado");
            response.setDataVencimento(Instant.now().plusSeconds(86400 * 7)); // Vencimento em 7 dias
            
        } else if (metodo == MetodoPagamento.PIX) {
            // PIX sempre aprovado em simulação
            response.setStatus(StatusPagamento.APROVADO);
            response.setTransacaoId("PIX" + gerarTxidSimulado());
            response.setMensagem("Pagamento PIX aprovado");
            
        } else {
            throw new IllegalArgumentException("Método de pagamento não suportado: " + metodo);
        }
        
        return response;
    }
    
    /**
     * Gera token de cartão baseado em SHA-256 dos dados sensíveis
     * NUNCA expor ou logar os dados originais
     */
    private String gerarTokenCartao(DadosCartaoDTO dados) {
        try {
            // Concatenar dados sensíveis para hash
            String dadosConcatenados = dados.getNumero() + "|" + dados.getValidade() + "|" + dados.getCvv();
            
            // Gerar hash SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dadosConcatenados.getBytes(StandardCharsets.UTF_8));
            
            // Converter para hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Retornar token no formato: tok_ + primeiros 32 caracteres do hash
            return "tok_" + hexString.toString().substring(0, 32);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar token do cartão", e);
        }
    }
    
    /**
     * Valida dados básicos do cartão sem logar informações sensíveis
     */
    private void validarDadosCartaoBasicos(DadosCartaoDTO dados) {
        if (dados.getNumero() == null || dados.getNumero().length() < 13 || dados.getNumero().length() > 19) {
            throw new IllegalArgumentException("Número do cartão inválido");
        }
        if (dados.getCvv() == null || dados.getCvv().length() < 3 || dados.getCvv().length() > 4) {
            throw new IllegalArgumentException("CVV inválido");
        }
        if (dados.getValidade() == null || !dados.getValidade().matches("\\d{2}/\\d{2}")) {
            throw new IllegalArgumentException("Validade inválida");
        }
        if (dados.getBandeira() == null || dados.getBandeira().trim().isEmpty()) {
            throw new IllegalArgumentException("Bandeira é obrigatória");
        }
    }
    
    /**
     * Gera NSU único
     */
    public String gerarNSU() {
        // NSU formato: YYYYMMDDHHMMSS + 6 dígitos aleatórios
        long timestamp = System.currentTimeMillis();
        return String.format("NSU%014d%06d", timestamp, (int)(Math.random() * 999999));
    }
    
    /**
     * Gera linha digitável para boleto com valor
     */
    public String gerarLinhaDigitavel(BigDecimal valor) {
        // Formato simulado de boleto bancário
        long valorCentavos = valor.multiply(BigDecimal.valueOf(100)).longValue();
        long timestamp = System.currentTimeMillis();
        
        // Simula código de barras do boleto
        // Banco(3) + Moeda(1) + DV(1) + Vencimento(4) + Valor(10) + Nosso Número(17) + Código(8) + DV(1)
        String banco = "033"; // Santander (exemplo)
        String moeda = "9";
        String dv1 = String.valueOf((timestamp % 9) + 1);
        String vencimento = String.format("%04d", (timestamp / 1000) % 10000);
        String valorFormatado = String.format("%010d", valorCentavos);
        String nossoNumero = String.format("%017d", timestamp);
        String codigo = String.format("%08d", (int)(Math.random() * 99999999));
        String dvFinal = String.valueOf((valorCentavos % 9) + 1);
        
        // Monta linha digitável
        String linhaDigitavel = banco + moeda + "." + dv1 + vencimento + 
                               " " + valorFormatado + 
                               " " + nossoNumero.substring(0, 5) + "." + nossoNumero.substring(5, 11) + 
                               " " + nossoNumero.substring(11) + codigo + dvFinal;
        
        return linhaDigitavel;
    }
    
    /**
     * Obtém os últimos 4 dígitos do cartão para display seguro
     */
    private String obterUltimosDigitos(String numeroCartao) {
        if (numeroCartao == null || numeroCartao.length() < 4) {
            return "****";
        }
        return numeroCartao.substring(numeroCartao.length() - 4);
    }
}