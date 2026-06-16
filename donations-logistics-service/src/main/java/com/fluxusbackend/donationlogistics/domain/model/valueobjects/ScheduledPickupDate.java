package com.fluxusbackend.donationlogistics.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
@io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date", example = "2026-06-05")
public record ScheduledPickupDate(@Column(name = "scheduled_pickup_date", nullable = false) LocalDate value) {

    public ScheduledPickupDate {
        if (value == null) {
            throw new IllegalArgumentException("Scheduled pickup date is required");
        }
    }

    @JsonCreator
    public static ScheduledPickupDate fromValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return new ScheduledPickupDate(localDate);
        }
        if (value instanceof String str) {
            return new ScheduledPickupDate(LocalDate.parse(str));
        }
        if (value instanceof java.util.Map<?, ?> map) {
            Object val = map.get("value");
            if (val instanceof LocalDate localDate) {
                return new ScheduledPickupDate(localDate);
            }
            if (val instanceof String str) {
                return new ScheduledPickupDate(LocalDate.parse(str));
            }
        }
        throw new IllegalArgumentException("Cannot deserialize ScheduledPickupDate from: " + value);
    }

    @JsonValue
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate value() {
        return value;
    }
}


