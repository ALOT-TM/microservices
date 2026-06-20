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
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public DashboardStatsDto getStats() {
        authorizationService.requireActor(UserActor.RETAIL);

        var shrinkages = getShrinkages();
        var donations = getDonations();
        var users = getRetailUsers();
        var roles = getRoles();

        var now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        int totalShrinkageMonth = 0;
        double totalLostValue = 0;

        for (var shrinkage : shrinkages) {
            var createdDate = toLocalDate(shrinkage.createdAt());
            if (createdDate.getMonthValue() == currentMonth && createdDate.getYear() == currentYear) {
                var quantity = safeInt(shrinkage.quantity());
                totalShrinkageMonth += quantity;
                totalLostValue += safeDouble(shrinkage.shrinkageValue()) * quantity;
            }
        }

        int totalDonated = donations.stream()
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
                buildMonthlyEvolution(shrinkages, donations, now)
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

    private List<MonthlyData> buildMonthlyEvolution(
            List<ShrinkageDto> shrinkages,
            List<DonationDto> donations,
            LocalDate now
    ) {
        List<MonthlyData> evolution = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthName = monthDate.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "PE"));
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1).replace(".", "");

            int monthShrinkages = shrinkages.stream()
                    .filter(shrinkage -> isSameMonth(toLocalDate(shrinkage.createdAt()), monthDate))
                    .mapToInt(shrinkage -> safeInt(shrinkage.quantity()))
                    .sum();

            int monthDonated = donations.stream()
                    .filter(donation -> isSameMonth(toLocalDate(donation.createdAt()), monthDate))
                    .mapToInt(donation -> safeInt(donation.quantity()))
                    .sum();

            evolution.add(new MonthlyData(monthName, monthShrinkages, monthDonated));
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
}
