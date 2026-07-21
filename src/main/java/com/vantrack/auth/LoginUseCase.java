package com.vantrack.auth;

import com.vantrack.auth.web.dto.LoginRequest;
import com.vantrack.shared.exception.BusinessRoleException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.util.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    public String execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(
                BusinessRoleException::new
        );

        if(!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessRoleException();
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        return tokenService.generateToken(authentication);
    }
}
