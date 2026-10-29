package com.nhom.parking.user.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.common.dto.PageResponse;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.user.dto.CreateUserRequest;
import com.nhom.parking.user.dto.UserResponse;
import com.nhom.parking.user.entity.Role;
import com.nhom.parking.user.entity.User;
import com.nhom.parking.user.mapper.UserMapper;
import com.nhom.parking.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class UserService {

    static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       PasswordEncoder passwordEncoder, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    /** API 2: create an account; password is stored as a BCrypt hash. */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        return create(request.username(), request.password(), request.fullName(), request.role());
    }

    /** Shared by API 2 and self-registration. */
    @Transactional
    public UserResponse create(String rawUsername, String password, String fullName, Role role) {
        String username = rawUsername.trim();
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        user.setRole(role);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Hai request cùng tên đăng nhập lọt qua bước kiểm tra trên: ràng buộc UNIQUE chặn lại.
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }
        // created_at do DB điền, nạp lại để trả về.
        entityManager.refresh(user);
        return userMapper.toResponse(user);
    }

    /** API 3: list accounts, optionally filtered by role, ordered by id. */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Role role, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        return PageResponse.of(
                role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable),
                userMapper::toResponse);
    }

}
