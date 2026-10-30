package com.modelslf.platform.auth;

import com.modelslf.platform.dto.CreateUserRequest;
import com.modelslf.platform.auth.dto.UserResponse;
import com.modelslf.platform.auth.domain.User;
import com.modelslf.platform.common.exception.UserNotFoundException;
import com.modelslf.platform.mapper.UserMapper;
import com.modelslf.platform.auth.domain.UserRepository;
import com.modelslf.platform.auth.dto.LoginRequest;
import com.modelslf.platform.auth.dto.LoginResponse;
import com.modelslf.platform.auth.dto.RegisterRequest;
import com.modelslf.platform.auth.domain.Role;
import com.modelslf.platform.auth.security.JwtService;
import com.modelslf.platform.common.exception.EmailAlreadyExistsException;
import org.springframework.security.authentication.BadCredentialsException;
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