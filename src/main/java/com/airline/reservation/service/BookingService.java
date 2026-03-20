package com.airline.reservation.service;

import com.airline.reservation.dto.booking.BookingConfirmRequest;
import com.airline.reservation.dto.booking.BookingHoldRequest;
import com.airline.reservation.dto.booking.BookingResponse;
import com.airline.reservation.dto.booking.CancellationResponse;
import com.airline.reservation.dto.booking.PassengerDto;
import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.BookingPassenger;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.entity.FlightSeatInventory;
import com.airline.reservation.entity.Passenger;
import com.airline.reservation.entity.Payment;
import com.airline.reservation.entity.User;
import com.airline.reservation.enums.BookingStatus;
import com.airline.reservation.enums.PaymentStatus;
import com.airline.reservation.enums.SeatInventoryStatus;
import com.airline.reservation.exception.BusinessRuleException;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.exception.SeatUnavailableException;
import com.airline.reservation.exception.UnauthorizedActionException;
import com.airline.reservation.repository.BookingPassengerRepository;
import com.airline.reservation.repository.BookingRepository;
import com.airline.reservation.repository.FlightRepository;
import com.airline.reservation.repository.FlightSeatInventoryRepository;
import com.airline.reservation.repository.PassengerRepository;
import com.airline.reservation.repository.UserRepository;
import com.airline.reservation.util.PnrGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final PassengerRepository passengerRepository;
    private final FlightRepository flightRepository;
    private final FlightSeatInventoryRepository seatInventoryRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final PnrGenerator pnrGenerator;
    private final long holdMinutes;

    public BookingService(BookingRepository bookingRepository,
                          BookingPassengerRepository bookingPassengerRepository,
                          PassengerRepository passengerRepository,
                          FlightRepository flightRepository,
                          FlightSeatInventoryRepository seatInventoryRepository,
                          UserRepository userRepository,
                          PaymentService paymentService,
                          NotificationService notificationService,
                          PnrGenerator pnrGenerator,
                          @Value("${app.booking.hold-minutes}") long holdMinutes) {
        this.bookingRepository = bookingRepository;
        this.bookingPassengerRepository = bookingPassengerRepository;
        this.passengerRepository = passengerRepository;
        this.flightRepository = flightRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.pnrGenerator = pnrGenerator;
        this.holdMinutes = holdMinutes;
    }

    @Transactional
    public BookingResponse holdBooking(@NonNull BookingHoldRequest request, @NonNull String email) {
        String userEmail = Objects.requireNonNull(email, "User email is required.");
        Long requestedFlightId = Objects.requireNonNull(request.getFlightId(), "Flight id is required.");
        List<String> requestedSeatNumbers = request.getSeatNumbers().stream()
            .map(seat -> seat.trim().toUpperCase())
            .toList();

        if (request.getPassengers().size() != requestedSeatNumbers.size()) {
            throw new BusinessRuleException("Passengers and seat selections must match.");
        }
        if (new LinkedHashSet<>(requestedSeatNumbers).size() != requestedSeatNumbers.size()) {
            throw new BusinessRuleException("Duplicate seat selections are not allowed.");
        }

        User user = loadUser(userEmail);
        Flight flight = flightRepository.findById(requestedFlightId)
            .orElseThrow(() -> new ResourceNotFoundException("Flight not found."));
        Long flightId = Objects.requireNonNull(flight.getId(), "Flight id is required.");

        if (!flight.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot book a past flight.");
        }

        List<FlightSeatInventory> seats = Objects.requireNonNull(
            seatInventoryRepository.findSeatsForUpdate(flightId, requestedSeatNumbers),
            "Seats are required."
        );
        if (seats.size() != requestedSeatNumbers.size()) {
            throw new SeatUnavailableException("One or more seats do not exist for this flight.");
        }

        Map<String, FlightSeatInventory> seatByNumber = seats.stream()
            .collect(java.util.stream.Collectors.toMap(FlightSeatInventory::getSeatNumber, seat -> seat));

        LocalDateTime holdExpiry = LocalDateTime.now().plusMinutes(holdMinutes);
        BigDecimal totalFare = BigDecimal.ZERO;

        for (String seatNumber : requestedSeatNumbers) {
            FlightSeatInventory seat = seatByNumber.get(seatNumber);
            if (seat == null) {
                throw new SeatUnavailableException("Seat is no longer available: " + seatNumber);
            }
            normalizeExpiredHold(seat);
            if (seat.getStatus() != SeatInventoryStatus.AVAILABLE) {
                throw new SeatUnavailableException("Seat is no longer available: " + seat.getSeatNumber());
            }
            totalFare = totalFare.add(seat.getPrice());
        }

        Booking booking = new Booking();
        booking.setPnr(pnrGenerator.generate());
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setStatus(BookingStatus.RESERVED);
        booking.setTotalFare(totalFare);
        booking.setHoldExpiresAt(holdExpiry);
        booking = bookingRepository.save(booking);

        List<BookingPassenger> bookingPassengers = new ArrayList<>();
        for (int i = 0; i < request.getPassengers().size(); i++) {
            PassengerDto passengerDto = Objects.requireNonNull(
                request.getPassengers().get(i),
                "Passenger details are required."
            );
            Passenger passenger = passengerRepository.save(
                Objects.requireNonNull(toPassenger(passengerDto), "Passenger is required.")
            );
            FlightSeatInventory seat = Objects.requireNonNull(
                seatByNumber.get(requestedSeatNumbers.get(i)),
                "Seat is required."
            );

            BookingPassenger bookingPassenger = new BookingPassenger();
            bookingPassenger.setBooking(booking);
            bookingPassenger.setPassenger(passenger);
            bookingPassenger.setSeatNumber(seat.getSeatNumber());
            bookingPassenger.setFare(seat.getPrice());
            bookingPassengers.add(bookingPassenger);

            seat.setStatus(SeatInventoryStatus.HELD);
            seat.setHoldExpiresAt(holdExpiry);
            seat.setBooking(booking);
        }

        bookingPassengerRepository.saveAll(Objects.requireNonNull(bookingPassengers, "Booking passengers are required."));
        seatInventoryRepository.saveAll(Objects.requireNonNull(seats, "Seats are required."));

        notificationService.send(
            user,
            "Seat hold created",
            "Your booking hold for " + flight.getFlightNumber() + " is active until " + holdExpiry + "."
        );

        return mapBooking(booking);
    }

    @Transactional
    public BookingResponse confirmBooking(@NonNull String pnr,
                                          @NonNull BookingConfirmRequest request,
                                          @NonNull String email) {
        String bookingPnr = Objects.requireNonNull(pnr, "PNR is required.");
        BookingConfirmRequest confirmRequest = Objects.requireNonNull(request, "Booking confirm request is required.");
        String userEmail = Objects.requireNonNull(email, "User email is required.");
        Booking booking = bookingRepository.findByPnrForUpdate(bookingPnr)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
        ensureOwnership(Objects.requireNonNull(booking, "Booking is required."), userEmail);
        Long bookingId = Objects.requireNonNull(booking.getId(), "Booking id is required.");

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BusinessRuleException("Only reserved bookings can be confirmed.");
        }
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            expireBooking(Objects.requireNonNull(booking, "Booking is required."));
            throw new BusinessRuleException("This seat hold has expired.");
        }

        List<FlightSeatInventory> seats = Objects.requireNonNull(
            seatInventoryRepository.findByBookingIdForUpdate(bookingId),
            "Seats are required."
        );
        if (seats.isEmpty()) {
            throw new SeatUnavailableException("No seats are currently held for this booking.");
        }

        Payment payment = paymentService.processPayment(booking, confirmRequest);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            releaseSeats(booking, seats);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setHoldExpiresAt(null);
            bookingRepository.save(booking);
            throw new BusinessRuleException("Payment failed. The held seats have been released.");
        }

        for (FlightSeatInventory seat : seats) {
            seat.setStatus(SeatInventoryStatus.BOOKED);
            seat.setHoldExpiresAt(null);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setHoldExpiresAt(null);
        seatInventoryRepository.saveAll(Objects.requireNonNull(seats, "Seats are required."));
        bookingRepository.save(booking);

        notificationService.send(
            booking.getUser(),
            "Booking confirmed",
            "Your booking " + booking.getPnr() + " is confirmed for flight " + booking.getFlight().getFlightNumber() + "."
        );

        return mapBooking(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(@NonNull String pnr, @NonNull String email) {
        String bookingPnr = Objects.requireNonNull(pnr, "PNR is required.");
        String userEmail = Objects.requireNonNull(email, "User email is required.");
        Booking booking = bookingRepository.findByPnr(bookingPnr)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
        ensureOwnership(Objects.requireNonNull(booking, "Booking is required."), userEmail);
        return mapBooking(Objects.requireNonNull(booking, "Booking is required."));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(@NonNull Long userId, @NonNull String email) {
        Long requiredUserId = Objects.requireNonNull(userId, "User id is required.");
        String userEmail = Objects.requireNonNull(email, "User email is required.");
        User user = loadUser(userEmail);
        Long currentUserId = Objects.requireNonNull(user.getId(), "User id is required.");
        if (!currentUserId.equals(requiredUserId) && user.getRole().name().equals("CUSTOMER")) {
            throw new UnauthorizedActionException("You can only view your own bookings.");
        }
        return bookingRepository.findByUserIdOrderByBookingTimeDesc(requiredUserId).stream()
            .map(this::mapBooking)
            .toList();
    }

    @Transactional
    public CancellationResponse cancelBooking(@NonNull String pnr, @NonNull String email) {
        String bookingPnr = Objects.requireNonNull(pnr, "PNR is required.");
        String userEmail = Objects.requireNonNull(email, "User email is required.");
        Booking booking = bookingRepository.findByPnrForUpdate(bookingPnr)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
        ensureOwnership(Objects.requireNonNull(booking, "Booking is required."), userEmail);
        BookingStatus previousStatus = booking.getStatus();
        Long bookingId = Objects.requireNonNull(booking.getId(), "Booking id is required.");

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessRuleException("Booking is already inactive.");
        }

        LocalDateTime departure = booking.getFlight().getDepartureTime();
        if (!departure.minusHours(2).isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cancellation is allowed up to 2 hours before departure.");
        }

        List<FlightSeatInventory> seats = Objects.requireNonNull(
            seatInventoryRepository.findByBookingIdForUpdate(bookingId),
            "Seats are required."
        );
        releaseSeats(booking, seats);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setHoldExpiresAt(null);
        bookingRepository.save(booking);

        BigDecimal refundAmount = previousStatus == BookingStatus.RESERVED
            ? booking.getTotalFare()
            : calculateRefund(booking);
        paymentService.refundLatestPayment(booking, refundAmount);

        notificationService.send(
            booking.getUser(),
            "Booking cancelled",
            "Your booking " + booking.getPnr() + " has been cancelled. Refund amount: " + refundAmount + "."
        );

        return new CancellationResponse(booking.getPnr(), booking.getStatus().name(), refundAmount);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseExpiredHolds() {
        List<FlightSeatInventory> expiredSeats = Objects.requireNonNull(
            seatInventoryRepository.findExpiredHeldSeats(LocalDateTime.now()),
            "Expired seats are required."
        );
        for (FlightSeatInventory seat : expiredSeats) {
            Booking booking = seat.getBooking();
            if (booking != null && booking.getStatus() == BookingStatus.RESERVED) {
                booking.setStatus(BookingStatus.EXPIRED);
                booking.setHoldExpiresAt(null);
                bookingRepository.save(booking);
            }
            seat.setStatus(SeatInventoryStatus.AVAILABLE);
            seat.setHoldExpiresAt(null);
            seat.setBooking(null);
        }
        seatInventoryRepository.saveAll(Objects.requireNonNull(expiredSeats, "Expired seats are required."));
    }

    private @NonNull Passenger toPassenger(@NonNull PassengerDto dto) {
        Passenger passenger = new Passenger();
        passenger.setFullName(dto.getFullName().trim());
        passenger.setAge(dto.getAge());
        passenger.setGender(dto.getGender().trim());
        passenger.setPassportNumber(dto.getPassportNumber().trim().toUpperCase());
        return passenger;
    }

    private void ensureOwnership(@NonNull Booking booking, @NonNull String email) {
        User currentUser = loadUser(Objects.requireNonNull(email, "User email is required."));
        Long bookingUserId = Objects.requireNonNull(booking.getUser().getId(), "Booking user id is required.");
        Long currentUserId = Objects.requireNonNull(currentUser.getId(), "User id is required.");
        if (!bookingUserId.equals(currentUserId) && currentUser.getRole().name().equals("CUSTOMER")) {
            throw new UnauthorizedActionException("You do not have access to this booking.");
        }
    }

    private @NonNull User loadUser(@NonNull String email) {
        return Objects.requireNonNull(
            userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
            "User is required."
        );
    }

    private void normalizeExpiredHold(@NonNull FlightSeatInventory seat) {
        if (seat.getStatus() == SeatInventoryStatus.HELD
            && seat.getHoldExpiresAt() != null
            && seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            seat.setStatus(SeatInventoryStatus.AVAILABLE);
            seat.setHoldExpiresAt(null);
            seat.setBooking(null);
        }
    }

    private void expireBooking(@NonNull Booking booking) {
        Long bookingId = Objects.requireNonNull(booking.getId(), "Booking id is required.");
        List<FlightSeatInventory> seats = Objects.requireNonNull(
            seatInventoryRepository.findByBookingIdForUpdate(bookingId),
            "Seats are required."
        );
        releaseSeats(booking, seats);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setHoldExpiresAt(null);
        bookingRepository.save(booking);
    }

    private void releaseSeats(@NonNull Booking booking, @NonNull List<FlightSeatInventory> seats) {
        Long bookingId = Objects.requireNonNull(booking.getId(), "Booking id is required.");
        for (FlightSeatInventory seat : seats) {
            if (seat.getBooking() != null && Objects.requireNonNull(seat.getBooking().getId(), "Seat booking id is required.").equals(bookingId)) {
                seat.setStatus(SeatInventoryStatus.AVAILABLE);
                seat.setHoldExpiresAt(null);
                seat.setBooking(null);
            }
        }
        seatInventoryRepository.saveAll(Objects.requireNonNull(seats, "Seats are required."));
    }

    private BigDecimal calculateRefund(@NonNull Booking booking) {
        long hoursToDeparture = Duration.between(LocalDateTime.now(), booking.getFlight().getDepartureTime()).toHours();
        BigDecimal percentage = hoursToDeparture > 24 ? BigDecimal.ONE
            : hoursToDeparture > 6 ? BigDecimal.valueOf(0.8)
            : BigDecimal.valueOf(0.5);
        return booking.getTotalFare().multiply(percentage).setScale(2, RoundingMode.HALF_UP);
    }

    private BookingResponse mapBooking(@NonNull Booking booking) {
        Long bookingId = Objects.requireNonNull(booking.getId(), "Booking id is required.");
        Long flightId = Objects.requireNonNull(booking.getFlight().getId(), "Flight id is required.");
        List<String> seatNumbers = bookingPassengerRepository.findByBookingId(bookingId).stream()
            .map(BookingPassenger::getSeatNumber)
            .sorted()
            .toList();

        BookingResponse response = new BookingResponse();
        response.setPnr(booking.getPnr());
        response.setBookingId(bookingId);
        response.setStatus(booking.getStatus());
        response.setFlightId(flightId);
        response.setFlightNumber(booking.getFlight().getFlightNumber());
        response.setRoute(booking.getFlight().getSource().getCode() + " -> " + booking.getFlight().getDestination().getCode());
        response.setDepartureTime(booking.getFlight().getDepartureTime());
        response.setTotalFare(booking.getTotalFare());
        response.setBookingTime(booking.getBookingTime());
        response.setHoldExpiresAt(booking.getHoldExpiresAt());
        response.setSeatNumbers(seatNumbers);
        return response;
    }
}
