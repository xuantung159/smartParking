package com.nhom.parking.slot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Khung endpoint: thêm DTO request/response và gọi service khi cài đặt.
@RestController
@RequestMapping("/api/slots")
public class SlotController {

	// TODO(A): Khai báo chỗ đỗ mới
	@PostMapping
	public ResponseEntity<Void> create() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	// TODO(A): Ngừng sử dụng một chỗ đỗ
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> disable(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
