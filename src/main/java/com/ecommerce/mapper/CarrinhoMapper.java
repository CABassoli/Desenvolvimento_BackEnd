package com.ecommerce.mapper;

import com.ecommerce.domain.Carrinho;
import com.ecommerce.domain.ItemCarrinho;
import com.ecommerce.dto.response.CarrinhoResponseDTO;
import com.ecommerce.dto.response.ItemCarrinhoResponseDTO;
import com.ecommerce.dto.response.ProdutoResponseDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para conversão entre Carrinho e DTOs
 */
@Mapper(componentModel = "default", uses = {ItemCarrinhoMapper.class})
public interface CarrinhoMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "cliente.id", target = "userId")
    @Mapping(target = "itens", ignore = true)
    @Mapping(target = "valorTotal", ignore = true)
    @Mapping(target = "totalItens", ignore = true)
    @Mapping(target = "totalValor", ignore = true)
    @Mapping(target = "totalQuantidade", ignore = true)
    CarrinhoResponseDTO toResponseDTO(Carrinho carrinho);
    
    /**
     * Calcula valores e mapeia itens após mapeamento
     */
    @AfterMapping
    default void calculateTotalsAndMapItens(@MappingTarget CarrinhoResponseDTO dto, Carrinho carrinho) {
        if (carrinho.getItens() != null && !carrinho.getItens().isEmpty()) {
            // Mapear itens para formato padrão
            List<ItemCarrinhoResponseDTO> itensMapeados = new ArrayList<>();
            BigDecimal valorTotal = BigDecimal.ZERO;
            int totalQuantidade = 0;
            
            for (ItemCarrinho item : carrinho.getItens()) {
                ItemCarrinhoResponseDTO itemDto = new ItemCarrinhoResponseDTO();
                
                // Mapear produto
                if (item.getProduto() != null) {
                    ProdutoResponseDTO produtoDto = new ProdutoResponseDTO();
                    produtoDto.setId(item.getProduto().getId());
                    produtoDto.setNome(item.getProduto().getNome());
                    produtoDto.setPreco(item.getProduto().getPreco());
                    produtoDto.setCodigoBarras(item.getProduto().getCodigoBarras());
                    if (item.getProduto().getCategoria() != null) {
                        produtoDto.setCategoriaId(item.getProduto().getCategoria().getId());
                    }
                    itemDto.setProduto(produtoDto);
                }
                
                itemDto.setId(item.getId());
                itemDto.setQuantidade(item.getQuantidade());
                
                BigDecimal subtotal = item.getProduto().getPreco().multiply(BigDecimal.valueOf(item.getQuantidade()));
                itemDto.setSubtotal(subtotal);
                
                itensMapeados.add(itemDto);
                valorTotal = valorTotal.add(subtotal);
                totalQuantidade += item.getQuantidade();
            }
            
            dto.setItens(itensMapeados);
            dto.setValorTotal(valorTotal);
            dto.setTotalValor(valorTotal);
            dto.setTotalItens(totalQuantidade);
            dto.setTotalQuantidade(totalQuantidade);
        } else {
            dto.setItens(new ArrayList<>());
            dto.setValorTotal(BigDecimal.ZERO);
            dto.setTotalValor(BigDecimal.ZERO);
            dto.setTotalItens(0);
            dto.setTotalQuantidade(0);
        }
    }
}