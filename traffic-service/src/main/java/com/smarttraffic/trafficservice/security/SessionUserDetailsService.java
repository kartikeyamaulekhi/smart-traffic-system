package com.smarttraffic.trafficservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stateless UserDetailsService for traffic-service.
 *
 * Unlike auth-service, traffic-service has no users table and never sees
 * a raw password - it only needs to reconstruct a UserDetails from the
 * email inside a validated JWT so the security filter chain can authenticate
 * the request. Any non-blank email from a valid (cryptographically signed)
 * token is treated as an authenticated user; authorization decisions on top
 * of that can be layered with roles/precedence later.
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