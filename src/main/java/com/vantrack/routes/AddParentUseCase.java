package com.vantrack.routes;

import com.vantrack.routes.web.dto.AddParentRequest;
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
public class AddParentUseCase {

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;

    public AddParentUseCase(RouteRepository routeRepository, UserRepository userRepository) {
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(UUID id, AddParentRequest request, UUID userId) {
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
                    "Não foi possível adicionar pai",
                    "Driver so pode adicionar pai em rotas que pertence a ele.",
                    HttpStatus.FORBIDDEN);
        }

        User parent = userRepository.findById(request.parentId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário"));

        if(!parent.getRole().equals(UserRole.PARENT)) {
            throw new BusinessRuleException(
                    "Não foi possível salvar pai",
                    "Não e possível salvar usuários como pai que não pertence a role PARENT.",
                    HttpStatus.FORBIDDEN);
        }

        UserRoute userRoute = new UserRoute();
        userRoute.setStudentName(request.studentName());
        userRoute.setParent(parent);

        route.addParent(userRoute);

        routeRepository.save(route);
    }

}
