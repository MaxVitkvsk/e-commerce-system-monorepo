package com.vitkvsk.user_service.specification;

import com.vitkvsk.user_service.entities.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class CardSpecifications {
    public static Specification<PaymentCard> hasUserName(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank())
                        ? null
                        : cb.equal(root.join("user").get("name"), name);
    }

    public static Specification<PaymentCard> hasUserSurname(String surname) {
        return (root, query, cb) ->
                (surname == null || surname.isBlank())
                        ? null
                        : cb.equal(root.join("user").get("surname"), surname);
    }
}
