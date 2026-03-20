package com.airline.reservation.repository;

import com.airline.reservation.entity.Flight;
import com.airline.reservation.enums.FlightStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    @EntityGraph(attributePaths = {"source", "destination", "aircraft"})
    List<Flight> findAllByOrderByDepartureTimeAsc();

    @EntityGraph(attributePaths = {"source", "destination", "aircraft"})
    @Query("""
        select f from Flight f
        where upper(f.source.code) = upper(:source)
          and upper(f.destination.code) = upper(:destination)
          and f.departureTime between :start and :end
          and f.status = :status
        order by f.departureTime asc
        """)
    List<Flight> searchFlights(@Param("source") String source,
                               @Param("destination") String destination,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end,
                               @Param("status") FlightStatus status);
}
