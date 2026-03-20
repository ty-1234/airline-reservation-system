package com.airline.reservation.service;

import com.airline.reservation.dto.booking.BookingConfirmRequest;
import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.Payment;
import com.airline.reservation.enums.PaymentStatus;
import com.airline.reservation.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment processPayment(Booking booking, BookingConfirmRequest request) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalFare());

        if ("FAIL".equalsIgnoreCase(request.getPaymentReference())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionReference("FAILED-" + UUID.randomUUID());
            return paymentRepository.save(payment);
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionReference(resolveReference(request));
        return paymentRepository.save(payment);
    }

    public BigDecimal refundLatestPayment(Booking booking, BigDecimal refundAmount) {
        List<Payment> payments = paymentRepository.findByBookingIdOrderByPaymentTimeDesc(booking.getId());
        Payment latestSuccess = payments.stream()
            .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
            .findFirst()
            .orElse(null);

        if (latestSuccess != null) {
            latestSuccess.setStatus(PaymentStatus.REFUNDED);
            latestSuccess.setAmount(refundAmount);
            paymentRepository.save(latestSuccess);
        }

        return refundAmount;
    }

    private String resolveReference(BookingConfirmRequest request) {
        if (request.getPaymentReference() != null && !request.getPaymentReference().isBlank()) {
            return request.getPaymentReference();
        }
        return request.getPaymentMethod().toUpperCase() + "-" + UUID.randomUUID();
    }
}
