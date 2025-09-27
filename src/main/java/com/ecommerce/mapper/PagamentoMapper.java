package com.ecommerce.mapper;

import com.ecommerce.domain.Pagamento;
import com.ecommerce.domain.PagamentoBoleto;
import com.ecommerce.domain.PagamentoCartao;
import com.ecommerce.domain.PagamentoPix;
import com.ecommerce.dto.request.PagamentoRequestDTO;
import com.ecommerce.dto.response.PagamentoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversão entre Pagamento e DTOs
 */
@Mapper(componentModel = "default")
public interface PagamentoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    @Mapping(source = "pedido.id", target = "pedidoId")
    @Mapping(target = "tipoPagamento", expression = "java(getTipoPagamento(pagamento))")
    @Mapping(target = "tokenCartao", expression = "java(getTokenCartao(pagamento))")
    @Mapping(target = "bandeira", expression = "java(getBandeira(pagamento))")
    @Mapping(target = "linhaDigitavel", expression = "java(getLinhaDigitavel(pagamento))")
    @Mapping(target = "txid", expression = "java(getTxid(pagamento))")
    PagamentoResponseDTO toResponseDTO(Pagamento pagamento);
    
    // Nota: Pagamento é abstrato, use métodos específicos nas subclasses
    
    // Métodos auxiliares para determinar campos específicos do tipo de pagamento
    default String getTipoPagamento(Pagamento pagamento) {
        if (pagamento instanceof PagamentoCartao) return "CARTAO";
        if (pagamento instanceof PagamentoBoleto) return "BOLETO";
        if (pagamento instanceof PagamentoPix) return "PIX";
        return "UNKNOWN";
    }
    
    default String getTokenCartao(Pagamento pagamento) {
        return pagamento instanceof PagamentoCartao ? ((PagamentoCartao) pagamento).getTokenCartao() : null;
    }
    
    default String getBandeira(Pagamento pagamento) {
        return pagamento instanceof PagamentoCartao ? ((PagamentoCartao) pagamento).getBandeira() : null;
    }
    
    default String getLinhaDigitavel(Pagamento pagamento) {
        return pagamento instanceof PagamentoBoleto ? ((PagamentoBoleto) pagamento).getLinhaDigitavel() : null;
    }
    
    default String getTxid(Pagamento pagamento) {
        return pagamento instanceof PagamentoPix ? ((PagamentoPix) pagamento).getTxid() : null;
    }
}