package com.vantrack.users;

import com.vantrack.users.web.dto.UserFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListAllUsersUseCase {

    private final UserRepository userRepository;

    public ListAllUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> execute(UserFilter filter) {

        Specification<User> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if(filter.name() != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
        }

        if(filter.role() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), filter.role()));
        }

        return userRepository.findAll(spec);
    }

}
