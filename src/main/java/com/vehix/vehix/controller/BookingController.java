package com.vehix.vehix.controller;

import com.vehix.vehix.dto.BookingRequest;
import com.vehix.vehix.dto.BookingResponse;
import com.vehix.vehix.dto.AvailabilityResponse;
import com.vehix.vehix.entity.BookingStatus;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.service.BookingService;
import com.vehix.vehix.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @GetMapping("/vehicles/{vehicleId}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        boolean available = bookingService.isAvailable(vehicleId, startDate, startTime, endDate, endTime);
        return ResponseEntity.ok(new AvailabilityResponse(
                available,
                available ? "This vehicle is available for your selected time." :
                        "This vehicle is not available for your selected date and time."));
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.status(201).body(bookingService.createBooking(currentUser, request));
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(bookingService.getMyBookings(currentUser));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getAllBookings(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(bookingService.getBookingsForSeller(currentUser));
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(@PathVariable Long id,
                                                         @RequestParam String status,
                                                         Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(bookingService.updateStatus(currentUser, id, bookingStatus));
    }
}
