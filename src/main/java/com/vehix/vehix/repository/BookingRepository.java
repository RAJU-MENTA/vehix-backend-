package com.vehix.vehix.repository;

import com.vehix.vehix.entity.Booking;
import com.vehix.vehix.entity.BookingStatus;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.Vehicle;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomer(User customer);

    List<Booking> findByVehicle(Vehicle vehicle);

    @Query("SELECT b FROM Booking b WHERE b.vehicle = :vehicle AND b.status <> :cancelled AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Booking> findBookingCandidates(@Param("vehicle") Vehicle vehicle,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("cancelled") BookingStatus cancelled);

    List<Booking> findByStatus(BookingStatus status);
}
