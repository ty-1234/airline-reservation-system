package com.airline.reservation.controller;

import com.airline.reservation.dto.booking.BookingConfirmRequest;
import com.airline.reservation.dto.booking.BookingHoldRequest;
import com.airline.reservation.dto.booking.BookingResponse;
import com.airline.reservation.dto.booking.CancellationResponse;
import com.airline.reservation.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/hold")
    public ResponseEntity<BookingResponse> holdBooking(@Valid @RequestBody @NonNull BookingHoldRequest request,
                                                       @NonNull Authentication authentication) {
        BookingHoldRequest holdRequest = Objects.requireNonNull(request, "Booking hold request is required.");
        String email = Objects.requireNonNull(authentication.getName(), "Authenticated user is required.");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(bookingService.holdBooking(holdRequest, email));
    }

    @PostMapping("/{pnr}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable @NonNull String pnr,
                                                          @Valid @RequestBody @NonNull BookingConfirmRequest request,
                                                          @NonNull Authentication authentication) {
        String bookingPnr = Objects.requireNonNull(pnr, "PNR is required.");
        BookingConfirmRequest confirmRequest = Objects.requireNonNull(request, "Booking confirm request is required.");
        String email = Objects.requireNonNull(authentication.getName(), "Authenticated user is required.");
        return ResponseEntity.ok(bookingService.confirmBooking(bookingPnr, confirmRequest, email));
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable @NonNull String pnr,
                                                      @NonNull Authentication authentication) {
        String bookingPnr = Objects.requireNonNull(pnr, "PNR is required.");
        String email = Objects.requireNonNull(authentication.getName(), "Authenticated user is required.");
        return ResponseEntity.ok(bookingService.getBooking(bookingPnr, email));
    }

    @PostMapping("/{pnr}/cancel")
    public ResponseEntity<CancellationResponse> cancelBooking(@PathVariable @NonNull String pnr,
                                                              @NonNull Authentication authentication) {
        String bookingPnr = Objects.requireNonNull(pnr, "PNR is required.");
        String email = Objects.requireNonNull(authentication.getName(), "Authenticated user is required.");
        return ResponseEntity.ok(bookingService.cancelBooking(bookingPnr, email));
    }
}
