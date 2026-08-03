package com.vantrack.trips;

import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.trips.web.dto.TripResponse;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class FinishTripUseCase {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    public FinishTripUseCase(UserRepository userRepository, TripRepository tripRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public TripResponse execute(UUID tripId, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Viagem"));

        UUID idDriver = trip.getRoute().getVan().getDriver().getId();

        if(!user.getId().equals(idDriver)) {
            throw new BusinessRuleException(
                    "Não foi possível finalizar viagem",
                    "Driver so pode finalizar viagem que pertence a ele.",
                    HttpStatus.FORBIDDEN);
        }

        if(trip.getStatus().equals(TripStatus.COMPLETED)) {
            throw new BusinessRuleException(
                    "Não foi possível finalizar viagem",
                    "Não e possível finalizar viagem completas.",
                    HttpStatus.CONFLICT);
        }

        trip.setStatus(TripStatus.COMPLETED);
        OffsetDateTime endedAt = OffsetDateTime.now();
        trip.setEndedAt(endedAt);

        return TripResponse.fromEntity(tripRepository.save(trip));
    }

}
