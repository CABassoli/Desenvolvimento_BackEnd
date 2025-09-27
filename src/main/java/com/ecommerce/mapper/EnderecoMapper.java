package com.ecommerce.mapper;

import com.ecommerce.domain.Endereco;
import com.ecommerce.dto.request.EnderecoRequestDTO;
import com.ecommerce.dto.response.EnderecoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para conversão entre Endereco e DTOs
 */
@Mapper(componentModel = "default")
public interface EnderecoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    @Mapping(source = "cliente.id", target = "clienteId")
    EnderecoResponseDTO toResponseDTO(Endereco endereco);
    
    /**
     * Converte DTO de requisição para entidade
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    Endereco toEntity(EnderecoRequestDTO requestDTO);
    
    /**
     * Atualiza entidade existente com dados do DTO de requisição
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    void updateEntity(EnderecoRequestDTO requestDTO, @MappingTarget Endereco endereco);
}