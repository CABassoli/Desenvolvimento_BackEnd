package com.ecommerce.mapper;

import com.ecommerce.domain.ItemCarrinho;
import com.ecommerce.dto.request.ItemCarrinhoRequestDTO;
import com.ecommerce.dto.response.ItemCarrinhoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para conversão entre ItemCarrinho e DTOs
 */
@Mapper(componentModel = "default", uses = {ProdutoMapper.class})
public interface ItemCarrinhoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    @Mapping(target = "subtotal", ignore = true)
    ItemCarrinhoResponseDTO toResponseDTO(ItemCarrinho itemCarrinho);
    
    /**
     * Converte DTO de requisição para entidade
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "carrinho", ignore = true)
    @Mapping(target = "produto", ignore = true)
    ItemCarrinho toEntity(ItemCarrinhoRequestDTO requestDTO);
    
    /**
     * Atualiza entidade existente com dados do DTO de requisição
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "carrinho", ignore = true)
    @Mapping(target = "produto", ignore = true)
    void updateEntity(ItemCarrinhoRequestDTO requestDTO, @MappingTarget ItemCarrinho itemCarrinho);
}