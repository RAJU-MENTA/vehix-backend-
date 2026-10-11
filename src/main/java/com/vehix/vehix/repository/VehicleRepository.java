package com.vehix.vehix.repository;

import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.Vehicle;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwner(User owner);

    List<Vehicle> findByAvailableTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    java.util.Optional<Vehicle> findByIdForBooking(@Param("id") Long id);

    @Query("""
            select v from Vehicle v
            where v.available = true
              and (:search is null
                   or lower(v.name) like lower(concat('%', :search, '%'))
                   or lower(v.brand) like lower(concat('%', :search, '%'))
                   or lower(v.model) like lower(concat('%', :search, '%'))
                   or lower(v.location) like lower(concat('%', :search, '%')))
              and (:category is null or lower(v.category) = lower(:category))
              and (:location is null or lower(v.location) like lower(concat('%', :location, '%')))
              and (:minPrice is null or v.pricePerDay >= :minPrice)
              and (:maxPrice is null or v.pricePerDay <= :maxPrice)
            """)
    List<Vehicle> searchAvailableVehicles(@Param("search") String search,
                                          @Param("category") String category,
                                          @Param("location") String location,
                                          @Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice);
}
