package com.nhom.parking.reservation.controller;

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
@RequestMapping("/api/reservations")
public class ReservationController {

	// TODO(B): Tạo đặt chỗ trước
	@PostMapping
	public ResponseEntity<Void> create() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(B): Danh sách đặt chỗ của bản thân
	@GetMapping("/me")
	public ResponseEntity<Void> listMine() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(B): Chi tiết một đặt chỗ
	@GetMapping("/{id}")
	public ResponseEntity<Void> get(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(B): Huỷ một đặt chỗ
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancel(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
