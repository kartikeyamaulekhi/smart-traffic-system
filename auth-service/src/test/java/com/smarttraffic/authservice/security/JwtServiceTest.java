package com.smarttraffic.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET = "sTFnBXGs9rdn32UKsGZixtYBm51daZj/rAdjoqkutTE=";

    private JwtService jwtService(long expirationMs) {
        return new JwtService(TEST_SECRET, expirationMs);
    }

    private UserDetails user(String email) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password("x")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void generateToken_containsSubjectAsEmail() {
        JwtService service = jwtService(3600000);

        String token = service.generateToken(user("alice@example.com"));

        assertEquals("alice@example.com", service.extractEmail(token));
    }

    @Test
    void getExpirationMs_returnsConfiguredValue() {
        assertEquals(3600000, jwtService(3600000).getExpirationMs());
        assertEquals(900000, jwtService(900000).getExpirationMs());
    }

    @Test
    void isTokenValid_acceptsMatch() {
        JwtService service = jwtService(3600000);
        UserDetails alice = user("alice@example.com");

        assertTrue(service.isTokenValid(service.generateToken(alice), alice));
    }

    @Test
    void isTokenValid_rejectsOtherUserToken() {
        JwtService service = jwtService(3600000);
        String aliceToken = service.generateToken(user("alice@example.com"));

        assertFalse(service.isTokenValid(aliceToken, user("bob@example.com")));
    }

    @Test
    void expiredToken_cannotBeParsed() {
        JwtService service = jwtService(-1000);
        String token = service.generateToken(user("alice@example.com"));

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> service.extractEmail(token));
    }
}