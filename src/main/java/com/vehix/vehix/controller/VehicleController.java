package com.vehix.vehix.controller;

import com.vehix.vehix.dto.VehicleRequest;
import com.vehix.vehix.dto.VehicleResponse;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.Vehicle;
import com.vehix.vehix.service.UserService;
import com.vehix.vehix.service.VehicleService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserService userService;

    public VehicleController(VehicleService vehicleService, UserService userService) {
        this.vehicleService = vehicleService;
        this.userService = userService;
    }

    @GetMapping("/vehicles")
    public List<VehicleResponse> getVehicles(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String location,
                                             @RequestParam(required = false) BigDecimal minPrice,
                                             @RequestParam(required = false) BigDecimal maxPrice,
                                             @RequestParam(required = false) String sort) {
        return vehicleService.searchAvailableVehicles(q, category, location, minPrice, maxPrice, sort)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/seller/vehicles")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public List<VehicleResponse> getSellerVehicles(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return vehicleService.getVehiclesForOwner(currentUser).stream().map(this::toResponse).toList();
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(vehicleService.getVehicleById(id)));
    }

    @PostMapping(value = "/vehicles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<VehicleResponse> createVehicle(@RequestPart("vehicle") @Valid VehicleRequest request,
                                                          @RequestPart(value = "image", required = false) MultipartFile image,
                                                          Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.status(201).body(toResponse(vehicleService.createVehicle(currentUser, request, image)));
    }

    @PutMapping(value = "/vehicles/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable Long id,
                                                          @RequestPart("vehicle") @Valid VehicleRequest request,
                                                          @RequestPart(value = "image", required = false) MultipartFile image,
                                                          Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(toResponse(vehicleService.updateVehicle(currentUser, id, request, image)));
    }

    @DeleteMapping("/vehicles/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        vehicleService.deleteVehicle(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/vehicles/{id}/availability")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<VehicleResponse> setVehicleAvailability(@PathVariable Long id,
                                                                   @RequestParam boolean available,
                                                                   Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(toResponse(vehicleService.setAvailability(currentUser, id, available)));
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getName(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getCategory(),
                vehicle.getFuelType(),
                vehicle.getTransmission(),
                vehicle.getSeatingCapacity(),
                vehicle.getLocation(),
                vehicle.getPricePerDay(),
                vehicle.getDescription(),
                vehicle.getImagePath(),
                vehicle.isAvailable(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }
}
