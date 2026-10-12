package com.nhom.parking.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Khung endpoint: thêm DTO request/response và gọi service khi cài đặt.
@RestController
@RequestMapping("/api/users")
public class UserController {

	// TODO(A): Tạo tài khoản
	@PostMapping
	public ResponseEntity<Void> create() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(A): Danh sách tài khoản, lọc theo vai trò
	@GetMapping
	public ResponseEntity<Void> list() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
