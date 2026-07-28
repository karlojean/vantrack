package com.vantrack.routes;

import com.vantrack.routes.web.dto.CreateRouteRequest;
import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import com.vantrack.vans.Van;
import com.vantrack.vans.VanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateRouteUseCase {

    private final VanRepository vanRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    public CreateRouteUseCase(VanRepository vanRepository, UserRepository userRepository, RouteRepository routeRepository) {
        this.vanRepository = vanRepository;
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    @Transactional
    public Route execute(CreateRouteRequest request, UUID userId) {
        Van van = vanRepository.findById(request.vanId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Van")
                );

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Usuário")
                );



        boolean userIsDriver = userId.equals(van.getDriver().getId());

        if(!userIsDriver && user.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleException(
                    "Erro ao criar rota",
                    "O usuário não tem permissão para criar um rota para essa van",
                    HttpStatus.FORBIDDEN
            );
        }

        Route route = request.toDomain();
        route.setVan(van);

        return routeRepository.save(route);
    }

}
