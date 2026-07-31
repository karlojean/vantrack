package com.vantrack.trips;

import com.vantrack.routes.Route;
import com.vantrack.routes.RouteRepository;
import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.tracking.TripLocation;
import com.vantrack.tracking.TripLocationRepository;
import com.vantrack.trips.web.dto.StartTripRequest;
import com.vantrack.trips.web.dto.TripResponse;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class StartTripUseCase {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final TripLocationRepository tripLocationRepository;
    private final UserRepository userRepository;

    public StartTripUseCase(TripRepository tripRepository, RouteRepository routeRepository, TripLocationRepository tripLocationRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.tripLocationRepository = tripLocationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TripResponse execute(StartTripRequest request, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));


        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new EntityNotFoundException("Rota"));

        if(!user.getId().equals(route.getVan().getDriver().getId())) {
            throw new BusinessRuleException(
                    "Não foi possível iniciar viagem",
                    "Driver so pode iniciar viagem que pertence a ele.",
                    HttpStatus.FORBIDDEN);
        }

        if(tripRepository.existsByRoute_IdAndStatus(route.getId(), TripStatus.ACTIVE)) {
            throw new BusinessRuleException(
                    "Não foi possível iniciar viagem",
                    "Não é possível iniciar uma rota com outra em andamento",
                    HttpStatus.CONFLICT
            );
        }



        OffsetDateTime startedAt = OffsetDateTime.now();

        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStartedAt(startedAt);
        trip.setStatus(TripStatus.ACTIVE);

        Trip savedTrip = tripRepository.save(trip);

        TripLocation firstLocation = new TripLocation();
        firstLocation.setTrip(savedTrip);
        firstLocation.setLatitude(request.latitude());
        firstLocation.setLongitude(request.longitude());
        firstLocation.setCreatedAt(startedAt);

        tripLocationRepository.save(firstLocation);

        return TripResponse.fromEntity(savedTrip);
}

}
