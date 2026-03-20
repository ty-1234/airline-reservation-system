package com.airline.reservation.controller;

import com.airline.reservation.dto.booking.BookingResponse;
import com.airline.reservation.service.BookingService;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final BookingService bookingService;

    public UserController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable @NonNull Long id,
                                                                 @NonNull Authentication authentication) {
        Long userId = Objects.requireNonNull(id, "User id is required.");
        String email = Objects.requireNonNull(authentication.getName(), "Authenticated user is required.");
        return ResponseEntity.ok(bookingService.getUserBookings(userId, email));
    }
}
