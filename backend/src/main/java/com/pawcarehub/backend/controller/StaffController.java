package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.staff.StaffResponse;
import com.pawcarehub.backend.service.StaffService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public List<StaffResponse> getActiveStaff() {
        return staffService.getActiveStaff();
    }
}
