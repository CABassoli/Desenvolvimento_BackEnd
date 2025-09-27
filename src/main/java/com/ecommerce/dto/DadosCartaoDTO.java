package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para receber dados do cartão na simulação (não tokenizado)
 * Estes dados são usados apenas para gerar o token e nunca persistidos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DadosCartaoDTO {
    
    @NotBlank(message = "Número do cartão é obrigatório")
    @Pattern(regexp = "\\d{13,19}", message = "Número do cartão deve ter entre 13 e 19 dígitos")
    private String numero;
    
    @NotBlank(message = "Nome do portador é obrigatório")
    private String nome;
    
    @NotBlank(message = "Validade é obrigatória")
    @Pattern(regexp = "\\d{2}/\\d{2}", message = "Validade deve estar no formato MM/AA")
    private String validade;
    
    @NotBlank(message = "CVV é obrigatório")
    @Pattern(regexp = "\\d{3,4}", message = "CVV deve ter 3 ou 4 dígitos")
    private String cvv;
    
    @NotBlank(message = "Bandeira é obrigatória")
    private String bandeira; // "VISA", "MASTERCARD", "ELO", etc.
}