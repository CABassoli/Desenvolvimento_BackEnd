package com.ecommerce.mapper;

import com.ecommerce.domain.Pedido;
import com.ecommerce.dto.request.PedidoRequestDTO;
import com.ecommerce.dto.response.PedidoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversão entre Pedido e DTOs
 */
@Mapper(componentModel = "default", uses = {ClienteMapper.class, EnderecoMapper.class, ItemPedidoMapper.class})
public interface PedidoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    @Mapping(source = "enderecoEntrega", target = "endereco")
    PedidoResponseDTO toResponseDTO(Pedido pedido);
    
    /**
     * Converte DTO de requisição para entidade
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataPedido", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "valorTotal", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "enderecoEntrega", ignore = true)
    @Mapping(target = "itens", ignore = true)
    Pedido toEntity(PedidoRequestDTO requestDTO);
}