package com.nhom.parking.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.common.security.AuthenticatedUser;
import com.nhom.parking.reservation.dto.CreateReservationRequest;
import com.nhom.parking.reservation.dto.ReservationResponse;
import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.reservation.service.ReservationService;

import jakarta.validation.Valid;

// Khung endpoint: thêm DTO request/response và gọi service khi cài đặt.
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	// API 7: Create a reservation
	@PostMapping
	public ResponseEntity<ReservationResponse> create(
			@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody CreateReservationRequest request) {
		ReservationResponse response = reservationService.create(currentUserId(user), request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	// API 8: List the user's reservations, optionally filtered by status
	@GetMapping("/me")
	public ResponseEntity<List<ReservationResponse>> listMine(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) ReservationStatus status) {
		return ResponseEntity.ok(reservationService.listMine(currentUserId(user), status));
	}

	// API 9: Details of one of the user's reservations
	@GetMapping("/{id}")
	public ResponseEntity<ReservationResponse> get(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long id) {
		return ResponseEntity.ok(reservationService.get(currentUserId(user), id));
	}

	// API 10: Cancel a reservation
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancel(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long id) {
		reservationService.cancel(currentUserId(user), id);
		return ResponseEntity.noContent().build();
	}

	// If the JWT filter is not present, the principal is null: return 401 instead of NullPointerException.
	private static Long currentUserId(AuthenticatedUser user) {
		if (user == null) {
			throw new AppException(ErrorCode.UNAUTHENTICATED);
		}
		return user.id();
	}

}
