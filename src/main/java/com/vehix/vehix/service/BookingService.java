package com.vehix.vehix.service;

import com.vehix.vehix.dto.BookingRequest;
import com.vehix.vehix.dto.BookingResponse;
import com.vehix.vehix.dto.UserSummary;
import com.vehix.vehix.dto.VehicleSummary;
import com.vehix.vehix.entity.Booking;
import com.vehix.vehix.entity.BookingStatus;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.entity.Vehicle;
import com.vehix.vehix.exception.ApiException;
import com.vehix.vehix.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleService vehicleService;

    public BookingService(BookingRepository bookingRepository, VehicleService vehicleService) {
        this.bookingRepository = bookingRepository;
        this.vehicleService = vehicleService;
    }

    @Transactional
    public BookingResponse createBooking(User customer, BookingRequest request) {
        validateBookingWindow(request.getStartDate(), request.getStartTime(), request.getEndDate(), request.getEndTime());

        Vehicle vehicle = vehicleService.getVehicleForBooking(request.getVehicleId());
        LocalDateTime start = LocalDateTime.of(request.getStartDate(), request.getStartTime());
        LocalDateTime end = LocalDateTime.of(request.getEndDate(), request.getEndTime());
        if (hasOverlap(vehicle, start, end)) {
            throw new ApiException(HttpStatus.CONFLICT, "Vehicle already booked for the selected date and time.");
        }

        long rentalMinutes = ChronoUnit.MINUTES.between(start, end);
        long rentalDays = Math.max(1, (rentalMinutes + 1439) / 1440);
        BigDecimal totalPrice = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(rentalDays));

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setStartDate(request.getStartDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndDate(request.getEndDate());
        booking.setEndTime(request.getEndTime());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(Long vehicleId, LocalDate startDate, LocalTime startTime,
                               LocalDate endDate, LocalTime endTime) {
        validateBookingWindow(startDate, startTime, endDate, endTime);
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
        return vehicle.isAvailable()
                && !hasOverlap(vehicle, LocalDateTime.of(startDate, startTime), LocalDateTime.of(endDate, endTime));
    }

    private boolean hasOverlap(Vehicle vehicle, LocalDateTime requestedStart, LocalDateTime requestedEnd) {
        List<Booking> candidates = bookingRepository.findBookingCandidates(
                vehicle, requestedStart.toLocalDate(), requestedEnd.toLocalDate(), BookingStatus.CANCELLED);
        return candidates.stream().anyMatch(booking -> {
            LocalDateTime existingStart = LocalDateTime.of(
                    booking.getStartDate(),
                    booking.getStartTime() == null ? LocalTime.MIN : booking.getStartTime());
            LocalDateTime existingEnd = booking.getEndTime() == null
                    ? booking.getEndDate().plusDays(1).atStartOfDay()
                    : LocalDateTime.of(booking.getEndDate(), booking.getEndTime());
            return requestedStart.isBefore(existingEnd) && requestedEnd.isAfter(existingStart);
        });
    }

    private void validateBookingWindow(LocalDate startDate, LocalTime startTime,
                                       LocalDate endDate, LocalTime endTime) {
        if (startDate == null || startTime == null || endDate == null || endTime == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start and end dates and times are required");
        }
        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);
        if (!end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End date and time must be after the start date and time");
        }
        if (start.isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking start must be in the future");
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(User customer) {
        return bookingRepository.findByCustomer(customer).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForSeller(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return bookingRepository.findAll().stream()
                    .map(this::toResponse)
                    .toList();
        }
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getVehicle() != null
                        && booking.getVehicle().getOwner() != null
                        && booking.getVehicle().getOwner().getId().equals(user.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse updateStatus(User currentUser, Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (currentUser.getRole() == UserRole.CUSTOMER && !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You may only manage your own bookings");
        }

        if (currentUser.getRole() == UserRole.CUSTOMER && status == BookingStatus.CANCELLED) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            return toResponse(booking);
        }

        if (currentUser.getRole() == UserRole.SELLER) {
            if (booking.getVehicle() == null
                    || booking.getVehicle().getOwner() == null
                    || !booking.getVehicle().getOwner().getId().equals(currentUser.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage bookings for your own vehicles");
            }
        }

        if (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.SELLER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to change booking status");
        }

        booking.setStatus(status);
        return toResponse(bookingRepository.save(booking));
    }

    private BookingResponse toResponse(Booking booking) {
        User customer = booking.getCustomer();
        Vehicle vehicle = booking.getVehicle();
        return new BookingResponse(
                booking.getId(),
                customer == null ? null : new UserSummary(
                        customer.getId(), customer.getFullName(), customer.getEmail(), customer.getRole().name()),
                vehicle == null ? null : new VehicleSummary(
                        vehicle.getId(), vehicle.getName(), vehicle.getBrand(), vehicle.getModel(),
                        vehicle.getImagePath(), vehicle.getPricePerDay()),
                booking.getStartDate(),
                booking.getStartTime(),
                booking.getEndDate(),
                booking.getEndTime(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt());
    }
}
