package com.airline.reservation.service;

import com.airline.reservation.dto.flight.FlightResponse;
import com.airline.reservation.dto.flight.FlightSearchRequest;
import com.airline.reservation.dto.flight.SeatInventoryResponse;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.entity.FlightSeatInventory;
import com.airline.reservation.enums.FlightStatus;
import com.airline.reservation.enums.SeatClass;
import com.airline.reservation.enums.SeatInventoryStatus;
import com.airline.reservation.exception.BusinessRuleException;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.repository.FlightRepository;
import com.airline.reservation.repository.FlightSeatInventoryRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightSeatInventoryRepository seatInventoryRepository;

    public FlightService(FlightRepository flightRepository, FlightSeatInventoryRepository seatInventoryRepository) {
        this.flightRepository = flightRepository;
        this.seatInventoryRepository = seatInventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<FlightResponse> searchFlights(@NonNull FlightSearchRequest request) {
        SeatClass seatClass = Objects.requireNonNull(request.getSeatClass(), "Seat class is required.");
        if (request.getSource().equalsIgnoreCase(request.getDestination())) {
            throw new BusinessRuleException("Source and destination cannot be the same.");
        }

        LocalDateTime start = request.getTravelDate().atStartOfDay();
        LocalDateTime end = request.getTravelDate().plusDays(1).atStartOfDay().minusNanos(1);

        return flightRepository.searchFlights(
                request.getSource(),
                request.getDestination(),
                start,
                end,
                FlightStatus.SCHEDULED
            ).stream()
            .map(flight -> mapFlight(Objects.requireNonNull(flight, "Flight is required."), seatClass))
            .filter(response -> response.getAvailableSeats() >= request.getPassengers())
            .toList();
    }

    @Transactional(readOnly = true)
    public FlightResponse getFlight(@NonNull Long flightId) {
        Long requiredFlightId = Objects.requireNonNull(flightId, "Flight id is required.");
        Flight flight = flightRepository.findById(requiredFlightId)
            .orElseThrow(() -> new ResourceNotFoundException("Flight not found."));
        return mapFlight(Objects.requireNonNull(flight, "Flight is required."), SeatClass.ECONOMY);
    }

    @Transactional(readOnly = true)
    public List<SeatInventoryResponse> getSeats(@NonNull Long flightId) {
        Long requiredFlightId = Objects.requireNonNull(flightId, "Flight id is required.");
        if (!flightRepository.existsById(requiredFlightId)) {
            throw new ResourceNotFoundException("Flight not found.");
        }

        return seatInventoryRepository.findByFlightIdOrderBySeatNumberAsc(requiredFlightId).stream()
            .map(this::mapSeat)
            .toList();
    }

    private FlightResponse mapFlight(@NonNull Flight flight, @NonNull SeatClass seatClass) {
        Long flightId = Objects.requireNonNull(flight.getId(), "Flight id is required.");
        FlightResponse response = new FlightResponse();
        response.setId(flightId);
        response.setFlightNumber(flight.getFlightNumber());
        response.setSourceCode(flight.getSource().getCode());
        response.setSourceCity(flight.getSource().getCity());
        response.setDestinationCode(flight.getDestination().getCode());
        response.setDestinationCity(flight.getDestination().getCity());
        response.setDepartureTime(flight.getDepartureTime());
        response.setArrivalTime(flight.getArrivalTime());
        response.setAircraftModel(flight.getAircraft().getModel());
        response.setStatus(flight.getStatus());
        response.setAvailableSeats(seatInventoryRepository.countByFlightIdAndSeatClassAndStatus(
            flightId,
            seatClass,
            SeatInventoryStatus.AVAILABLE
        ));

        BigDecimal lowestFare = seatInventoryRepository.findLowestAvailableFare(flightId, seatClass);
        response.setStartingFare(lowestFare == null ? BigDecimal.ZERO : lowestFare);
        return response;
    }

    private SeatInventoryResponse mapSeat(@NonNull FlightSeatInventory seat) {
        SeatInventoryResponse response = new SeatInventoryResponse();
        response.setSeatNumber(seat.getSeatNumber());
        response.setSeatClass(seat.getSeatClass());
        response.setStatus(seat.getStatus());
        response.setPrice(seat.getPrice());
        response.setHoldExpiresAt(seat.getHoldExpiresAt());
        return response;
    }
}
