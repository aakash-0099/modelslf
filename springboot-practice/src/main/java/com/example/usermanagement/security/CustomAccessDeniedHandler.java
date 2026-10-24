package com.example.usermanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex
    ) throws IOException {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.FORBIDDEN);

        problemDetail.setTitle("Access Denied");
        problemDetail.setDetail(
                "You do not have permission to access this resource."
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/problem+json");

        objectMapper.writeValue(
                response.getOutputStream(),
                problemDetail
        );
    }
}