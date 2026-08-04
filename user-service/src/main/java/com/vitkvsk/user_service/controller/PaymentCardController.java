package com.vitkvsk.user_service.controller;

import com.vitkvsk.user_service.dto.paymentcard.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardUpdateDto;
import com.vitkvsk.user_service.security.SecurityUtils;
import com.vitkvsk.user_service.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;
    private final SecurityUtils security;

    @PostMapping
    public ResponseEntity<PaymentCardResponseDto> createCard(@Valid @RequestBody PaymentCardCreateDto dto) {
        UUID ownerId = security.isAdmin() ? dto.userId() : security.currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentCardService.createCard(dto, ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> getCardById(@PathVariable Long id) {
        PaymentCardResponseDto card = paymentCardService.getCardById(id);
        security.requireOwnerOrAdmin(card.userId());
        return ResponseEntity.ok(card);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentCardResponseDto>> getCardsByUserId(@PathVariable UUID userId) {
        security.requireOwnerOrAdmin(userId);
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardResponseDto>> getAllCards(
            @RequestParam(required = false) String holderName,
            @RequestParam(required = false) String holderSurname, Pageable pageable) {
        return ResponseEntity.ok(paymentCardService.getAllCards(holderName, holderSurname, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> updateCard(@PathVariable Long id, @Valid @RequestBody PaymentCardUpdateDto dto) {
        return ResponseEntity.ok(paymentCardService.updateCard(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateActiveStatus(@PathVariable Long id, @RequestParam boolean active) {
        paymentCardService.updateActiveStatus(id, active);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}