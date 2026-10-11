package com.vehix.vehix.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.entity.Vehicle;
import com.vehix.vehix.exception.ApiException;
import com.vehix.vehix.repository.VehicleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void removingListingHidesItWithoutDeletingVehicleOrBookingHistory() {
        User owner = new User("Seller", "seller@example.com", "password", null, UserRole.SELLER);
        owner.setId(4L);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(12L);
        vehicle.setOwner(owner);
        vehicle.setAvailable(true);
        when(vehicleRepository.findById(12L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        vehicleService.deleteVehicle(owner, 12L);

        assertThat(vehicle.isAvailable()).isFalse();
        verify(vehicleRepository).save(vehicle);
        verify(vehicleRepository, never()).delete(vehicle);
    }

    @Test
    void sellerCannotRemoveAnotherSellersVehicle() {
        User owner = new User("Seller", "seller@example.com", "password", null, UserRole.SELLER);
        owner.setId(4L);
        User otherSeller = new User("Other Seller", "other@example.com", "password", null, UserRole.SELLER);
        otherSeller.setId(8L);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(12L);
        vehicle.setOwner(owner);
        when(vehicleRepository.findById(12L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.deleteVehicle(otherSeller, 12L))
                .isInstanceOf(ApiException.class)
                .hasMessage("You do not have permission to manage this vehicle")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(vehicleRepository, never()).save(vehicle);
    }
}
