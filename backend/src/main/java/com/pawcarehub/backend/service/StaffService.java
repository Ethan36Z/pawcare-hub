package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.staff.StaffResponse;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.repository.StaffRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getAllStaff() {
        return staffRepository.findAllByOrderByActiveDescNameAsc().stream()
            .map(staff -> new StaffResponse(
                staff.getId(),
                staff.getName(),
                staff.getRole(),
                staff.isActive()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getActiveStaff() {
        return staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .map(staff -> new StaffResponse(
                staff.getId(),
                staff.getName(),
                staff.getRole(),
                staff.isActive()
            ))
            .toList();
    }

    @Transactional
    public StaffResponse toggleStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));

        staff.setActive(!staff.isActive());
        Staff savedStaff = staffRepository.save(staff);
        return new StaffResponse(
            savedStaff.getId(),
            savedStaff.getName(),
            savedStaff.getRole(),
            savedStaff.isActive()
        );
    }
}
