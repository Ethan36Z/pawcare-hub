package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.AdminUserDetailResponse;
import com.pawcarehub.backend.dto.admin.AdminUserResponse;
import com.pawcarehub.backend.service.AdminUserService;
import com.pawcarehub.backend.service.RoleAccessService;
import com.pawcarehub.backend.service.UserRoles;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final RoleAccessService roleAccessService;

    public AdminUserController(AdminUserService adminUserService, RoleAccessService roleAccessService) {
        this.adminUserService = adminUserService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public List<AdminUserResponse> getUsers(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean active
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN);
        return adminUserService.getAllUsers(search, role, active);
    }

    @GetMapping("/{id}")
    public AdminUserDetailResponse getUser(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN);
        return adminUserService.getUserById(id);
    }
}
