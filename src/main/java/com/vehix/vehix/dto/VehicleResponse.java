package com.vehix.vehix.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleResponse(Long id, String name, String brand, String model, Integer year,
                              String category, String fuelType, String transmission,
                              Integer seatingCapacity, String location, BigDecimal pricePerDay,
                              String description, String imagePath, boolean available,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
}
