package com.ecommerce.service;

import com.ecommerce.domain.Categoria;
import com.ecommerce.domain.Produto;
import com.ecommerce.dto.request.ProdutoRequestDTO;
import com.ecommerce.dto.response.ProdutoResponseDTO;
import com.ecommerce.mapper.ProdutoMapper;
import com.ecommerce.repository.CategoriaRepository;
import com.ecommerce.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de produtos
 */
public class ProdutoService {
    
    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoMapper produtoMapper;
    
    public ProdutoService(ProdutoRepository produtoRepository,
                         CategoriaRepository categoriaRepository,
                         ProdutoMapper produtoMapper) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoMapper = produtoMapper;
    }
    
    /**
     * Cria novo produto
     */
    public ProdutoResponseDTO create(ProdutoRequestDTO requestDTO) {
        // Verifica se código de barras já existe
        if (produtoRepository.existsByCodigoBarras(requestDTO.getCodigoBarras())) {
            throw new RuntimeException("Já existe um produto com este código de barras");
        }
        
        // Verifica se categoria existe
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(requestDTO.getCategoriaId());
        if (categoriaOpt.isEmpty()) {
            throw new RuntimeException("Categoria não encontrada");
        }
        
        // Valida preço
        if (requestDTO.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Preço deve ser maior que zero");
        }
        
        // Cria novo produto
        Produto produto = produtoMapper.toEntity(requestDTO);
        produto.setId(UUID.randomUUID());
        produto.setCategoria(categoriaOpt.get());
        
        Produto savedProduto = produtoRepository.save(produto);
        
        return produtoMapper.toResponseDTO(savedProduto);
    }
    
    /**
     * Busca produto por ID
     */
    public Optional<ProdutoResponseDTO> findById(UUID id) {
        return produtoRepository.findById(id)
                .map(produtoMapper::toResponseDTO);
    }
    
    /**
     * Busca produto por código de barras
     */
    public Optional<ProdutoResponseDTO> findByCodigoBarras(String codigoBarras) {
        return produtoRepository.findByCodigoBarras(codigoBarras)
                .map(produtoMapper::toResponseDTO);
    }
    
    /**
     * Lista produtos por categoria
     */
    public List<ProdutoResponseDTO> findByCategoria(UUID categoriaId) {
        return produtoRepository.findByCategoriaId(categoriaId).stream()
                .map(produtoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Busca produtos por nome (busca parcial)
     */
    public List<ProdutoResponseDTO> findByNome(String nome) {
        return produtoRepository.findByNomeContaining(nome).stream()
                .map(produtoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista produtos por faixa de preço
     */
    public List<ProdutoResponseDTO> findByPrecoRange(BigDecimal precoMin, BigDecimal precoMax) {
        if (precoMin.compareTo(precoMax) > 0) {
            throw new RuntimeException("Preço mínimo não pode ser maior que preço máximo");
        }
        
        return produtoRepository.findByPrecoRange(precoMin, precoMax).stream()
                .map(produtoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista todos os produtos
     */
    public List<ProdutoResponseDTO> findAll() {
        return produtoRepository.findAll().stream()
                .map(produtoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Atualiza produto
     */
    public ProdutoResponseDTO update(UUID id, ProdutoRequestDTO requestDTO) {
        Optional<Produto> produtoOpt = produtoRepository.findById(id);
        if (produtoOpt.isEmpty()) {
            throw new RuntimeException("Produto não encontrado");
        }
        
        Produto produto = produtoOpt.get();
        
        // Verifica se novo código de barras já existe em outro produto
        Optional<Produto> existingProduto = produtoRepository.findByCodigoBarras(requestDTO.getCodigoBarras());
        if (existingProduto.isPresent() && !existingProduto.get().getId().equals(id)) {
            throw new RuntimeException("Já existe um produto com este código de barras");
        }
        
        // Verifica se categoria existe
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(requestDTO.getCategoriaId());
        if (categoriaOpt.isEmpty()) {
            throw new RuntimeException("Categoria não encontrada");
        }
        
        // Valida preço
        if (requestDTO.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Preço deve ser maior que zero");
        }
        
        // Atualiza dados
        produtoMapper.updateEntity(requestDTO, produto);
        produto.setCategoria(categoriaOpt.get());
        
        Produto savedProduto = produtoRepository.save(produto);
        
        return produtoMapper.toResponseDTO(savedProduto);
    }
    
    /**
     * Remove produto
     */
    public void delete(UUID id) {
        Optional<Produto> produtoOpt = produtoRepository.findById(id);
        if (produtoOpt.isEmpty()) {
            throw new RuntimeException("Produto não encontrado");
        }
        
        // TODO: Verificar se produto está em carrinhos ou pedidos ativos
        
        produtoRepository.deleteById(id);
    }
    
    /**
     * Verifica se produto existe
     */
    public boolean exists(UUID id) {
        return produtoRepository.findById(id).isPresent();
    }
    
    /**
     * Verifica se código de barras já está em uso
     */
    public boolean existsByCodigoBarras(String codigoBarras) {
        return produtoRepository.existsByCodigoBarras(codigoBarras);
    }
    
    public long count() {
        return produtoRepository.count();
    }
}