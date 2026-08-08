package com.vantrack.tracking.authorization;

import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.tracking.UserRouteRepository;
import com.vantrack.trips.Trip;
import com.vantrack.trips.TripRepository;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.http.HttpStatus;
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
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        List<UserRole> rolesCanAccess = List.of(UserRole.PARENT, UserRole.ADMIN);

        if(!rolesCanAccess.contains(user.getRole())) {
            return false;
        }

        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new EntityNotFoundException("Viagem")
        );

        boolean userIsParentInRoute = userRouteRepository   .existsByRouteAndParent(trip.getRoute(), user);

        if(!userIsParentInRoute && !user.getRole().equals(UserRole.ADMIN)) {
            return false;
        }

        return true;
    }

}
