package com.vantrack.routes;

import com.vantrack.routes.web.dto.ParentResponse;
import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListRouteParentsUseCase {

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    public ListRouteParentsUseCase(UserRepository userRepository, RouteRepository routeRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    public List<ParentResponse> execute(UUID id, UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rota"));

        UUID driverId = route.getVan().getDriver().getId();
        if(!driverId.equals(userId) && !user.getRole().equals(UserRole.ADMIN)) {
            throw new BusinessRuleException(
                    "Não foi buscar pais",
                    "Driver so pode buscar pais em rotas que pertence a ele.",
                    HttpStatus.FORBIDDEN);
        }

        return route.getUserRoutes().stream().map(ParentResponse::fromEntity).toList();


    };

}
