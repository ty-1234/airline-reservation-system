package com.airline.reservation.controller;

import com.airline.reservation.dto.flight.FlightResponse;
import com.airline.reservation.dto.flight.FlightSearchRequest;
import com.airline.reservation.dto.flight.SeatInventoryResponse;
import com.airline.reservation.enums.SeatClass;
import com.airline.reservation.service.FlightService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<FlightResponse>> searchFlights(@RequestParam String source,
                                                              @RequestParam String destination,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                              @RequestParam(defaultValue = "1") int passengers,
                                                              @RequestParam SeatClass seatClass) {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setSource(source);
        request.setDestination(destination);
        request.setTravelDate(date);
        request.setPassengers(passengers);
        request.setSeatClass(seatClass);
        return ResponseEntity.ok(flightService.searchFlights(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable @NonNull Long id) {
        Long flightId = Objects.requireNonNull(id, "Flight id is required.");
        return ResponseEntity.ok(flightService.getFlight(flightId));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatInventoryResponse>> getSeats(@PathVariable @NonNull Long id) {
        Long flightId = Objects.requireNonNull(id, "Flight id is required.");
        return ResponseEntity.ok(flightService.getSeats(flightId));
    }
}
