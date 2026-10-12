package com.nhom.parking.session.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Khung endpoint: thêm DTO request/response và gọi service khi cài đặt.
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

	// TODO(C): Xác nhận xe vào bãi
	@PostMapping("/check-in")
	public ResponseEntity<Void> checkIn() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(C): Xác nhận xe ra, tính phí
	@PostMapping("/{id}/check-out")
	public ResponseEntity<Void> checkOut(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(C): Danh sách lượt gửi, lọc theo trạng thái
	@GetMapping
	public ResponseEntity<Void> list() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(C): Chi tiết một lượt gửi xe
	@GetMapping("/{id}")
	public ResponseEntity<Void> get(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(C): Huỷ lượt gửi nhập nhầm, khi xe chưa ra
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancel(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
