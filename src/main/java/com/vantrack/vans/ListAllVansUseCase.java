package com.vantrack.vans;

import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.shared.persistence.LikeEscaper;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import com.vantrack.vans.web.dto.VanFilter;
import com.vantrack.vans.web.dto.VanResponse;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListAllVansUseCase {

    private final VanRepository vanRepository;
    private final UserRepository userRepository;

    public ListAllVansUseCase(VanRepository vanRepository, UserRepository userRepository) {
        this.vanRepository = vanRepository;
        this.userRepository = userRepository;
    }

    public List<VanResponse> execute(VanFilter filter, UUID authenticatedUserId) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Erro ao buscar usuário",
                        "Erro ao encontrar usuário autenticado",
                        HttpStatus.UNAUTHORIZED
                ));

        Specification<Van> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if(filter.plate() != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("plate")), LikeEscaper.containing(filter.plate().toLowerCase()), LikeEscaper.ESCAPE_CHAR));
        }

        UUID filterUserId = user.getRole() == UserRole.DRIVER ? user.getId() : filter.userId();

        if(filterUserId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("driver").get("id"), filterUserId)
            );
        }

        List<Van> vans = vanRepository.findAll(spec);

        return vans.stream().map(VanResponse::fromEntity).toList();
    }

}
