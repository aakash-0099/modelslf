package com.example.usermanagement.security;

import com.example.usermanagement.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.usermanagement.entity.User;
import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final boolean rateLimitEnabled;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            ObjectMapper objectMapper,
            @Value("${rate-limit.enabled}") boolean rateLimitEnabled
    ) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.rateLimitEnabled = rateLimitEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        if (!rateLimitEnabled) {                                                  // If rate limiting is disabled, proceed with the request
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();

        if ("/api/auth/login".equals(requestUri)
                || "/api/auth/register".equals(requestUri)) {

            String key = "ip:" + request.getRemoteAddr();

            if (!rateLimitService.isAllowed(key)) {
                writeRateLimitResponse(response);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }


        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // System.out.println("AUTHENTICATION = " + authentication);
        // System.out.println("AUTHENTICATED = " +
        //         (authentication != null && authentication.isAuthenticated()));

        // if (authentication != null) {
        //     System.out.println("AUTH NAME = " + authentication.getName());
        //     System.out.println("PRINCIPAL = " + authentication.getPrincipal());
        // }

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User) {

            User user = (User) authentication.getPrincipal();
            String key = "user:" + user.getId();

            // System.out.println("RATE LIMIT KEY = " + key);

            if (!rateLimitService.isAllowed(key)) {
                writeRateLimitResponse(response);
                return;
            }
        }


        filterChain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response)
            throws IOException {

        ProblemDetail problemDetail = ProblemDetail.forStatus(429);

        problemDetail.setTitle("Too Many Requests");
        problemDetail.setDetail(
                "Rate limit exceeded. Please try again later."
        );

        response.setStatus(429);
        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getWriter(),
                problemDetail
        );
    }
}