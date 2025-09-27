package com.ecommerce.mapper;

import com.ecommerce.domain.ItemPedido;
import com.ecommerce.dto.response.ItemPedidoResponseDTO;
import org.mapstruct.Mapper;

/**
 * Mapper para conversão entre ItemPedido e DTOs
 */
@Mapper(componentModel = "default", uses = {ProdutoMapper.class})
public interface ItemPedidoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    ItemPedidoResponseDTO toResponseDTO(ItemPedido itemPedido);
}