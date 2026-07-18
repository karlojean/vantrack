package com.vantrack.users.web;

import com.vantrack.users.CreateUserUseCase;
import com.vantrack.users.web.dto.CreateUserRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    // TODO add pre authorize for ADMIN user
    @PostMapping
    void createUser(@RequestBody @Valid CreateUserRequest request) {
        createUserUseCase.execute(request);
    }
}
