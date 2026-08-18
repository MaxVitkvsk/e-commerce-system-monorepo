package com.vitkvsk.order_service.mapper;

import com.vitkvsk.order_service.dto.*;
import com.vitkvsk.order_service.entity.Order;
import com.vitkvsk.order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    Order toEntity(OrderCreateDto dto);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "userId", source = "order.userId")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "totalPrice", source = "order.totalPrice")
    @Mapping(target = "createdAt", source = "order.createdAt")
    @Mapping(target = "updatedAt", source = "order.updatedAt")
    @Mapping(target = "items", source = "order.items")
    @Mapping(target = "user", source = "user")
    OrderResponseDto toResponseDto(Order order, UserInfoDto user);

    default OrderResponseDto toResponseDto(Order order, Map<UUID, UserInfoDto> users) {
        return toResponseDto(order, users.get(order.getUserId()));
    }

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    @Mapping(target = "price", source = "item.price")
    OrderItemResponseDto toItemResponseDto(OrderItem orderItem);
}