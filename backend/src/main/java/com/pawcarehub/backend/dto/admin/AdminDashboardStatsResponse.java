package com.pawcarehub.backend.dto.admin;

import java.util.List;

public record AdminDashboardStatsResponse(
    long totalUsers,
    long petRecords,
    long activeStaff,
    long activeServices,
    long totalBookings,
    List<BookingStatusStat> bookingsByStatus,
    List<DashboardBookingSnapshotItem> upcomingBookings,
    List<DashboardBookingSnapshotItem> recentCompletedVisits
) {
    public record BookingStatusStat(
        String status,
        long count
    ) {
    }

    public record DashboardBookingSnapshotItem(
        Long id,
        String petName,
        String service,
        String date,
        String time,
        String staff,
        String ownerName,
        String status
    ) {
    }
}
