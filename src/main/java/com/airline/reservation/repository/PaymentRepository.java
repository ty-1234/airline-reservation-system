package com.airline.reservation.repository;

import com.airline.reservation.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingIdOrderByPaymentTimeDesc(Long bookingId);
}
