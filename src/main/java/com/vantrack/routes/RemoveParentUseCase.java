package com.vantrack.routes;

import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RemoveParentUseCase {

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final UserRouteRepository userRouteRepository;

    public RemoveParentUseCase(UserRepository userRepository, RouteRepository routeRepository, UserRouteRepository userRouteRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.userRouteRepository = userRouteRepository;
    }


    @Transactional
    public void execute(UUID id, UUID parentId, UUID userId) {
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
                    "Não foi possível remover pai",
                    "Driver so pode remover pai em rotas que pertence a ele.",
                    HttpStatus.FORBIDDEN);
        }

        if(!userRepository.existsById(parentId)) {
            throw new EntityNotFoundException("Pai");
        }

        userRouteRepository.removeByRoute_IdAndParent_Id(route.getId(), parentId);
    }
}
