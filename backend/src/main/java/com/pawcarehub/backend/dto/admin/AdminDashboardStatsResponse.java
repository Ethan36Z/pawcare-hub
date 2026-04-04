package com.pawcarehub.backend.dto.admin;

public record AdminDashboardStatsResponse(
    long totalUsers,
    long totalPets,
    long totalBookings,
    long totalServices,
    long confirmedBookings,
    long cancelledBookings
) {
}
