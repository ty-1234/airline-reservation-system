package com.airline.reservation.service;

import com.airline.reservation.dto.admin.AircraftRequest;
import com.airline.reservation.dto.admin.AirportRequest;
import com.airline.reservation.dto.admin.FlightRequest;
import com.airline.reservation.entity.Aircraft;
import com.airline.reservation.entity.Airport;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.entity.FlightSeatInventory;
import com.airline.reservation.enums.SeatClass;
import com.airline.reservation.enums.SeatInventoryStatus;
import com.airline.reservation.exception.BusinessRuleException;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.repository.AircraftRepository;
import com.airline.reservation.repository.AirportRepository;
import com.airline.reservation.repository.FlightRepository;
import com.airline.reservation.repository.FlightSeatInventoryRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AdminService {

    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;
    private final FlightSeatInventoryRepository seatInventoryRepository;

    public AdminService(AirportRepository airportRepository,
                        AircraftRepository aircraftRepository,
                        FlightRepository flightRepository,
                        FlightSeatInventoryRepository seatInventoryRepository) {
        this.airportRepository = airportRepository;
        this.aircraftRepository = aircraftRepository;
        this.flightRepository = flightRepository;
        this.seatInventoryRepository = seatInventoryRepository;
    }

    public Airport createAirport(AirportRequest request) {
        Airport airport = new Airport();
        airport.setCode(request.getCode().trim().toUpperCase());
        airport.setName(request.getName().trim());
        airport.setCity(request.getCity().trim());
        airport.setCountry(request.getCountry().trim());
        return airportRepository.save(airport);
    }

    public Aircraft createAircraft(AircraftRequest request) {
        Aircraft aircraft = new Aircraft();
        aircraft.setModel(request.getModel().trim());
        aircraft.setTotalSeats(request.getTotalSeats());
        return aircraftRepository.save(aircraft);
    }

    @Transactional
    public Flight createFlight(@NonNull FlightRequest request) {
        Flight flight = new Flight();
        applyFlightRequest(flight, request);
        Flight savedFlight = flightRepository.save(flight);
        seatInventoryRepository.saveAll(buildSeatInventory(savedFlight));
        return savedFlight;
    }

    @Transactional
    public Flight updateFlight(@NonNull Long id, @NonNull FlightRequest request) {
        Flight flight = flightRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight not found."));

        Long previousAircraftId = flight.getAircraft().getId();
        Long requestedAircraftId = Objects.requireNonNull(request.getAircraftId(), "Aircraft id is required.");
        boolean aircraftChanged = !previousAircraftId.equals(requestedAircraftId);
        if (aircraftChanged && !seatInventoryRepository.findByFlightIdOrderBySeatNumberAsc(id).isEmpty()) {
            throw new BusinessRuleException("Aircraft cannot be changed after seat inventory has been created for a flight.");
        }

        applyFlightRequest(flight, request);
        Flight savedFlight = flightRepository.save(flight);
        return savedFlight;
    }

    @Transactional(readOnly = true)
    public List<Flight> listFlights() {
        return flightRepository.findAllByOrderByDepartureTimeAsc();
    }

    private void applyFlightRequest(@NonNull Flight flight, @NonNull FlightRequest request) {
        Long aircraftId = Objects.requireNonNull(request.getAircraftId(), "Aircraft id is required.");

        if (request.getSourceCode().equalsIgnoreCase(request.getDestinationCode())) {
            throw new BusinessRuleException("Source and destination cannot be the same.");
        }
        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new BusinessRuleException("Arrival time must be after departure time.");
        }

        Airport source = airportRepository.findByCodeIgnoreCase(request.getSourceCode())
            .orElseThrow(() -> new ResourceNotFoundException("Source airport not found."));
        Airport destination = airportRepository.findByCodeIgnoreCase(request.getDestinationCode())
            .orElseThrow(() -> new ResourceNotFoundException("Destination airport not found."));
        Aircraft aircraft = aircraftRepository.findById(aircraftId)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found."));

        flight.setFlightNumber(request.getFlightNumber().trim().toUpperCase());
        flight.setSource(source);
        flight.setDestination(destination);
        flight.setAircraft(aircraft);
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setStatus(request.getStatus());
    }

    private @NonNull List<FlightSeatInventory> buildSeatInventory(Flight flight) {
        int totalSeats = flight.getAircraft().getTotalSeats();
        int rows = (int) Math.ceil(totalSeats / 6.0);
        List<FlightSeatInventory> inventory = new ArrayList<>();
        char[] columns = {'A', 'B', 'C', 'D', 'E', 'F'};
        int seatCounter = 0;

        for (int row = 1; row <= rows && seatCounter < totalSeats; row++) {
            for (char column : columns) {
                if (seatCounter >= totalSeats) {
                    break;
                }
                FlightSeatInventory seat = new FlightSeatInventory();
                seat.setFlight(flight);
                seat.setSeatNumber(row + String.valueOf(column));
                seat.setSeatClass(resolveSeatClass(row));
                seat.setStatus(SeatInventoryStatus.AVAILABLE);
                seat.setPrice(resolvePrice(seat.getSeatClass()));
                inventory.add(seat);
                seatCounter++;
            }
        }

        return inventory;
    }

    private SeatClass resolveSeatClass(int row) {
        if (row <= 2) {
            return SeatClass.FIRST;
        }
        if (row <= 6) {
            return SeatClass.BUSINESS;
        }
        return SeatClass.ECONOMY;
    }

    private BigDecimal resolvePrice(SeatClass seatClass) {
        return switch (seatClass) {
            case FIRST -> BigDecimal.valueOf(1200);
            case BUSINESS -> BigDecimal.valueOf(700);
            case ECONOMY -> BigDecimal.valueOf(250);
        };
    }
}
