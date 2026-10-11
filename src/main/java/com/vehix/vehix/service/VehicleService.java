package com.vehix.vehix.service;

import com.vehix.vehix.dto.VehicleRequest;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.entity.Vehicle;
import com.vehix.vehix.exception.ApiException;
import com.vehix.vehix.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final FileStorageService fileStorageService;

    public VehicleService(VehicleRepository vehicleRepository, FileStorageService fileStorageService) {
        this.vehicleRepository = vehicleRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByAvailableTrue();
    }

    public List<Vehicle> searchAvailableVehicles(String search, String category, String location,
                                                 BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Prices cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum price cannot exceed maximum price");
        }

        Comparator<Vehicle> comparator = switch (sortBy == null ? "" : sortBy) {
            case "price-low" -> Comparator.comparing(Vehicle::getPricePerDay);
            case "price-high" -> Comparator.comparing(Vehicle::getPricePerDay).reversed();
            case "year-newest" -> Comparator.comparing(Vehicle::getYear).reversed();
            case "" -> null;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported vehicle sort order");
        };

        List<Vehicle> vehicles = vehicleRepository.searchAvailableVehicles(
                normalize(search), normalize(category), normalize(location), minPrice, maxPrice);
        if (comparator != null) {
            vehicles.sort(comparator);
        }
        return vehicles;
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    public Vehicle getVehicleForBooking(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdForBooking(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (!vehicle.isAvailable()) {
            throw new ApiException(HttpStatus.CONFLICT, "This vehicle is no longer available for booking");
        }
        return vehicle;
    }

    public List<Vehicle> getVehiclesForOwner(User owner) {
        return owner.getRole() == UserRole.ADMIN
                ? vehicleRepository.findAll()
                : vehicleRepository.findByOwner(owner);
    }

    public Vehicle createVehicle(User owner, VehicleRequest request, MultipartFile image) {
        Vehicle vehicle = new Vehicle();
        applyRequest(vehicle, request);
        vehicle.setOwner(owner);

        if (image != null && !image.isEmpty()) {
            if (!isAllowedImageType(image.getOriginalFilename())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WEBP image types are allowed");
            }
            if (image.getSize() > 5 * 1024 * 1024) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Image size must be less than 5MB");
            }
            vehicle.setImagePath(fileStorageService.store(image));
        } else if (request.getImagePath() != null && !request.getImagePath().isBlank()) {
            vehicle.setImagePath(request.getImagePath());
        }

        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(User currentUser, Long id, VehicleRequest request, MultipartFile image) {
        Vehicle vehicle = getVehicleById(id);

        if (!currentUser.getRole().equals(UserRole.ADMIN) && !vehicle.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to edit this vehicle");
        }

        String previousImage = vehicle.getImagePath();
        applyRequest(vehicle, request);

        if (image != null && !image.isEmpty()) {
            if (!isAllowedImageType(image.getOriginalFilename())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WEBP image types are allowed");
            }
            if (image.getSize() > 5 * 1024 * 1024) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Image size must be less than 5MB");
            }
            vehicle.setImagePath(fileStorageService.store(image));
            if (previousImage != null) {
                fileStorageService.deleteFile(previousImage);
            }
        } else if (request.getImagePath() != null && !request.getImagePath().isBlank()) {
            if (previousImage != null && !previousImage.equals(request.getImagePath())) {
                fileStorageService.deleteFile(previousImage);
            }
            vehicle.setImagePath(request.getImagePath());
        }

        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(User currentUser, Long id) {
        setAvailability(currentUser, id, false);
    }

    public Vehicle setAvailability(User currentUser, Long id, boolean available) {
        Vehicle vehicle = getVehicleById(id);
        if (!currentUser.getRole().equals(UserRole.ADMIN) && !vehicle.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to manage this vehicle");
        }

        vehicle.setAvailable(available);
        return vehicleRepository.save(vehicle);
    }

    private void applyRequest(Vehicle vehicle, VehicleRequest request) {
        vehicle.setName(request.getName());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setCategory(request.getCategory());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmission(request.getTransmission());
        vehicle.setSeatingCapacity(request.getSeatingCapacity());
        vehicle.setLocation(request.getLocation());
        vehicle.setPricePerDay(request.getPricePerDay());
        vehicle.setDescription(request.getDescription());
        vehicle.setAvailable(request.getAvailable() == null || request.getAvailable());
    }

    private boolean isAllowedImageType(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
