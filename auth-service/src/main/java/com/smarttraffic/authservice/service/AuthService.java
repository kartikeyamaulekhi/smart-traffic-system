package com.smarttraffic.authservice.service;

import com.smarttraffic.authservice.dto.AuthResponse;
import com.smarttraffic.authservice.dto.LoginRequest;
import com.smarttraffic.authservice.dto.RegisterRequest;
import com.smarttraffic.authservice.model.Role;
import com.smarttraffic.authservice.model.User;
import com.smarttraffic.authservice.repository.UserRepository;
import com.smarttraffic.authservice.security.JwtService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MeterRegistry meterRegistry;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            meterRegistry.counter("smart_traffic_registrations_total", "outcome", "duplicate").increment();
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        meterRegistry.counter("smart_traffic_registrations_total", "outcome", "created").increment();
        return buildAuthResponse(user.getEmail(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            meterRegistry.counter("smart_traffic_logins_total", "outcome", "unauthorized").increment();
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        meterRegistry.counter("smart_traffic_logins_total", "outcome", "success").increment();
        return buildAuthResponse(user.getEmail(), user.getRole());
    }

    private AuthResponse buildAuthResponse(String email, Role role) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password("") // not needed to generate the token
                .authorities("ROLE_" + role.name())
                .build();

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(email)
                .role(role)
                .expiresInMs(jwtService.getExpirationMs())
                .build();
    }

}
