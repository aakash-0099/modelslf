package com.example.usermanagement.service;

import com.example.usermanagement.cache.CacheInvalidationEventRepository;
import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.CacheInvalidationEvent;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheInvalidationEventRepository cacheInvalidationEventRepository;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(
        UserRepository userRepository,
        UserMapper userMapper,
        PasswordEncoder passwordEncoder,
        CacheInvalidationEventRepository cacheInvalidationEventRepository
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.cacheInvalidationEventRepository = cacheInvalidationEventRepository;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "users", key = "'all'")
    public UserResponse createUser(CreateUserRequest request) {

        User user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Cacheable(value = "users", key = "#id", sync = true)
    public UserResponse getUser(Long id) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id)
                );

        log.info("Loading user {} from PostgreSQL", id);
        return userMapper.toResponse(user);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "users", key = "'all'")
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
	@Transactional
	@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
	@Caching(evict = {
			@CacheEvict(value = "users", key = "#id"),
			@CacheEvict(value = "users", key = "'all'")
	})
	public UserResponse updateUser(
			Long id,
			UpdateUserRequest request
	) {

		User user = userRepository.findById(id)
				.orElseThrow(() ->
						new UserNotFoundException(id)
				);

		user.setName(request.getName());
		user.setEmail(request.getEmail());

		User updatedUser = userRepository.save(user);

		cacheInvalidationEventRepository.save(
				new CacheInvalidationEvent(
						"users",							// Cache name
						String.valueOf(id)					// Cache key
				)
		);

		cacheInvalidationEventRepository.save(
				new CacheInvalidationEvent(
						"users",
						"all"
				)
		);

		return userMapper.toResponse(updatedUser);
	}

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "users", key = "'all'")
    })
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id)
                );

        userRepository.delete(user);
    }
}