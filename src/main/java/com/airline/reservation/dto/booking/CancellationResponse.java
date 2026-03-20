package com.airline.reservation.dto.booking;

import java.math.BigDecimal;

public class CancellationResponse {

    private String pnr;
    private String status;
    private BigDecimal refundAmount;

    public CancellationResponse() {
    }

    public CancellationResponse(String pnr, String status, BigDecimal refundAmount) {
        this.pnr = pnr;
        this.status = status;
        this.refundAmount = refundAmount;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
}
