package com.vitkvsk.user_service.controller;

import com.vitkvsk.user_service.dto.user.InternalUserCreateRequest;
import com.vitkvsk.user_service.dto.user.UserResponseDto;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
import com.vitkvsk.user_service.security.SecurityUtils;
import com.vitkvsk.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils security;

    @PostMapping("/internal")
    public ResponseEntity<UserResponseDto> createUserInternal(@Valid @RequestBody InternalUserCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req.user(), req.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        security.requireOwnerOrAdmin(id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname, Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(name, surname, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateDto dto) {
        security.requireOwnerOrAdmin(id);
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeUserStatus(@PathVariable UUID id, @RequestParam boolean active) {
        userService.changeUserStatus(id, active);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}