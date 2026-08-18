package com.vitkvsk.order_service.service;

import com.vitkvsk.order_service.client.UserServiceClient;
import com.vitkvsk.order_service.dto.*;
import com.vitkvsk.order_service.entity.Item;
import com.vitkvsk.order_service.entity.Order;
import com.vitkvsk.order_service.entity.OrderItem;
import com.vitkvsk.order_service.entity.OrderStatus;
import com.vitkvsk.order_service.exception.ResourceNotFoundException;
import com.vitkvsk.order_service.mapper.OrderMapper;
import com.vitkvsk.order_service.repository.ItemRepository;
import com.vitkvsk.order_service.repository.OrderRepository;
import com.vitkvsk.order_service.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserServiceClient userServiceClient;
    private final OrderMapper orderMapper;

    private OrderItem buildItem(OrderItemCreateDto line) {
        Item item = itemRepository.getReferenceById(line.itemId());
        return OrderItem.builder().item(item).quantity(line.quantity()).build();
    }

    private Order activeOrder(Long id) {
        return orderRepository.findWithItemsById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id));
    }

    private Map<UUID, UserInfoDto> usersFor(List<Order> orders) {
        return userServiceClient.getUsersByIds(
                orders.stream().map(Order::getUserId).distinct().toList());
    }

    private OrderResponseDto toDto(Order order) {
        return orderMapper.toResponseDto(order, userServiceClient.getUserInfo(order.getUserId()));
    }

    @Transactional
    public OrderResponseDto create(OrderCreateDto dto) {
        Order order = orderMapper.toEntity(dto);
        dto.items().forEach(line -> order.addItem(buildItem(line)));
        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getById(Long id) {
        return toDto(activeOrder(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> search(Instant from, Instant to,
                                         Collection<OrderStatus> statuses, Pageable pageable) {
        Specification<Order> spec = Specification.where(OrderSpecifications.notDeleted())
                .and(OrderSpecifications.createdBetween(from, to))
                .and(OrderSpecifications.statusIn(statuses));

        Page<Order> page = orderRepository.findAll(spec, pageable);
        Map<UUID, UserInfoDto> users = usersFor(page.getContent());
        return page.map(order -> orderMapper.toResponseDto(order, users));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getByUserId(UUID userId) {
        List<Order> orders = orderRepository.findAllByUserIdAndDeletedFalse(userId);
        Map<UUID, UserInfoDto> users = usersFor(orders);
        return orders.stream().map(o -> orderMapper.toResponseDto(o, users)).toList();
    }

    @Transactional
    public OrderResponseDto update(Long id, OrderUpdateDto dto) {
        Order order = activeOrder(id);

        if (dto.status() != null) order.setStatus(dto.status());

        if (dto.items() != null) {
            order.getItems().clear();
            dto.items().forEach(line -> order.addItem(buildItem(line)));
        }
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        activeOrder(id).setDeleted(true);
    }
}
