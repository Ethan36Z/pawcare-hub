package com.pawcarehub.backend.dto.scheduling;

import com.pawcarehub.backend.dto.admin.UpsertStaffScheduleExceptionRequest;
import java.util.List;

public record ResolveScheduleExceptionConflictRequest(
    UpsertStaffScheduleExceptionRequest scheduleChange,
    List<BookingReassignmentRequest> reassignments
) {
}
