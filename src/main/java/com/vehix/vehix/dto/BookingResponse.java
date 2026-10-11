package com.vehix.vehix.dto;

import com.vehix.vehix.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookingResponse(Long id, UserSummary customer, VehicleSummary vehicle,
                              LocalDate startDate, LocalTime startTime,
                              LocalDate endDate, LocalTime endTime,
                              BigDecimal totalPrice, BookingStatus status, LocalDateTime createdAt) {
}
