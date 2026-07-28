package com.vantrack.users;

import com.vantrack.shared.exception.EmailAlreadyExistsException;
import com.vantrack.users.web.dto.CreateUserRequest;
import com.vantrack.users.web.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserUseCase {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.repository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse execute(CreateUserRequest request) {
        if(repository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User newUser = request.toEntity();
        newUser.setPassword(passwordEncoder.encode(request.password()));

        return UserResponse.fromEntity(repository.save(newUser));
    }

}
