package com.nhom.parking.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.auth.dto.LoginRequest;
import com.nhom.parking.auth.dto.LoginResponse;
import com.nhom.parking.auth.dto.RegisterRequest;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.common.security.AuthenticatedUser;
import com.nhom.parking.common.security.JwtService;
import com.nhom.parking.user.dto.UserResponse;
import com.nhom.parking.user.entity.Role;
import com.nhom.parking.user.mapper.UserMapper;
import com.nhom.parking.user.repository.UserRepository;
import com.nhom.parking.user.service.UserService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserMapper userMapper, UserService userService,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Đăng nhập **/
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserResponse user = userRepository.findByUsername(request.username().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .map(userMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
        return tokenFor(user);
    }

    /** Đăng ký **/
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        return tokenFor(userService.create(request.username(), request.password(), request.fullName(),
                Role.CUSTOMER));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private LoginResponse tokenFor(UserResponse user) {
        String token = jwtService.generateToken(
                new AuthenticatedUser(user.id(), user.username(), user.role().name()));
        return new LoginResponse(token, "Bearer", jwtService.getExpiresInSeconds(), user);
    }

}
