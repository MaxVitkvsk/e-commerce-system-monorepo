package com.vitkvsk.order_service.unit;

import com.vitkvsk.order_service.client.UserServiceClient;
import com.vitkvsk.order_service.dto.*;
import com.vitkvsk.order_service.entity.Item;
import com.vitkvsk.order_service.entity.Order;
import com.vitkvsk.order_service.entity.OrderItem;
import com.vitkvsk.order_service.entity.OrderStatus;
import com.vitkvsk.order_service.exception.ResourceNotFoundException;
import com.vitkvsk.order_service.mapper.OrderMapper;
import com.vitkvsk.order_service.mapper.OrderMapperImpl;
import com.vitkvsk.order_service.repository.ItemRepository;
import com.vitkvsk.order_service.repository.OrderRepository;
import com.vitkvsk.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserServiceClient userServiceClient;

    private OrderMapper orderMapper;
    private OrderService orderService;

    private Order testOrder;
    private Item testItem;
    private UserInfoDto testUserInfo;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapperImpl();
        orderService = new OrderService(orderRepository, itemRepository, userServiceClient, orderMapper);

        testUserId = UUID.randomUUID();

        testItem = Item.builder()
                .id(1L)
                .name("Test Item")
                .price(new BigDecimal("10.00"))
                .build();

        testOrder = Order.builder()
                .id(1L)
                .userId(testUserId)
                .status(OrderStatus.NEW)
                .totalPrice(new BigDecimal("20.00"))
                .deleted(false)
                .build();
        testOrder.setCreatedAt(Instant.now());

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .item(testItem)
                .quantity(2)
                .build();
        testOrder.addItem(orderItem);

        testUserInfo = new UserInfoDto(testUserId, "Pop", "Dod", "john@example.com");
    }

    @Test
    void shouldCreateOrder() {
        OrderCreateDto dto = new OrderCreateDto(
                testUserId, new BigDecimal("20.00"),
                List.of(new OrderItemCreateDto(1L, 2)));

        when(itemRepository.getReferenceById(1L)).thenReturn(testItem);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userServiceClient.getUserInfo(testUserId)).thenReturn(testUserInfo);

        OrderResponseDto result = orderService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUserId);
        assertThat(result.status()).isEqualTo(OrderStatus.NEW);
        verify(orderRepository).save(any(Order.class));
        verify(itemRepository).getReferenceById(1L);
    }

    @Test
    void shouldGetOrderById() {
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserInfo(testUserId)).thenReturn(testUserInfo);

        OrderResponseDto result = orderService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(testUserId);
        verify(orderRepository).findWithItemsById(1L);
    }

    @Test
    void shouldThrowNotFoundWhenOrderNotFound() {
        when(orderRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order 999");
    }

    @Test
    void shouldThrowNotFoundWhenOrderIsDeleted() {
        testOrder.setDeleted(true);
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldSearchOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userServiceClient.getUsersByIds(anyList())).thenReturn(Map.of(testUserId, testUserInfo));

        Page<OrderResponseDto> result = orderService.search(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldSearchOrdersWithStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userServiceClient.getUsersByIds(anyList())).thenReturn(Map.of(testUserId, testUserInfo));

        Page<OrderResponseDto> result = orderService.search(null, null, List.of(OrderStatus.NEW), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void shouldGetOrdersByUserId() {
        when(orderRepository.findAllByUserIdAndDeletedFalse(testUserId)).thenReturn(List.of(testOrder));
        when(userServiceClient.getUsersByIds(anyList())).thenReturn(Map.of(testUserId, testUserInfo));

        List<OrderResponseDto> result = orderService.getByUserId(testUserId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(testUserId);
        verify(orderRepository).findAllByUserIdAndDeletedFalse(testUserId);
    }

    @Test
    void shouldUpdateOrderStatus() {
        OrderUpdateDto dto = new OrderUpdateDto(OrderStatus.CONFIRMED, null);

        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userServiceClient.getUserInfo(testUserId)).thenReturn(testUserInfo);

        OrderResponseDto result = orderService.update(1L, dto);

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldUpdateOrderItems() {
        OrderUpdateDto dto = new OrderUpdateDto(null, List.of(new OrderItemCreateDto(1L, 5)));

        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(testOrder));
        when(itemRepository.getReferenceById(1L)).thenReturn(testItem);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userServiceClient.getUserInfo(testUserId)).thenReturn(testUserInfo);

        OrderResponseDto result = orderService.update(1L, dto);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(5);
        verify(itemRepository).getReferenceById(1L);
    }

    @Test
    void shouldSoftDeleteOrder() {
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(testOrder));

        orderService.delete(1L);

        assertThat(testOrder.isDeleted()).isTrue();
        verify(orderRepository, never()).delete(any(Order.class));
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentOrder() {
        when(orderRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
