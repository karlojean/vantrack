package com.vantrack.tracking;

import com.vantrack.trips.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripLocationRepository extends JpaRepository<TripLocation, UUID> {
    List<TripLocation> findByTripOrderByCreatedAt(Trip trip);
}
