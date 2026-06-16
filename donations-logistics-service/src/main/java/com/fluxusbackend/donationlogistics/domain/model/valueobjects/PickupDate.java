package com.fluxusbackend.donationlogistics.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public record PickupDate(@Column(name = "pickup_date") LocalDate value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PickupDate {
        if (value == null) {
            throw new IllegalArgumentException("Pickup date is required");
        }
    }

    @JsonValue
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate value() {
        return value;
    }
}


