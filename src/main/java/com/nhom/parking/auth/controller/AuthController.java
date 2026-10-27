package com.nhom.parking.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom.parking.auth.dto.LoginRequest;
import com.nhom.parking.auth.dto.LoginResponse;
import com.nhom.parking.auth.dto.RegisterRequest;
import com.nhom.parking.auth.service.AuthService;
import com.nhom.parking.common.security.AuthenticatedUser;
import com.nhom.parking.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	// API 1: Đăng nhập, cấp mã thông báo
	@PostMapping("/login")
	@PreAuthorize("permitAll()")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	// Đăng ký tài khoản khách hàng, trả luôn mã thông báo
	@PostMapping("/register")
	@PreAuthorize("permitAll()")
	public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	// Thông tin tài khoản đang đăng nhập
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
		return ResponseEntity.ok(authService.me(user.id()));
	}

}
