package com.vitkvsk.user_service.mapper;

import com.vitkvsk.user_service.dto.paymentcard.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entity.PaymentCard;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardCreateDto dto);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponseDto toResponseDto(PaymentCard entity);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE
    )
    void updateEntityFromDto(PaymentCardUpdateDto dto, @MappingTarget PaymentCard entity);
}
