package com.vitkvsk.user_service.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {
    public UUID currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null) throw new AccessDeniedException("not authenticated");
        return UUID.fromString(a.getName()); // name == JWT sub == user.id
    }
    public boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
    }
    public void requireOwnerOrAdmin(UUID id) {
        if (!isAdmin() && !currentUserId().equals(id)) throw new AccessDeniedException("access denied");
    }
}
