package com.vitkvsk.user_service.controller;


import com.vitkvsk.user_service.dto.*;
import com.vitkvsk.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@RequestBody UserCreateDto userCreateDto) {
        UserResponseDto createdUser = userService.createUser(userCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(name, surname, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @RequestBody UserUpdateDto userUpdateDto) {
        return ResponseEntity.ok(userService.updateUser(id, userUpdateDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeUserStatus(@PathVariable Long id, @RequestParam boolean active) {
        userService.changeUserStatus(id, active);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/cards")
    public ResponseEntity<PaymentCardResponseDto> addCardToUser(
            @PathVariable Long userId,
            @RequestBody PaymentCardCreateDto paymentCardCreateDto) {
        PaymentCardResponseDto createdCard = userService.addCardToUser(userId, paymentCardCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }
}
