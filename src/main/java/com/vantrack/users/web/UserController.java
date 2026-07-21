package com.vantrack.users.web;

import com.vantrack.users.CreateUserUseCase;
import com.vantrack.users.FindUserByIdUseCase;
import com.vantrack.users.ListAllUsersUseCase;
import com.vantrack.users.User;
import com.vantrack.users.web.dto.CreateUserRequest;
import com.vantrack.users.web.dto.UserFilter;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final ListAllUsersUseCase listAllUsersUseCase;
    private final FindUserByIdUseCase findUserByIdUseCase;

    public UserController(CreateUserUseCase createUserUseCase, ListAllUsersUseCase listAllUsersUseCase, FindUserByIdUseCase findUserByIdUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.listAllUsersUseCase = listAllUsersUseCase;
        this.findUserByIdUseCase = findUserByIdUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    void createUser(@RequestBody @Valid CreateUserRequest request) {
        createUserUseCase.execute(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    List<User> listAllUsers(@ModelAttribute UserFilter filter){
        return listAllUsersUseCase.execute(filter);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    User findUserById(@PathVariable UUID id) {
        return findUserByIdUseCase.execute(id);
    }

}
