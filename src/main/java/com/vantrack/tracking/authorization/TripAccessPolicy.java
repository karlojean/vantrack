package com.vantrack.tracking.authorization;

import com.vantrack.tracking.UserRouteRepository;
import com.vantrack.trips.Trip;
import com.vantrack.trips.TripRepository;
import com.vantrack.trips.TripStatus;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TripAccessPolicy {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final UserRouteRepository userRouteRepository;

    public TripAccessPolicy(UserRepository userRepository, TripRepository tripRepository, UserRouteRepository userRouteRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.userRouteRepository = userRouteRepository;
    }

    public boolean canAccess(UUID userId, UUID tripId){
        User user = userRepository.findById(userId)
                .orElse(null);

        if(user == null) {
            return false;
        }

        List<UserRole> rolesCanAccess = List.of(UserRole.PARENT, UserRole.ADMIN);

        if(!rolesCanAccess.contains(user.getRole())) {
            return false;
        }

        Trip trip = tripRepository.findById(tripId).orElse(null);

        if(trip == null) {
            return false;
        }

        if(trip.getStatus().equals(TripStatus.COMPLETED)) {
            return false;
        }

        boolean userIsParentInRoute = userRouteRepository.existsByRouteAndParent(trip.getRoute(), user);

        return userIsParentInRoute || user.getRole().equals(UserRole.ADMIN);
    }

}
