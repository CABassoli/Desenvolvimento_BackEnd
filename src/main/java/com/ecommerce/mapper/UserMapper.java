package com.ecommerce.mapper;

import com.ecommerce.domain.UserModel;
import com.ecommerce.dto.response.UserResponseDTO;
import org.mapstruct.Mapper;

/**
 * Mapper para conversão entre UserModel e DTOs
 */
@Mapper(componentModel = "default")
public interface UserMapper {
    
    /**
     * Converte entidade para DTO de resposta
     */
    UserResponseDTO toResponseDTO(UserModel user);
}