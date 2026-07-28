package com.vantrack.users;

import com.vantrack.shared.exception.EntityNotFoundException;
import com.vantrack.users.web.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FindUserByIdUseCase {

    private final UserRepository userRepository;

    public FindUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse execute(UUID id) {
        return UserResponse.fromEntity(
                userRepository.findById(id).orElseThrow(
                        () -> new EntityNotFoundException("Usuário")
                )
        );
    }

}
