package com.vantrack.routes;

import com.vantrack.routes.web.dto.RouteResponse;
import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListParentRoutesUseCase {

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    public ListParentRoutesUseCase(UserRepository userRepository, RouteRepository routeRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    public List<RouteResponse> execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        List<Route> routes = routeRepository.findAllByUserRoutes_Parent_Id(userId);

        return routes.stream().map(RouteResponse::fromEntity).toList();
    }

}
