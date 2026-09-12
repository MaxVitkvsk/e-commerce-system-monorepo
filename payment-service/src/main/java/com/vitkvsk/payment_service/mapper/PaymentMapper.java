package com.vitkvsk.payment_service.mapper;

import com.vitkvsk.payment_service.dto.PaymentCreateDto;
import com.vitkvsk.payment_service.dto.PaymentResponseDto;
import com.vitkvsk.payment_service.entity.Payment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    Payment toEntity(PaymentCreateDto dto);

    PaymentResponseDto toResponseDto(Payment entity);

    List<PaymentResponseDto> toResponseDtoList(List<Payment> entities);

}