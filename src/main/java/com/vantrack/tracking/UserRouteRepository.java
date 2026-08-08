package com.vantrack.tracking;

import com.vantrack.routes.Route;
import com.vantrack.routes.UserRoute;
import com.vantrack.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRouteRepository extends JpaRepository<UserRoute, UUID> {
    boolean existsByRouteAndParent(Route route, User user);
}
