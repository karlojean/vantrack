package com.vantrack.routes;

import com.vantrack.routes.web.dto.RouteResponse;
import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListAllRoutesUseCase {

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;

    public ListAllRoutesUseCase(RouteRepository routeRepository, UserRepository userRepository) {
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
    }

    public List<RouteResponse> execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        if(user.getRole().equals(UserRole.ADMIN)) {
            return routeRepository.findAll().stream().map(RouteResponse::fromEntity).toList();
        }

        return routeRepository.findAllByVan_Driver_Id(userId).stream().map(RouteResponse::fromEntity).toList();
    }
}
