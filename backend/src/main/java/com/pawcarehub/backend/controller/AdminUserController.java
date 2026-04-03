package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.AdminUserResponse;
import com.pawcarehub.backend.service.AdminUserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> getUsers() {
        return adminUserService.getAllUsers();
    }
}
