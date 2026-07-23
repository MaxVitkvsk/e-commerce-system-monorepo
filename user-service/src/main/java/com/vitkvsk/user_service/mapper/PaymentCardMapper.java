package com.vitkvsk.user_service.mapper;

import com.vitkvsk.user_service.dto.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entities.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardCreateDto dto);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponseDto toResponseDto(PaymentCard entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "number", ignore = true)
    void updateEntityFromDto(PaymentCardUpdateDto dto, @MappingTarget PaymentCard entity);
}
