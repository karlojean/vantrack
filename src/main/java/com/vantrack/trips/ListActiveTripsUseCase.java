package com.vantrack.trips;

import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.trips.web.dto.TripResponse;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListActiveTripsUseCase {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    public ListActiveTripsUseCase(UserRepository userRepository, TripRepository tripRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
    }

    public List<TripResponse> execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        if(user.getRole().equals(UserRole.ADMIN)) {
            return tripRepository.findAllByStatus(TripStatus.ACTIVE)
                    .stream()
                    .map(TripResponse::fromEntity)
                    .toList();
        }

        return tripRepository
                .findAllByParentIdAndStatus(userId, TripStatus.ACTIVE)
                .stream()
                .map(TripResponse::fromEntity)
                .toList();
    }

}
