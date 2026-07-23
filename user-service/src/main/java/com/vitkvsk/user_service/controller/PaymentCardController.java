package com.vitkvsk.user_service.controller;

import com.vitkvsk.user_service.dto.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.PaymentCardUpdateDto;
import com.vitkvsk.user_service.service.PaymentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("api/cards")
@RequiredArgsConstructor
public class PaymentCardController {
    private final PaymentCardService paymentCardService;

    @PostMapping
    public ResponseEntity<PaymentCardResponseDto> createCard(@RequestBody PaymentCardCreateDto paymentCardCreateDto){
        PaymentCardResponseDto createdCard = paymentCardService.createCard(paymentCardCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getCardById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentCardResponseDto>> getCardsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardResponseDto>> getAllCards(
            @RequestParam(required = false) String holderName,
            @RequestParam(required = false) String holderSurname,
            Pageable pageable) {
        return ResponseEntity.ok(paymentCardService.getAllCards(holderName, holderSurname, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> updateCard(@PathVariable Long id, @RequestBody PaymentCardUpdateDto dto) {
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
