package com.vitkvsk.order_service.repository;

import com.vitkvsk.order_service.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"items", "items.item"})
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "items.item"})
    List<Order> findAllByUserIdAndDeletedFalse(UUID userId);

    boolean existsByIdAndDeletedFalse(Long id);
}
