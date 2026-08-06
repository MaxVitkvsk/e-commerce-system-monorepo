package com.vitkvsk.order_service.specification;

import com.vitkvsk.order_service.entity.Order;
import com.vitkvsk.order_service.entity.OrderStatus;
import com.vitkvsk.order_service.entity.Order_;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;

public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<Order> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get(Order_.deleted));
    }

    public static Specification<Order> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null)
                return cb.between(root.get(Order_.createdAt), from, to);
            if (from != null)
                return cb.greaterThanOrEqualTo(root.get(Order_.createdAt), from);
            if (to != null)
                return cb.lessThanOrEqualTo(root.get(Order_.createdAt), to);
            return null;
        };
    }

    public static Specification<Order> statusIn(Collection<OrderStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get(Order_.status).in(statuses);
    }
}