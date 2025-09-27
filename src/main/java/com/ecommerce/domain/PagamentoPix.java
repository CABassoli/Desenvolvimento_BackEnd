package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pagamento via PIX
 */
@Entity
@Table(name = "pagamentos_pix")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoPix extends Pagamento {
    
    @Column(name = "txid", nullable = false)
    @NotBlank(message = "TXID do PIX é obrigatório")
    private String txid;
}