package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.auth.RegisterRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.User;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    User toEntity(RegisterRequest request);
}