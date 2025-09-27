package com.ecommerce.mapper;

import com.ecommerce.domain.Produto;
import com.ecommerce.dto.request.ProdutoRequestDTO;
import com.ecommerce.dto.response.ProdutoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para conversão entre Produto e DTOs
 */
@Mapper(componentModel = "default", uses = {CategoriaMapper.class})
public interface ProdutoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    ProdutoResponseDTO toResponseDTO(Produto produto);
    
    /**
     * Converte DTO de requisição para entidade
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    Produto toEntity(ProdutoRequestDTO requestDTO);
    
    /**
     * Atualiza entidade existente com dados do DTO de requisição
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    void updateEntity(ProdutoRequestDTO requestDTO, @MappingTarget Produto produto);
}