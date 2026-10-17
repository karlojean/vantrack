package com.vantrack.tracking.authorization;

import com.vantrack.routes.UserRouteRepository;
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

    public boolean canViewHistory(UUID userId, UUID tripId) {
        User user = userRepository.findById(userId)
                .orElse(null);

        Trip trip = tripRepository.findById(tripId).orElse(null);

        List<UserRole> rolesCanAccess = List.of(UserRole.PARENT, UserRole.ADMIN, UserRole.DRIVER);

        if(!isEligible(rolesCanAccess, user, trip)) return false;

        return isLinkedToTrip(user, trip);
    }

    public boolean canSubscribe(UUID userId, UUID tripId){
        User user = userRepository.findById(userId)
                .orElse(null);

        Trip trip = tripRepository.findById(tripId).orElse(null);

        List<UserRole> rolesCanAccess = List.of(UserRole.PARENT, UserRole.ADMIN);

        if(!isEligible(rolesCanAccess, user, trip)) return false;

        if(trip.getStatus() == TripStatus.COMPLETED) return false;

        return isLinkedToTrip(user, trip);
    }

    private boolean isEligible(List<UserRole> rolesCanAccess, User user, Trip trip) {
        if(user == null) {
            return false;
        }

        if(!rolesCanAccess.contains(user.getRole())) {
            return false;
        }

        if(trip == null) {
            return false;
        }

        return true;
    }

    private boolean isLinkedToTrip(User user, Trip trip) {
        boolean userIsParentInRoute = userRouteRepository.existsByRouteAndParent(trip.getRoute(), user);
        boolean userIsDriver = tripRepository.existsByIdAndRouteVanDriverId(trip.getId(), user.getId());

        return userIsDriver || userIsParentInRoute || user.getRole().equals(UserRole.ADMIN);
    }

}
