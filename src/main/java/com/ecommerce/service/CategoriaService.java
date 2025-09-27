package com.ecommerce.service;

import com.ecommerce.domain.Categoria;
import com.ecommerce.dto.request.CategoriaRequestDTO;
import com.ecommerce.dto.response.CategoriaResponseDTO;
import com.ecommerce.mapper.CategoriaMapper;
import com.ecommerce.repository.CategoriaRepository;
import com.ecommerce.repository.ProdutoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de categorias de produtos
 */
public class CategoriaService {
    
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final CategoriaMapper categoriaMapper;
    
    public CategoriaService(CategoriaRepository categoriaRepository, 
                           ProdutoRepository produtoRepository,
                           CategoriaMapper categoriaMapper) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.categoriaMapper = categoriaMapper;
    }
    
    /**
     * Cria nova categoria
     */
    public CategoriaResponseDTO create(CategoriaRequestDTO requestDTO) {
        // Verifica se nome já existe
        if (categoriaRepository.existsByNome(requestDTO.getNome())) {
            throw new RuntimeException("Já existe uma categoria com este nome");
        }
        
        // Cria nova categoria
        Categoria categoria = categoriaMapper.toEntity(requestDTO);
        
        Categoria savedCategoria = categoriaRepository.save(categoria);
        
        return categoriaMapper.toResponseDTO(savedCategoria);
    }
    
    /**
     * Busca categoria por ID
     */
    public Optional<CategoriaResponseDTO> findById(UUID id) {
        return categoriaRepository.findById(id)
                .map(categoriaMapper::toResponseDTO);
    }
    
    /**
     * Busca categoria por nome
     */
    public Optional<CategoriaResponseDTO> findByNome(String nome) {
        return categoriaRepository.findByNome(nome)
                .map(categoriaMapper::toResponseDTO);
    }
    
    /**
     * Lista todas as categorias
     */
    public List<CategoriaResponseDTO> findAll() {
        return categoriaRepository.findAll().stream()
                .map(categoriaMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Atualiza categoria
     */
    public CategoriaResponseDTO update(UUID id, CategoriaRequestDTO requestDTO) {
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(id);
        if (categoriaOpt.isEmpty()) {
            throw new RuntimeException("Categoria não encontrada");
        }
        
        Categoria categoria = categoriaOpt.get();
        
        // Verifica se novo nome já existe em outra categoria
        Optional<Categoria> existingCategoria = categoriaRepository.findByNome(requestDTO.getNome());
        if (existingCategoria.isPresent() && !existingCategoria.get().getId().equals(id)) {
            throw new RuntimeException("Já existe uma categoria com este nome");
        }
        
        // Atualiza dados
        categoriaMapper.updateEntity(requestDTO, categoria);
        
        Categoria savedCategoria = categoriaRepository.save(categoria);
        
        return categoriaMapper.toResponseDTO(savedCategoria);
    }
    
    /**
     * Remove categoria
     */
    public void delete(UUID id) {
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(id);
        if (categoriaOpt.isEmpty()) {
            throw new RuntimeException("Categoria não encontrada");
        }
        
        // Verifica se categoria possui produtos
        long produtoCount = produtoRepository.countByCategoria(id);
        if (produtoCount > 0) {
            throw new RuntimeException("Não é possível excluir categoria que possui produtos");
        }
        
        categoriaRepository.deleteById(id);
    }
    
    /**
     * Conta total de categorias
     */
    public long count() {
        return categoriaRepository.count();
    }
    
    /**
     * Verifica se categoria existe
     */
    public boolean exists(UUID id) {
        return categoriaRepository.findById(id).isPresent();
    }
    
    /**
     * Verifica se nome já está em uso
     */
    public boolean existsByNome(String nome) {
        return categoriaRepository.existsByNome(nome);
    }
}