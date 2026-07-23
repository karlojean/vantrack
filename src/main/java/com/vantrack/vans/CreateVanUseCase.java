package com.vantrack.vans;

import com.vantrack.shared.exception.BusinessRoleException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import com.vantrack.vans.web.dto.CreateVanRequest;
import com.vantrack.vans.web.dto.VanResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateVanUseCase {

    private final UserRepository userRepository;
    private final VanRepository vanRepository;

    public CreateVanUseCase(UserRepository userRepository, VanRepository vanRepository) {
        this.userRepository = userRepository;
        this.vanRepository = vanRepository;
    }

    @Transactional
    public VanResponse execute(CreateVanRequest request, UUID authenticatedUserId) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new BusinessRoleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        boolean isCreatingForAnotherUser = !user.getId().equals(request.driverId());
        if (user.getRole() != UserRole.ADMIN && isCreatingForAnotherUser) {
            throw new BusinessRoleException(
                    "Sem permissão",
                    "O usuário não tem permissão para criar van para outro usuári",
                    HttpStatus.FORBIDDEN
            );
        }

        if (user.getId() != request.driverId()) {
            user = userRepository.findById(request.driverId())
                    .orElseThrow(() -> new BusinessRoleException(
                            "Erro ao buscar usuário",
                            String.format("Não foi possível encontrar o usuário com Id: %s", request.driverId()),
                            HttpStatus.NOT_FOUND
                    ));
        }

        Van newVan = request.toEntity();
        newVan.setDriver(user);

        vanRepository.save(newVan);

        return VanResponse.fromEntity(newVan);
    }

}
