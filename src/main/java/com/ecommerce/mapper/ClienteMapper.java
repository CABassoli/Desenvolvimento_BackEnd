package com.ecommerce.mapper;

import com.ecommerce.domain.Cliente;
import com.ecommerce.dto.request.ClienteRequestDTO;
import com.ecommerce.dto.response.ClienteResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para conversão entre Cliente e DTOs
 */
@Mapper(componentModel = "default")
public interface ClienteMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    ClienteResponseDTO toResponseDTO(Cliente cliente);
    
    /**
     * Converte DTO de requisição para entidade
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enderecos", ignore = true)
    @Mapping(target = "carrinho", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    Cliente toEntity(ClienteRequestDTO requestDTO);
    
    /**
     * Atualiza entidade existente com dados do DTO de requisição
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enderecos", ignore = true)
    @Mapping(target = "carrinho", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    void updateEntity(ClienteRequestDTO requestDTO, @MappingTarget Cliente cliente);
}