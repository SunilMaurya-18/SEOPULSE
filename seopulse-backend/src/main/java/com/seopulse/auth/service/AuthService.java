package com.seopulse.auth.service;

import com.seopulse.auth.dto.AuthResponse;
import com.seopulse.auth.dto.LoginRequest;
import com.seopulse.auth.dto.RegisterRequest;
import com.seopulse.common.exception.DuplicateResourceException;
import com.seopulse.common.exception.InvalidCredentialsException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.user.entity.Role;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
                .name(request.name())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        log.info(
                "User registered: userId={}, email={}",
                savedUser.getId(),
                savedUser.getEmail()
        );

        return new AuthResponse(
                token,
                "Bearer",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    request.password()
                            )
                    );

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found"
                            )
                    );

            String token = jwtService.generateToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            log.info(
                    "User logged in: userId={}, email={}",
                    user.getId(),
                    user.getEmail()
            );

            return new AuthResponse(
                    token,
                    "Bearer",
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().name()
            );

        } catch (AuthenticationException ex) {

            log.warn(
                    "Failed login attempt: email={}",
                    email
            );

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }
    }
}

