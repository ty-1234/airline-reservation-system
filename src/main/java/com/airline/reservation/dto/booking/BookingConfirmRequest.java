package com.airline.reservation.dto.booking;

import jakarta.validation.constraints.NotBlank;

public class BookingConfirmRequest {

    @NotBlank
    private String paymentMethod;

    private String paymentReference;

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
}
