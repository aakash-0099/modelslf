package com.example.usermanagement.service;

import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.dto.auth.LoginRequest;
import com.example.usermanagement.dto.auth.LoginResponse;
import com.example.usermanagement.dto.auth.RegisterRequest;
import com.example.usermanagement.entity.Role;
import com.example.usermanagement.security.JwtService;
import com.example.usermanagement.exception.EmailAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
	private final UserMapper userMapper;

    public AuthService(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
			JwtService jwtService,
		UserMapper userMapper
	) {
			this.userRepository = userRepository;
			this.passwordEncoder = passwordEncoder;
			this.jwtService = jwtService;
			this.userMapper = userMapper;
	}

    public UserResponse register(RegisterRequest request) {

			if (userRepository.existsByEmail(request.getEmail())) {
			throw new EmailAlreadyExistsException(
					"Email already registered"
			);
			}

			User user = userMapper.toEntity(request);

			user.setPassword(passwordEncoder.encode(request.getPassword()));
			user.setRole(Role.USER);
			user.setEnabled(true);

			User savedUser = userRepository.save(user);

			return userMapper.toResponse(savedUser);
	}

    public LoginResponse login(LoginRequest request) {

		User user = userRepository
				.findByEmail(request.getEmail())
				.orElseThrow(() ->
						new RuntimeException("Invalid email or password")
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
				throw new RuntimeException("Invalid email or password");
		}
		String accessToken = jwtService.generateToken(user);

		return new LoginResponse(accessToken);
	}
}