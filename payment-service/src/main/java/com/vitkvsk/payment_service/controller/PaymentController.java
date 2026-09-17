package com.vitkvsk.payment_service.controller;

import com.vitkvsk.payment_service.dto.PaymentCreateDto;
import com.vitkvsk.payment_service.dto.PaymentResponseDto;
import com.vitkvsk.payment_service.dto.PaymentTotalDto;
import com.vitkvsk.payment_service.entity.PaymentStatus;
import com.vitkvsk.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto create(@Valid @RequestBody PaymentCreateDto dto) {
        return paymentService.create(dto);
    }

    @GetMapping
    public List<PaymentResponseDto> search(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) PaymentStatus status) {
        return paymentService.search(userId, orderId, status);
    }

    @GetMapping("/total")
    public PaymentTotalDto totalForUser(@RequestParam String userId,
                                        @RequestParam Instant from,
                                        @RequestParam Instant to) {
        return paymentService.totalForUser(userId, from, to);
    }

    @GetMapping("/total/all")
    public PaymentTotalDto totalForAll(@RequestParam Instant from,
                                       @RequestParam Instant to) {
        return paymentService.totalForAll(from, to);
    }
}