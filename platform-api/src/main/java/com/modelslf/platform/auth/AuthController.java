package com.modelslf.platform.auth;

import com.modelslf.platform.dto.CreateUserRequest;
import com.modelslf.platform.auth.dto.UserResponse;
import com.modelslf.platform.service.UserService;
import com.modelslf.platform.auth.AuthService;
import com.modelslf.platform.auth.dto.RegisterRequest;
import com.modelslf.platform.auth.dto.LoginRequest;
import com.modelslf.platform.auth.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }
}