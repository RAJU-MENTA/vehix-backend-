package com.vehix.vehix.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vehix.vehix.dto.BookingRequest;
import com.vehix.vehix.entity.Booking;
import com.vehix.vehix.entity.BookingStatus;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.entity.Vehicle;
import com.vehix.vehix.exception.ApiException;
import com.vehix.vehix.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private BookingService bookingService;

    private Vehicle vehicle;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setName("Toyota Test");
        vehicle.setPricePerDay(new BigDecimal("4500"));
        bookingDate = LocalDate.now().plusDays(5);
    }

    @Test
    void rejectsAReservationThatOverlapsAnExistingBookingTime() {
        when(vehicleService.getVehicleForBooking(1L)).thenReturn(vehicle);
        when(bookingRepository.findBookingCandidates(vehicle, bookingDate, bookingDate, BookingStatus.CANCELLED))
                .thenReturn(List.of(existingBooking(LocalTime.of(10, 0), LocalTime.of(12, 0))));

        assertThatThrownBy(() -> bookingService.createBooking(newCustomer(), request(
                bookingDate, LocalTime.of(11, 30), bookingDate, LocalTime.of(13, 0))))
                .isInstanceOf(ApiException.class)
                .hasMessage("Vehicle already booked for the selected date and time.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void refusesBookingWhenVehicleWasRemovedFromThePortal() {
        when(vehicleService.getVehicleForBooking(1L))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "This vehicle is no longer available for booking"));

        assertThatThrownBy(() -> bookingService.createBooking(newCustomer(), request(
                bookingDate, LocalTime.of(10, 0), bookingDate, LocalTime.of(11, 0))))
                .isInstanceOf(ApiException.class)
                .hasMessage("This vehicle is no longer available for booking")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void reportsRemovedVehicleAsUnavailable() {
        vehicle.setAvailable(false);
        when(vehicleService.getVehicleById(1L)).thenReturn(vehicle);

        boolean available = bookingService.isAvailable(
                1L, bookingDate, LocalTime.of(10, 0), bookingDate, LocalTime.of(11, 0));

        assertThat(available).isFalse();
    }

    @Test
    void allowsAnAdjacentTimeSlotAndPricesItAsOneRentalDay() {
        when(vehicleService.getVehicleForBooking(1L)).thenReturn(vehicle);
        when(bookingRepository.findBookingCandidates(vehicle, bookingDate, bookingDate, BookingStatus.CANCELLED))
                .thenReturn(List.of(existingBooking(LocalTime.of(10, 0), LocalTime.of(12, 0))));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = bookingService.createBooking(newCustomer(), request(
                bookingDate, LocalTime.of(12, 0), bookingDate, LocalTime.of(13, 0)));

        assertThat(result.totalPrice()).isEqualByComparingTo("4500");
        assertThat(result.startTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(result.endTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(result.vehicle().name()).isEqualTo(vehicle.getName());
        assertThat(result.customer().getFullName()).isEqualTo("Test Customer");
        ArgumentCaptor<Booking> savedBooking = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(savedBooking.capture());
        assertThat(savedBooking.getValue().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    private Booking existingBooking(LocalTime start, LocalTime end) {
        Booking booking = new Booking();
        booking.setVehicle(vehicle);
        booking.setStartDate(bookingDate);
        booking.setStartTime(start);
        booking.setEndDate(bookingDate);
        booking.setEndTime(end);
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }

    private BookingRequest request(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime) {
        BookingRequest request = new BookingRequest();
        request.setVehicleId(1L);
        request.setStartDate(startDate);
        request.setStartTime(startTime);
        request.setEndDate(endDate);
        request.setEndTime(endTime);
        return request;
    }

    private User newCustomer() {
        return new User("Test Customer", "customer@example.com", "password", null, UserRole.CUSTOMER);
    }
}
