package com.vantrack.routes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RouteRepository extends JpaRepository<Route, UUID> {
}
