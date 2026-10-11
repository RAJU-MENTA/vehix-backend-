package com.vehix.vehix.service;

import com.vehix.vehix.dto.AuthResponse;
import com.vehix.vehix.dto.LoginRequest;
import com.vehix.vehix.dto.RegisterRequest;
import com.vehix.vehix.dto.UserSummary;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.exception.ApiException;
import com.vehix.vehix.repository.UserRepository;
import com.vehix.vehix.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        String requestedRole = request.getRole() == null || request.getRole().isBlank()
                ? "CUSTOMER"
                : request.getRole().trim().toUpperCase();
        UserRole role = switch (requestedRole) {
            case "CUSTOMER" -> UserRole.CUSTOMER;
            case "SELLER" -> UserRole.SELLER;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported account type");
        };

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(role);

        user = userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail().trim().toLowerCase(),
                request.getPassword()
        ));

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name()));
    }
}
