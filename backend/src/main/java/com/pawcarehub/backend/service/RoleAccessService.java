package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.User;
import java.util.Arrays;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleAccessService {

    private final AuthService authService;

    public RoleAccessService(AuthService authService) {
        this.authService = authService;
    }

    public User requireAnyRole(String... allowedRoles) {
        User user = authService.getAuthenticatedUserEntity();
        String normalizedRole = UserRoles.normalize(user.getRole());
        Set<String> allowed = Set.copyOf(Arrays.asList(allowedRoles));

        if (!allowed.contains(normalizedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this action");
        }

        return user;
    }
}
