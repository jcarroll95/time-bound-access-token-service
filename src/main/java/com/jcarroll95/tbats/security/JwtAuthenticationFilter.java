package com.jcarroll95.tbats.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Read Authorization header
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // invalid or missing header
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // Strip bearer prefix
        final String jwt = authHeader.substring(7);

        // validate token, if invalid continue chain
        if (!jwtUtil.validateToken(jwt)) {
            // invalid token
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        final String username = jwtUtil.extractUsername(jwt);
        final String role = jwtUtil.extractRole(jwt);

        // if valid extractUsername + extractRole
        // create a user/pass auth token with null password and role
        // three-argument UsernamePasswordAuthenticationToken constructs authenticated token
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        // attach ip address and session id metadata to the authentication object
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);

        // continue filter chain
        filterChain.doFilter(request, response);
    }

}
