package com.airline.reservation.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AircraftRequest {

    @NotBlank
    private String model;

    @Min(1)
    private int totalSeats;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }
}
