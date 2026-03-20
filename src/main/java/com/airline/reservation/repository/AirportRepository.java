package com.airline.reservation.repository;

import com.airline.reservation.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long> {

    Optional<Airport> findByCodeIgnoreCase(String code);
}
