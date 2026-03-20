package com.airline.reservation.controller;

import com.airline.reservation.dto.admin.AircraftRequest;
import com.airline.reservation.dto.admin.AirportRequest;
import com.airline.reservation.dto.admin.FlightRequest;
import com.airline.reservation.entity.Aircraft;
import com.airline.reservation.entity.Airport;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/airports")
    public ResponseEntity<Airport> createAirport(@Valid @RequestBody @NonNull AirportRequest request) {
        AirportRequest airportRequest = Objects.requireNonNull(request, "Airport request is required.");
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAirport(airportRequest));
    }

    @PostMapping("/aircraft")
    public ResponseEntity<Aircraft> createAircraft(@Valid @RequestBody @NonNull AircraftRequest request) {
        AircraftRequest aircraftRequest = Objects.requireNonNull(request, "Aircraft request is required.");
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAircraft(aircraftRequest));
    }

    @PostMapping("/flights")
    public ResponseEntity<Flight> createFlight(@Valid @RequestBody @NonNull FlightRequest request) {
        FlightRequest flightRequest = Objects.requireNonNull(request, "Flight request is required.");
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createFlight(flightRequest));
    }

    @PutMapping("/flights/{id}")
    public ResponseEntity<Flight> updateFlight(@PathVariable @NonNull Long id,
                                               @Valid @RequestBody @NonNull FlightRequest request) {
        Long flightId = Objects.requireNonNull(id, "Flight id is required.");
        FlightRequest flightRequest = Objects.requireNonNull(request, "Flight request is required.");
        return ResponseEntity.ok(adminService.updateFlight(flightId, flightRequest));
    }

    @GetMapping("/flights")
    public ResponseEntity<List<Flight>> listFlights() {
        return ResponseEntity.ok(adminService.listFlights());
    }
}
