package com.vitkvsk.user_service.repository;

import com.vitkvsk.user_service.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {
    List<PaymentCard> findAllByUserId(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query(value = "UPDATE payment_cards SET active = :active, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
            nativeQuery = true)
    void updateActiveStatus(@Param("active") boolean active, @Param("id") Long id);
}
