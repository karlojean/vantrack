package com.vantrack.auth;

import com.vantrack.auth.web.dto.LoginRequest;
import com.vantrack.auth.web.dto.LoginResponse;
import com.vantrack.shared.exception.BusinessRuleException;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.util.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public LoginUseCase( TokenService tokenService, AuthenticationManager authenticationManager) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    public LoginResponse execute(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = tokenService.generateToken(authentication);;

        return new LoginResponse(token);
    }
}
