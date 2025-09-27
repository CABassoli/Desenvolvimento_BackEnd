package com.ecommerce.mapper;

import com.ecommerce.domain.Notificacao;
import com.ecommerce.dto.response.NotificacaoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversão entre Notificacao e DTOs
 */
@Mapper(componentModel = "default")
public interface NotificacaoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "pedido.id", target = "pedidoId")
    NotificacaoResponseDTO toResponseDTO(Notificacao notificacao);
}