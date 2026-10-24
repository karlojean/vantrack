package com.vantrack.routes;

import com.vantrack.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRouteRepository extends JpaRepository<UserRoute, UUID> {
    boolean existsByRouteAndParent(Route route, User user);
    List<UserRoute> findByParent(User user);

    void removeByRoute_IdAndParent_Id(UUID routeId, UUID parentId);

    long countByRoute(Route route);

}
