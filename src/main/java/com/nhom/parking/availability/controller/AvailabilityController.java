package com.nhom.parking.availability.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom.parking.availability.dto.AvailabilityQuery;
import com.nhom.parking.availability.dto.AvailabilityResponse;
import com.nhom.parking.availability.service.AvailabilityService;

import jakarta.validation.Valid;

// Endpoint skeleton: add request/response DTOs and call the service during implementation.
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

	private final AvailabilityService availabilityService;

	public AvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	// TODO(B): Search for available spaces by vehicle type and time slot
	@GetMapping
	public ResponseEntity<AvailabilityResponse> search(@ParameterObject @Valid AvailabilityQuery query) {
		return ResponseEntity.ok(availabilityService.search(query));
	}
}
