package com.smarttraffic.authservice.service;

import com.smarttraffic.authservice.dto.AuthResponse;
import com.smarttraffic.authservice.dto.LoginRequest;
import com.smarttraffic.authservice.dto.RegisterRequest;
import com.smarttraffic.authservice.model.Role;
import com.smarttraffic.authservice.model.User;
import com.smarttraffic.authservice.repository.UserRepository;
import com.smarttraffic.authservice.security.JwtService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Spy
    private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");
        return request;
    }

    private User user() {
        return User.builder()
                .id(1L)
                .email("alice@example.com")
                .passwordHash("$2a$hashed")
                .role(Role.USER)
                .build();
    }

    @Test
    void register_newEmail_returnsAuthResponse() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("token123");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(registerRequest());

        assertEquals("alice@example.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        assertEquals("token123", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600000L, response.getExpiresInMs());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> authService.register(registerRequest()));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user()));
        when(jwtService.generateToken(any())).thenReturn("token123");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        LoginRequest login = new LoginRequest();
        login.setEmail("alice@example.com");
        login.setPassword("secret123");

        AuthResponse response = authService.login(login);

        assertEquals("alice@example.com", response.getEmail());
        assertEquals("token123", response.getToken());
    }

    @Test
    void login_invalidCredentials_throwsBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad creds"));

        LoginRequest login = new LoginRequest();
        login.setEmail("alice@example.com");
        login.setPassword("wrong");

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class, () -> authService.login(login));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }
}