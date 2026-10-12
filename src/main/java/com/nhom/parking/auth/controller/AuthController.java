package com.nhom.parking.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Khung endpoint: thêm DTO request/response và gọi service khi cài đặt.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	// TODO(A): Đăng nhập, cấp mã thông báo
	@PostMapping("/login")
	public ResponseEntity<Void> login() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
