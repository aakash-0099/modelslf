package com.modelslf.platform.auth;

import com.modelslf.platform.auth.domain.User;
import com.modelslf.platform.auth.domain.UserRepository;
import com.modelslf.platform.auth.dto.LoginRequest;
import com.modelslf.platform.auth.dto.LoginResponse;
import com.modelslf.platform.auth.dto.RegisterRequest;
import com.modelslf.platform.auth.domain.Role;
import com.modelslf.platform.auth.dto.UserResponse;
import com.modelslf.platform.auth.security.JwtService;
import com.modelslf.platform.common.exception.EmailAlreadyExistsException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isEnabled()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password")
                );

        if (!user.isEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);
        return new LoginResponse(accessToken);
    }
}