package com.vantrack.tracking;

import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.tracking.web.dto.SendLocationRequest;
import com.vantrack.trips.Trip;
import com.vantrack.trips.TripRepository;
import com.vantrack.trips.TripStatus;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SendLocationUseCase {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripLocationRepository tripLocationRepository;

    public SendLocationUseCase(UserRepository userRepository, TripRepository tripRepository, TripLocationRepository tripLocationRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripLocationRepository = tripLocationRepository;
    }

    @Transactional
    public void execute(SendLocationRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new EntityNotFoundException("Viagem"));

        UUID idDriver = trip.getRoute().getVan().getDriver().getId();

        if(!user.getId().equals(idDriver)) {
            throw new BusinessRuleException(
                    "Não foi possível salvar localização",
                    "Driver so pode salvar localização em viagem que pertence a ele.",
                    HttpStatus.FORBIDDEN);
        }

        if(trip.getStatus().equals(TripStatus.COMPLETED)) {
            throw new BusinessRuleException(
                    "Não foi possível salvar localização",
                    "Não e possível salvar localização em viagens finalizadas.",
                    HttpStatus.CONFLICT);
        }

        TripLocation location = SendLocationRequest.toEntity(request);
        location.setTrip(trip);

        tripLocationRepository.save(location);
    }

}
