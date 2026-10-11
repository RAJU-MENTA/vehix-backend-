package com.vehix.vehix.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.entity.Vehicle;
import com.vehix.vehix.entity.SupportTicket;
import com.vehix.vehix.entity.Booking;
import com.vehix.vehix.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vehix-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void searchAvailableVehiclesAppliesTextCategoryLocationAndPriceFilters() {
        User owner = userRepository.save(
                new User("Test Seller", "seller@example.com", "password", null, UserRole.SELLER));
        vehicleRepository.save(vehicle(owner, "BMW 3 Series", "BMW", "Luxury Sedan", "Dubai Marina", "180", true));
        vehicleRepository.save(vehicle(owner, "BMW X5", "BMW", "SUV", "Downtown", "240", true));
        vehicleRepository.save(vehicle(owner, "BMW 5 Series", "BMW", "Luxury Sedan", "Dubai Marina", "160", false));

        assertThat(vehicleRepository.searchAvailableVehicles(
                "bmw", "Luxury Sedan", "marina", new BigDecimal("100"), new BigDecimal("200")))
                .extracting(Vehicle::getName)
                .containsExactly("BMW 3 Series");
    }

    @Test
    void supportTicketsCanBePersistedForAdminFollowUp() {
        SupportTicket ticket = new SupportTicket();
        ticket.setFullName("Test Customer");
        ticket.setEmail("customer@example.com");
        ticket.setTopic("Booking and availability");
        ticket.setSubject("Need to change pickup time");
        ticket.setMessage("Please help me change my pickup time.");

        SupportTicket saved = supportTicketRepository.saveAndFlush(ticket);

        assertThat(supportTicketRepository.findById(saved.getId()))
                .get()
                .extracting(SupportTicket::getStatus)
                .isEqualTo("OPEN");
        assertThat(supportTicketRepository.findAllByOrderByCreatedAtDesc())
                .extracting(SupportTicket::getId)
                .contains(saved.getId());
    }

    @Test
    void bookingDatesAndTimesArePersistedAndReturnedByAvailabilityQuery() {
        User owner = userRepository.save(
                new User("Test Seller", "seller@example.com", "password", null, UserRole.SELLER));
        Vehicle vehicle = vehicleRepository.save(
                vehicle(owner, "Toyota Innova", "Toyota", "Family", "Bengaluru", "3500", true));
        User customer = userRepository.save(
                new User("Test Customer", "customer@example.com", "password", null, UserRole.CUSTOMER));
        LocalDate date = LocalDate.now().plusDays(10);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setStartDate(date);
        booking.setStartTime(LocalTime.of(10, 30));
        booking.setEndDate(date);
        booking.setEndTime(LocalTime.of(12, 0));
        booking.setTotalPrice(new BigDecimal("3500"));
        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.saveAndFlush(booking);

        assertThat(bookingRepository.findBookingCandidates(vehicle, date, date, BookingStatus.CANCELLED))
                .filteredOn(candidate -> candidate.getId().equals(saved.getId()))
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.getStartTime()).isEqualTo(LocalTime.of(10, 30));
                    assertThat(candidate.getEndTime()).isEqualTo(LocalTime.of(12, 0));
                });
    }

    private Vehicle vehicle(User owner, String name, String brand, String category,
                            String location, String price, boolean available) {
        Vehicle vehicle = new Vehicle();
        vehicle.setName(name);
        vehicle.setBrand(brand);
        vehicle.setModel(name);
        vehicle.setYear(2024);
        vehicle.setCategory(category);
        vehicle.setFuelType("Petrol");
        vehicle.setTransmission("Automatic");
        vehicle.setSeatingCapacity(5);
        vehicle.setLocation(location);
        vehicle.setPricePerDay(new BigDecimal(price));
        vehicle.setAvailable(available);
        vehicle.setOwner(owner);
        return vehicle;
    }
}
