package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.UpsertStaffScheduleExceptionRequest;
import com.pawcarehub.backend.dto.staffavailability.StaffScheduleExceptionResponse;
import com.pawcarehub.backend.service.StaffScheduleExceptionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operations/staff/{staffId}/schedule-exceptions")
public class AdminStaffScheduleExceptionController {

    private final StaffScheduleExceptionService staffScheduleExceptionService;

    public AdminStaffScheduleExceptionController(StaffScheduleExceptionService staffScheduleExceptionService) {
        this.staffScheduleExceptionService = staffScheduleExceptionService;
    }

    @GetMapping
    public List<StaffScheduleExceptionResponse> getScheduleExceptions(@PathVariable Long staffId) {
        return staffScheduleExceptionService.getScheduleExceptions(staffId);
    }

    @PostMapping
    public StaffScheduleExceptionResponse createScheduleException(
        @PathVariable Long staffId,
        @RequestBody UpsertStaffScheduleExceptionRequest request
    ) {
        return staffScheduleExceptionService.createScheduleException(staffId, request);
    }

    @PatchMapping("/{exceptionId}")
    public StaffScheduleExceptionResponse updateScheduleException(
        @PathVariable Long staffId,
        @PathVariable Long exceptionId,
        @RequestBody UpsertStaffScheduleExceptionRequest request
    ) {
        return staffScheduleExceptionService.updateScheduleException(staffId, exceptionId, request);
    }

    @DeleteMapping("/{exceptionId}")
    public ResponseEntity<Void> deleteScheduleException(
        @PathVariable Long staffId,
        @PathVariable Long exceptionId
    ) {
        staffScheduleExceptionService.deleteScheduleException(staffId, exceptionId);
        return ResponseEntity.noContent().build();
    }
}
