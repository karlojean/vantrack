package com.vantrack.trips;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TripRepository extends JpaRepository<Trip, UUID> {
    boolean existsByRoute_IdAndStatus(UUID routeId, TripStatus status);
}
