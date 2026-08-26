package com.vitkvsk.payment_service.repository;


import com.vitkvsk.payment_service.entity.Payment;
import com.vitkvsk.payment_service.entity.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(String userId);
    List<Payment> findByOrderId(String orderId);
    List<Payment> findByStatus(PaymentStatus status);
}
