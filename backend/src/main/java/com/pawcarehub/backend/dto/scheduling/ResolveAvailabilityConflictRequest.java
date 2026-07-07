package com.pawcarehub.backend.dto.scheduling;

import com.pawcarehub.backend.dto.admin.UpsertStaffAvailabilityRequest;
import java.util.List;

public record ResolveAvailabilityConflictRequest(
    UpsertStaffAvailabilityRequest scheduleChange,
    List<BookingReassignmentRequest> reassignments
) {
}
