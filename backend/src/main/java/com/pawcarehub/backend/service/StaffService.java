package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.CreateStaffRequest;
import com.pawcarehub.backend.dto.staff.PublicStaffProfileResponse;
import com.pawcarehub.backend.dto.staff.StaffResponse;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.repository.StaffRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getActiveStaff() {
        return staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicStaffProfileResponse> getHomepageStaff() {
        return staffRepository.findByActiveTrueAndShowOnHomepageTrueOrderByNameAsc().stream()
            .map(staff -> new PublicStaffProfileResponse(
                staff.getId(),
                getPublicDisplayName(staff),
                getPublicTitle(staff),
                staff.getBio(),
                staff.getPhotoUrl()
            ))
            .toList();
    }

    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        String name = normalizeRequiredField(request.name(), "name");
        String role = normalizeRequiredField(request.role(), "role");

        if (staffRepository.existsByNameIgnoreCaseAndRoleIgnoreCase(name, role)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A staff member with this name and role already exists");
        }

        Staff savedStaff = staffRepository.save(new Staff(
            name,
            role,
            request.active() == null || request.active()
        ));
        applyPublicProfileFields(savedStaff, request);
        savedStaff = staffRepository.save(savedStaff);

        return toResponse(savedStaff);
    }

    @Transactional
    public StaffResponse updateStaff(Long staffId, CreateStaffRequest request) {
        Staff staff = staffRepository.findById(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));

        String name = normalizeRequiredField(request.name(), "name");
        String role = normalizeRequiredField(request.role(), "role");

        if (staffRepository.existsByNameIgnoreCaseAndRoleIgnoreCaseAndIdNot(name, role, staffId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A staff member with this name and role already exists");
        }

        staff.setName(name);
        staff.setRole(role);
        staff.setActive(request.active() == null || request.active());
        applyPublicProfileFields(staff, request);

        Staff savedStaff = staffRepository.save(staff);
        return toResponse(savedStaff);
    }

    @Transactional
    public StaffResponse toggleStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));

        staff.setActive(!staff.isActive());
        Staff savedStaff = staffRepository.save(staff);
        return toResponse(savedStaff);
    }

    private StaffResponse toResponse(Staff staff) {
        return new StaffResponse(
            staff.getId(),
            staff.getName(),
            staff.getRole(),
            staff.isActive(),
            getPublicDisplayName(staff),
            getPublicTitle(staff),
            staff.getBio(),
            staff.getPhotoUrl(),
            staff.isShowOnHomepage()
        );
    }

    private void applyPublicProfileFields(Staff staff, CreateStaffRequest request) {
        staff.setDisplayName(normalizeOptionalField(request.displayName()));
        staff.setSpecialty(normalizeOptionalField(request.specialty()));
        staff.setBio(normalizeOptionalField(request.bio()));
        staff.setPhotoUrl(normalizeOptionalField(request.photoUrl()));
        staff.setShowOnHomepage(Boolean.TRUE.equals(request.showOnHomepage()));
    }

    private String normalizeOptionalField(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String getPublicDisplayName(Staff staff) {
        return StringUtils.hasText(staff.getDisplayName()) ? staff.getDisplayName() : staff.getName();
    }

    private String getPublicTitle(Staff staff) {
        return StringUtils.hasText(staff.getSpecialty()) ? staff.getSpecialty() : staff.getRole();
    }

    private String normalizeRequiredField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        return value.trim();
    }
}
