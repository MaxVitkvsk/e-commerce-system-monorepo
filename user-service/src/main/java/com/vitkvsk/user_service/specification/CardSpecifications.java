package com.vitkvsk.user_service.specification;

import com.vitkvsk.user_service.entity.PaymentCard;
import com.vitkvsk.user_service.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {}

    public static Specification<PaymentCard> hasUserName(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank()) ? null : cb.equal(root.join(PaymentCard.Fields.user).get(User.Fields.name), name);
    }

    public static Specification<PaymentCard> hasUserSurname(String surname) {
        return (root, query, cb) ->
                (surname == null || surname.isBlank()) ? null : cb.equal(root.join(PaymentCard.Fields.user).get(User.Fields.surname), surname);
    }
}
