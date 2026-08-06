package com.vitkvsk.order_service.dto;

import java.util.UUID;

public record UserInfoDto(
        UUID id,
        String name,
        String surname,
        String email
) {}