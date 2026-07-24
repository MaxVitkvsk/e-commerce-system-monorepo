package com.vitkvsk.user_service.mapper;

import com.vitkvsk.user_service.dto.user.UserCreateDto;
import com.vitkvsk.user_service.dto.user.UserResponseDto;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
import com.vitkvsk.user_service.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "cards", ignore = true)
    User toEntity(UserCreateDto dto);

    UserResponseDto toResponseDto(User user);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE
    )
    void updateEntityFromDto(UserUpdateDto dto, @MappingTarget User user);
}
