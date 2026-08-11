package com.vantrack.trips;

import com.vantrack.routes.Route;
import com.vantrack.routes.UserRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    boolean existsByRoute_IdAndStatus(UUID routeId, TripStatus status);

    @Query("""
          SELECT DISTINCT trip
          FROM Trip trip
          JOIN trip.route route
          JOIN route.userRoutes userRoute
          WHERE userRoute.parent.id = :parentId
            AND trip.status = :status
          """)
    List<Trip> findAllByParentIdAndStatus(
            @Param("parentId") UUID parentId,
            @Param("status") TripStatus status
    );

    List<Trip> findAllByStatus(TripStatus status);
}
