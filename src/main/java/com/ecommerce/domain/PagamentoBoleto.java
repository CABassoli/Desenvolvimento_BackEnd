package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pagamento via boleto bancário
 */
@Entity
@Table(name = "pagamentos_boleto")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoBoleto extends Pagamento {
    
    @Column(name = "linha_digitavel", nullable = false)
    @NotBlank(message = "Linha digitável do boleto é obrigatória")
    private String linhaDigitavel;
}