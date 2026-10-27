package com.nhom.parking.user.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nhom.parking.common.dto.PageResponse;
import com.nhom.parking.user.dto.CreateUserRequest;
import com.nhom.parking.user.dto.UserResponse;
import com.nhom.parking.user.entity.Role;
import com.nhom.parking.user.service.UserService;

import jakarta.validation.Valid;

// Chỉ ADMIN.
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	// API 2: Tạo tài khoản
	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		UserResponse response = userService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	// API 3: Danh sách tài khoản, lọc theo vai trò
	@GetMapping
	public ResponseEntity<PageResponse<UserResponse>> list(
			@RequestParam(required = false) Role role,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(userService.list(role, page, size));
	}

}
