package com.fluxusbackend.companyretail.interfaces.rest.dto;

import java.util.List;

public record DashboardStatsDto(
        int totalShrinkageMonth,
        double totalLostValue,
        int totalDonated,
        int activeUsers,
        int configuredRoles,
        List<MonthlyData> monthlyEvolution
) {
    public record MonthlyData(String name, int merma, int donada) {
    }
}
