package com.smarttraffic.routingservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stateless UserDetailsService for routing-service.
 *
 * Like traffic-service, routing-service has no users table and never sees a
 * password - it only needs to reconstruct a UserDetails from the email inside
 * a validated JWT so the security filter chain can authenticate the request.
 */
@Service
public class SessionUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) {
        List<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        // Password is irrelevant here - we never authenticate against it. The
        // JwtAuthenticationFilter has already cryptographically verified the
        // token's signature before calling this.
        return User.withUsername(username)
                .password("")
                .authorities(authorities)
                .build();
    }

}