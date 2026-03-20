package com.airline.reservation.config;

import com.airline.reservation.dto.admin.AircraftRequest;
import com.airline.reservation.dto.admin.AirportRequest;
import com.airline.reservation.dto.admin.FlightRequest;
import com.airline.reservation.entity.Aircraft;
import com.airline.reservation.entity.Airport;
import com.airline.reservation.entity.User;
import com.airline.reservation.enums.FlightStatus;
import com.airline.reservation.enums.Role;
import com.airline.reservation.repository.AircraftRepository;
import com.airline.reservation.repository.AirportRepository;
import com.airline.reservation.repository.UserRepository;
import com.airline.reservation.service.AdminService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               AirportRepository airportRepository,
                               AircraftRepository aircraftRepository,
                               PasswordEncoder passwordEncoder,
                               AdminService adminService) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            User admin = new User();
            admin.setFullName("System Admin");
            admin.setEmail("admin@airline.com");
            admin.setPhone("+441111111111");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            User customer = new User();
            customer.setFullName("Demo Customer");
            customer.setEmail("customer@airline.com");
            customer.setPhone("+442222222222");
            customer.setPasswordHash(passwordEncoder.encode("Customer@123"));
            customer.setRole(Role.CUSTOMER);
            userRepository.save(customer);

            Airport lhr = createAirportIfMissing(airportRepository, adminService, "LHR", "Heathrow Airport", "London", "United Kingdom");
            Airport dxb = createAirportIfMissing(airportRepository, adminService, "DXB", "Dubai International Airport", "Dubai", "UAE");
            Airport jfk = createAirportIfMissing(airportRepository, adminService, "JFK", "John F. Kennedy International Airport", "New York", "USA");

            Aircraft aircraft = aircraftRepository.findAll().stream().findFirst().orElseGet(() -> {
                AircraftRequest request = new AircraftRequest();
                request.setModel("Boeing 737-800");
                request.setTotalSeats(180);
                return adminService.createAircraft(request);
            });

            if (airportRepository.count() >= 3 && aircraftRepository.count() >= 1) {
                seedFlight(adminService, aircraft, lhr, dxb, "AR101", LocalDateTime.now().plusDays(3).withHour(9).withMinute(30));
                seedFlight(adminService, aircraft, dxb, jfk, "AR202", LocalDateTime.now().plusDays(4).withHour(14).withMinute(15));
                seedFlight(adminService, aircraft, jfk, lhr, "AR303", LocalDateTime.now().plusDays(5).withHour(20).withMinute(0));
            }
        };
    }

    private Airport createAirportIfMissing(AirportRepository repository,
                                           AdminService adminService,
                                           String code,
                                           String name,
                                           String city,
                                           String country) {
        return repository.findByCodeIgnoreCase(code).orElseGet(() -> {
            AirportRequest request = new AirportRequest();
            request.setCode(code);
            request.setName(name);
            request.setCity(city);
            request.setCountry(country);
            return adminService.createAirport(request);
        });
    }

    private void seedFlight(AdminService adminService,
                            Aircraft aircraft,
                            Airport source,
                            Airport destination,
                            String flightNumber,
                            LocalDateTime departure) {
        FlightRequest request = new FlightRequest();
        request.setFlightNumber(flightNumber);
        request.setSourceCode(source.getCode());
        request.setDestinationCode(destination.getCode());
        request.setAircraftId(aircraft.getId());
        request.setDepartureTime(departure.withSecond(0).withNano(0));
        request.setArrivalTime(departure.plusHours(7).withSecond(0).withNano(0));
        request.setStatus(FlightStatus.SCHEDULED);
        adminService.createFlight(request);
    }
}
