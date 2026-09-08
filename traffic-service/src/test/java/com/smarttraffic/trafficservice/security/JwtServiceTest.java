package com.smarttraffic.trafficservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET = "sTFnBXGs9rdn32UKsGZixtYBm51daZj/rAdjoqkutTE=";

    private JwtService jwtService() {
        return new JwtService(TEST_SECRET, 3600000);
    }

    private UserDetails user(String email) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password("x")
                .authorities("ROLE_USER")
                .build();
    }

    private String signToken(String email, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    @Test
    void extractEmail_readsSubject() {
        JwtService service = jwtService();

        assertEquals("alice@example.com", service.extractEmail(signToken("alice@example.com", 3600000)));
    }

    @Test
    void isTokenValid_acceptsMatch() {
        JwtService service = jwtService();
        UserDetails alice = user("alice@example.com");

        assertTrue(service.isTokenValid(signToken("alice@example.com", 3600000), alice));
    }

    @Test
    void isTokenValid_rejectsOtherUserToken() {
        JwtService service = jwtService();
        String aliceToken = signToken("alice@example.com", 3600000);

        assertFalse(service.isTokenValid(aliceToken, user("bob@example.com")));
    }

    @Test
    void expiredToken_cannotBeParsed() {
        JwtService service = jwtService();
        String token = signToken("alice@example.com", -1000);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> service.extractEmail(token));
    }
}