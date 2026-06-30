package com.fluxusbackend.companyretail.interfaces.rest.transform;

import com.fluxusbackend.authaccess.application.internal.services.AuthorizationService;
import com.fluxusbackend.authaccess.domain.model.enums.UserActor;
import com.fluxusbackend.companyretail.infrastructure.clients.AuthAccessClient;
import com.fluxusbackend.companyretail.infrastructure.clients.DonationClient;
import com.fluxusbackend.companyretail.infrastructure.clients.ShrinkageClient;
import com.fluxusbackend.companyretail.infrastructure.clients.dto.DonationDto;
import com.fluxusbackend.companyretail.infrastructure.clients.dto.RetailUserDto;
import com.fluxusbackend.companyretail.infrastructure.clients.dto.RoleDto;
import com.fluxusbackend.companyretail.infrastructure.clients.dto.ShrinkageDto;
import com.fluxusbackend.companyretail.interfaces.rest.dto.DashboardStatsDto;
import com.fluxusbackend.companyretail.interfaces.rest.dto.DashboardStatsDto.MonthlyData;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retail/dashboard")
@Tag(name = "Retail Dashboard", description = "Dashboard operations for retailers")
@SecurityRequirement(name = "bearer")
public class RetailDashboardController {

    private final ShrinkageClient shrinkageClient;
    private final DonationClient donationClient;
    private final AuthAccessClient authAccessClient;
    private final AuthorizationService authorizationService;

    public RetailDashboardController(
            ShrinkageClient shrinkageClient,
            DonationClient donationClient,
            AuthAccessClient authAccessClient,
            AuthorizationService authorizationService
    ) {
        this.shrinkageClient = shrinkageClient;
        this.donationClient = donationClient;
        this.authAccessClient = authAccessClient;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get retail dashboard statistics")
    @CircuitBreaker(name = "retailDashboard", fallbackMethod = "fallbackStats")
    public DashboardStatsDto getStats(@RequestParam(value = "period", defaultValue = "Últimos 30 días") String period) {
        authorizationService.requireActor(UserActor.RETAIL);

        var shrinkages = getShrinkages();
        var donations = getDonations();
        var users = getRetailUsers();
        var roles = getRoles();

        var now = LocalDate.now();

        LocalDate startDate = null;
        if ("Últimos 7 días".equals(period)) {
            startDate = now.minusDays(6);
        } else if ("Últimos 30 días".equals(period)) {
            startDate = now.minusDays(29);
        } else if ("Últimos 3 meses".equals(period)) {
            startDate = now.minusMonths(3).plusDays(1);
        } else if ("Este año".equals(period)) {
            startDate = LocalDate.of(now.getYear(), 1, 1);
        }

        int totalShrinkageMonth = 0;
        double totalLostValue = 0;

        for (var shrinkage : shrinkages) {
            var createdDate = toLocalDate(shrinkage.createdAt());
            if (startDate == null || !createdDate.isBefore(startDate)) {
                var quantity = safeInt(shrinkage.quantity());
                totalShrinkageMonth += quantity;
                totalLostValue += safeDouble(shrinkage.shrinkageValue()) * quantity;
            }
        }

        LocalDate finalStartDate = startDate;
        int totalDonated = donations.stream()
                .filter(donation -> finalStartDate == null || !toLocalDate(donation.createdAt()).isBefore(finalStartDate))
                .mapToInt(donation -> safeInt(donation.quantity()))
                .sum();

        int activeUsers = (int) users.stream()
                .filter(user -> Boolean.TRUE.equals(user.retailUserActive()))
                .count();

        int configuredRoles = roles.size();

        return new DashboardStatsDto(
                totalShrinkageMonth,
                totalLostValue,
                totalDonated,
                activeUsers,
                configuredRoles,
                buildEvolution(shrinkages, donations, now, period)
        );
    }

    @GetMapping(value = "/report", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Download detailed retail company report")
    public void downloadReport(HttpServletResponse response) throws IOException {
        authorizationService.requireActor(UserActor.RETAIL);

        var shrinkages = getShrinkages();
        var donations = getDonations();

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_gestion.csv\"");

        var writer = response.getWriter();
        writer.write('\ufeff');

        writer.println("REPORTE DE GESTION - MERMA Y DONACIONES");
        writer.println();
        writer.println("MERMAS REGISTRADAS");
        writer.println("ID,Producto,Cantidad,Categoria,Razon,Valor Unitario,Valor Total,Fecha de Registro,Estado");

        for (var shrinkage : shrinkages) {
            int quantity = safeInt(shrinkage.quantity());
            double unitValue = safeDouble(shrinkage.shrinkageValue());
            writer.printf("%d,%s,%d,%s,%s,%.2f,%.2f,%s,%s%n",
                    shrinkage.shrinkageId(),
                    escapeCsv(shrinkage.name()),
                    quantity,
                    escapeCsv(shrinkage.category() == null ? "-" : shrinkage.category().name()),
                    escapeCsv(shrinkage.shrinkageReason() == null ? "-" : shrinkage.shrinkageReason().name()),
                    unitValue,
                    unitValue * quantity,
                    toLocalDate(shrinkage.createdAt()),
                    shrinkage.status()
            );
        }

        writer.println();
        writer.println("DONACIONES REALIZADAS");
        writer.println("ID,Cantidad,Fecha de Registro,Estado");

        for (var donation : donations) {
            writer.printf("%s,%d,%s,%s%n",
                    donation.donationId(),
                    safeInt(donation.quantity()),
                    toLocalDate(donation.createdAt()),
                    donation.status()
            );
        }

        writer.flush();
    }

    private List<MonthlyData> buildEvolution(
            List<ShrinkageDto> shrinkages,
            List<DonationDto> donations,
            LocalDate now,
            String period
    ) {
        List<MonthlyData> evolution = new ArrayList<>();
        if ("Últimos 7 días".equals(period)) {
            for (int i = 6; i >= 0; i--) {
                LocalDate dayDate = now.minusDays(i);
                String dayName = dayDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.of("es", "PE"));
                dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1).replace(".", "") + " " + dayDate.getDayOfMonth();

                int dayShrinkages = shrinkages.stream()
                        .filter(s -> toLocalDate(s.createdAt()).isEqual(dayDate))
                        .mapToInt(s -> safeInt(s.quantity()))
                        .sum();

                int dayDonated = donations.stream()
                        .filter(d -> toLocalDate(d.createdAt()).isEqual(dayDate))
                        .mapToInt(d -> safeInt(d.quantity()))
                        .sum();

                evolution.add(new MonthlyData(dayName, dayShrinkages, dayDonated));
            }
        } else if ("Últimos 30 días".equals(period)) {
            for (int i = 29; i >= 0; i--) {
                LocalDate dayDate = now.minusDays(i);
                String label = dayDate.getDayOfMonth() + "/" + dayDate.getMonthValue();

                int dayShrinkages = shrinkages.stream()
                        .filter(s -> toLocalDate(s.createdAt()).isEqual(dayDate))
                        .mapToInt(s -> safeInt(s.quantity()))
                        .sum();

                int dayDonated = donations.stream()
                        .filter(d -> toLocalDate(d.createdAt()).isEqual(dayDate))
                        .mapToInt(d -> safeInt(d.quantity()))
                        .sum();

                evolution.add(new MonthlyData(label, dayShrinkages, dayDonated));
            }
        } else if ("Últimos 3 meses".equals(period)) {
            for (int i = 11; i >= 0; i--) {
                LocalDate weekStart = now.minusWeeks(i).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate weekEnd = weekStart.plusDays(6);
                String label = weekStart.getDayOfMonth() + "/" + weekStart.getMonthValue() + " - " + weekEnd.getDayOfMonth() + "/" + weekEnd.getMonthValue();

                int weekShrinkages = shrinkages.stream()
                        .filter(s -> {
                            LocalDate date = toLocalDate(s.createdAt());
                            return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
                        })
                        .mapToInt(s -> safeInt(s.quantity()))
                        .sum();

                int weekDonated = donations.stream()
                        .filter(d -> {
                            LocalDate date = toLocalDate(d.createdAt());
                            return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
                        })
                        .mapToInt(d -> safeInt(d.quantity()))
                        .sum();

                evolution.add(new MonthlyData(label, weekShrinkages, weekDonated));
            }
        } else if ("Este año".equals(period)) {
            int currentMonthVal = now.getMonthValue();
            for (int i = 1; i <= currentMonthVal; i++) {
                LocalDate monthDate = LocalDate.of(now.getYear(), i, 1);
                String monthName = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.of("es", "PE"));
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1).replace(".", "");

                int monthShrinkages = shrinkages.stream()
                        .filter(s -> isSameMonth(toLocalDate(s.createdAt()), monthDate))
                        .mapToInt(s -> safeInt(s.quantity()))
                        .sum();

                int monthDonated = donations.stream()
                        .filter(d -> isSameMonth(toLocalDate(d.createdAt()), monthDate))
                        .mapToInt(d -> safeInt(d.quantity()))
                        .sum();

                evolution.add(new MonthlyData(monthName, monthShrinkages, monthDonated));
            }
        } else {
            for (int i = 11; i >= 0; i--) {
                LocalDate monthDate = now.minusMonths(i);
                String monthName = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.of("es", "PE"));
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1).replace(".", "");

                int monthShrinkages = shrinkages.stream()
                        .filter(s -> isSameMonth(toLocalDate(s.createdAt()), monthDate))
                        .mapToInt(s -> safeInt(s.quantity()))
                        .sum();

                int monthDonated = donations.stream()
                        .filter(d -> isSameMonth(toLocalDate(d.createdAt()), monthDate))
                        .mapToInt(d -> safeInt(d.quantity()))
                        .sum();

                evolution.add(new MonthlyData(monthName, monthShrinkages, monthDonated));
            }
        }
        return evolution;
    }

    private List<ShrinkageDto> getShrinkages() {
        try {
            return shrinkageClient.listByCompany();
        } catch (FeignException ex) {
            return List.of();
        }
    }

    private List<DonationDto> getDonations() {
        try {
            return donationClient.listByCompany();
        } catch (FeignException ex) {
            return List.of();
        }
    }

    private List<RetailUserDto> getRetailUsers() {
        try {
            return authAccessClient.listRetailUsers();
        } catch (FeignException ex) {
            return List.of();
        }
    }

    private List<RoleDto> getRoles() {
        try {
            return authAccessClient.listRoles();
        } catch (FeignException ex) {
            return List.of();
        }
    }

    private LocalDate toLocalDate(Instant instant) {
        if (instant == null) {
            return LocalDate.now();
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private boolean isSameMonth(LocalDate value, LocalDate monthDate) {
        return value.getMonthValue() == monthDate.getMonthValue() && value.getYear() == monthDate.getYear();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private String escapeCsv(String text) {
        if (text == null) {
            return "";
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    public DashboardStatsDto fallbackStats(String period, Throwable t) {
        System.err.println("Circuit breaker triggered for retail dashboard statistics. Error: " + t.getMessage());
        return new DashboardStatsDto(0, 0.0, 0, 0, 0, new java.util.ArrayList<>());
    }
}
