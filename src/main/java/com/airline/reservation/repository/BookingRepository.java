package com.airline.reservation.repository;

import com.airline.reservation.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"flight", "flight.source", "flight.destination", "user"})
    Optional<Booking> findByPnr(String pnr);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b join fetch b.flight f join fetch f.source join fetch f.destination join fetch b.user where b.pnr = :pnr")
    Optional<Booking> findByPnrForUpdate(@Param("pnr") String pnr);

    @EntityGraph(attributePaths = {"flight", "flight.source", "flight.destination"})
    List<Booking> findByUserIdOrderByBookingTimeDesc(Long userId);
}
