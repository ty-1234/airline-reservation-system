package com.airline.reservation.repository;

import com.airline.reservation.entity.FlightSeatInventory;
import com.airline.reservation.enums.SeatClass;
import com.airline.reservation.enums.SeatInventoryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FlightSeatInventoryRepository extends JpaRepository<FlightSeatInventory, Long> {

    @EntityGraph(attributePaths = {"flight", "booking"})
    List<FlightSeatInventory> findByFlightIdOrderBySeatNumberAsc(Long flightId);

    long countByFlightIdAndSeatClassAndStatus(Long flightId, SeatClass seatClass, SeatInventoryStatus status);

    @Query("""
        select min(fsi.price) from FlightSeatInventory fsi
        where fsi.flight.id = :flightId
          and fsi.seatClass = :seatClass
          and fsi.status = 'AVAILABLE'
        """)
    java.math.BigDecimal findLowestAvailableFare(@Param("flightId") Long flightId, @Param("seatClass") SeatClass seatClass);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select fsi from FlightSeatInventory fsi
        where fsi.flight.id = :flightId
          and fsi.seatNumber in :seatNumbers
        """)
    List<FlightSeatInventory> findSeatsForUpdate(@Param("flightId") Long flightId, @Param("seatNumbers") List<String> seatNumbers);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select fsi from FlightSeatInventory fsi
        where fsi.booking.id = :bookingId
        """)
    List<FlightSeatInventory> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    @Query("""
        select fsi from FlightSeatInventory fsi
        where fsi.status = 'HELD'
          and fsi.holdExpiresAt < :now
        """)
    List<FlightSeatInventory> findExpiredHeldSeats(@Param("now") LocalDateTime now);
}
