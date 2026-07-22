package com.vitkvsk.user_service.dao;

import com.vitkvsk.user_service.entities.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
    List<PaymentCard> findAllByUserId(Long userId);
}
