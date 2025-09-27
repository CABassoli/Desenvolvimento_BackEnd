package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pagamento via cartão de crédito/débito
 */
@Entity
@Table(name = "pagamentos_cartao")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoCartao extends Pagamento {
    
    @Column(name = "bandeira", nullable = false)
    @NotBlank(message = "Bandeira do cartão é obrigatória")
    private String bandeira;
    
    @Column(name = "token_cartao", nullable = false)
    @NotBlank(message = "Token do cartão é obrigatório")
    private String tokenCartao; // Armazenar token, não PAN
}