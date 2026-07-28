package com.vantrack.routes;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {
    @EntityGraph(attributePaths = {"van", "van.driver"})
    List<Route> findAllByVan_Driver_Id(UUID userId);

    @EntityGraph(attributePaths = {"van", "van.driver"})
    List<Route> findAll();
}
