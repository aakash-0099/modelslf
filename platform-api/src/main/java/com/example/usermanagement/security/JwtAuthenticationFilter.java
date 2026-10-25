package com.example.usermanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;

import com.example.usermanagement.entity.User;
import com.example.usermanagement.repository.UserRepository;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {               //header does not contain a valid JWT token or empty, just continue the filter chain

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {

            filterChain.doFilter(request, response);                //token is invalid, so we cannot authenticate the user, just continue the filter chain
            return;
        }

        String email =
                jwtService.extractEmail(token);

        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        if (user != null &&
                user.isEnabled() &&
                SecurityContextHolder
                        .getContext()
                        .getAuthentication() == null ) {

                String authority =
                    "ROLE_" + user.getRole().name();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            java.util.List.of(
                                    new SimpleGrantedAuthority(authority)
                            )
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }
        Authentication authentication =
        SecurityContextHolder
                .getContext()
                .getAuthentication();

        // System.out.println("Authentication: " + authentication);
        // if (authentication != null) {
        //         System.out.println("Principal: " + authentication.getPrincipal());
        //         System.out.println("Authorities: " + authentication.getAuthorities());
        //         System.out.println("Authenticated: " + authentication.isAuthenticated());
        // }
        filterChain.doFilter(request, response);
    }
}