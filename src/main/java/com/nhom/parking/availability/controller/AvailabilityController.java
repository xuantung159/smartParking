package com.nhom.parking.availability.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Khung endpoint: thêm DTO request/response và gọi service khi cài đặt.
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

	// TODO(B): Tra cứu chỗ trống theo loại xe và khung giờ
	@GetMapping
	public ResponseEntity<Void> search() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
