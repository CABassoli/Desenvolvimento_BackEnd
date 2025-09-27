package com.ecommerce.mapper;

import com.ecommerce.domain.Categoria;
import com.ecommerce.dto.request.CategoriaRequestDTO;
import com.ecommerce.dto.response.CategoriaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para conversão entre Categoria e DTOs
 */
@Mapper(componentModel = "default")
public interface CategoriaMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    CategoriaResponseDTO toResponseDTO(Categoria categoria);
    
    /**
     * Converte DTO de requisição para entidade
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "produtos", ignore = true)
    Categoria toEntity(CategoriaRequestDTO requestDTO);
    
    /**
     * Atualiza entidade existente com dados do DTO de requisição
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "produtos", ignore = true)
    void updateEntity(CategoriaRequestDTO requestDTO, @MappingTarget Categoria categoria);
}