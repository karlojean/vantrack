package com.vantrack.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TripLocationRepository extends JpaRepository<TripLocation, UUID> {
}
