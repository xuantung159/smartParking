package com.nhom.parking.availability.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom.parking.availability.dto.AvailabilityQuery;
import com.nhom.parking.availability.dto.AvailabilityResponse;
import com.nhom.parking.availability.service.AvailabilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

	private final AvailabilityService availabilityService;

	public AvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	// API 6: Tra cứu chỗ trống theo loại xe và khung giờ (công khai, không giữ chỗ)
	@GetMapping
	@PreAuthorize("permitAll()")
	public ResponseEntity<AvailabilityResponse> search(@ParameterObject @Valid AvailabilityQuery query) {
		return ResponseEntity.ok(availabilityService.search(query));
	}

}