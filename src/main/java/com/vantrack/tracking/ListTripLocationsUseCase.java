package com.vantrack.tracking;

import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.tracking.authorization.TripAccessPolicy;
import com.vantrack.tracking.web.dto.TripLocationResponse;
import com.vantrack.trips.Trip;
import com.vantrack.trips.TripRepository;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListTripLocationsUseCase {

    private final TripLocationRepository tripLocationRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripAccessPolicy tripAccessPolicy;

    public ListTripLocationsUseCase(TripLocationRepository tripLocationRepository, TripRepository tripRepository, UserRepository userRepository, TripAccessPolicy tripAccessPolicy) {
        this.tripLocationRepository = tripLocationRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripAccessPolicy = tripAccessPolicy;
    }

    public List<TripLocationResponse> execute(UUID tripId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Viagem"));


        if (!tripAccessPolicy.canViewHistory(userId, tripId)) {
            throw new BusinessRuleException(
                    "Não foi possível buscar as localizações",
                    "Usuário so pode buscar as localizações em viagem que ele tem vínculo.",
                    HttpStatus.FORBIDDEN);
        }

        List<TripLocation> locations = tripLocationRepository.findByTripOrderByCreatedAt(trip);

        return locations.stream().map(TripLocationResponse::fromEntity).toList();
    }

}
