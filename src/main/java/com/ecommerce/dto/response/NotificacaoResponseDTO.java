package com.ecommerce.dto.response;

import com.ecommerce.domain.Notificacao;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para resposta com dados da notificação
 */
@Data
public class NotificacaoResponseDTO {
    
    private UUID id;
    private Notificacao.TipoNotificacao tipo;
    private String mensagem;
    private LocalDateTime criadoEm;
    private UUID clienteId;
    private UUID pedidoId;
}