package com.airline.reservation.dto.flight;

import com.airline.reservation.enums.SeatClass;
import com.airline.reservation.enums.SeatInventoryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SeatInventoryResponse {

    private String seatNumber;
    private SeatClass seatClass;
    private SeatInventoryStatus status;
    private BigDecimal price;
    private LocalDateTime holdExpiresAt;

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatClass getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(SeatClass seatClass) {
        this.seatClass = seatClass;
    }

    public SeatInventoryStatus getStatus() {
        return status;
    }

    public void setStatus(SeatInventoryStatus status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }
}
