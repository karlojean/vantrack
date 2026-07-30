package com.vantrack.support;

import com.vantrack.routes.Route;
import com.vantrack.routes.RouteRepository;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import com.vantrack.util.TokenService;
import com.vantrack.vans.Van;
import com.vantrack.vans.VanRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.atomic.AtomicInteger;


public class TestDataFactory {

    public static final String DEFAULT_PASSWORD = "senha123";

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final UserRepository userRepository;
    private final VanRepository vanRepository;
    private final RouteRepository routeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public TestDataFactory(UserRepository userRepository,
                           VanRepository vanRepository,
                           RouteRepository routeRepository,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService) {
        this.userRepository = userRepository;
        this.vanRepository = vanRepository;
        this.routeRepository = routeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public User admin() {
        return user(UserRole.ADMIN);
    }

    public User driver() {
        return user(UserRole.DRIVER);
    }

    public User parent() {
        return user(UserRole.PARENT);
    }

    public User user(UserRole role) {
        int seq = SEQUENCE.incrementAndGet();

        return userNamed(role.name().toLowerCase() + " " + seq, role);
    }

    public User userNamed(String name, UserRole role) {
        int seq = SEQUENCE.incrementAndGet();

        User user = new User();
        user.setName(name);
        user.setEmail(role.name().toLowerCase() + "-" + seq + "@vantrack.test");
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRole(role);

        return userRepository.save(user);
    }

    public Van van(User driver) {
        return van(driver, nextPlate());
    }

    public Van van(User driver, String plate) {
        Van van = new Van();
        van.setPlate(plate);
        van.setDriver(driver);

        return vanRepository.save(van);
    }

    public Route route(Van van) {
        return route(van, "Rota " + SEQUENCE.incrementAndGet());
    }

    public Route route(Van van, String name) {
        Route route = new Route();
        route.setName(name);
        route.setVan(van);

        return routeRepository.save(route);
    }

    public String nextPlate() {
        return String.format("ABC%04d", SEQUENCE.incrementAndGet() % 10_000);
    }


    public String tokenFor(User user) {
        return tokenService.generateToken(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    public String bearerFor(User user) {
        return "Bearer " + tokenFor(user);
    }
}
