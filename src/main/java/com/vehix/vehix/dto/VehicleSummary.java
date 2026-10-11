package com.vehix.vehix.dto;

import java.math.BigDecimal;

public record VehicleSummary(Long id, String name, String brand, String model, String imagePath,
                             BigDecimal pricePerDay) {
}
