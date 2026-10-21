package com.nhom.parking.reservation.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.common.security.AuthenticatedUser;
import com.nhom.parking.reservation.dto.CreateReservationRequest;
import com.nhom.parking.reservation.dto.ReservationResponse;
import com.nhom.parking.reservation.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	// API 7 (CUSTOMER): create a reservation
	@PostMapping
	public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
													  @Valid @RequestBody CreateReservationRequest request)
	{
		ReservationResponse response = reservationService.create(userId(user), request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	// API 8 (CUSTOMER): list the customer's own reservations
	@GetMapping("/me")
	public ResponseEntity<List<ReservationResponse>> listMine(@AuthenticationPrincipal AuthenticatedUser user) {
		return ResponseEntity.ok(reservationService.listMine(userId(user)));
	}

	// API 9 (CUSTOMER): details of the customer's own reservation
	@GetMapping("/{id}")
	public ResponseEntity<ReservationResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
												   @PathVariable Long id) {
		return ResponseEntity.ok(reservationService.get(userId(user), id));
	}

	// API 10 (CUSTOMER): cancel the customer's own reservation
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
		reservationService.cancel(userId(user), id);
		return ResponseEntity.noContent().build();
	}

	// If not logged in, return 401.
	private static Long userId(AuthenticatedUser user) {
		if (user == null) {
			throw new AppException(ErrorCode.UNAUTHENTICATED);
		}
		return user.id();
	}

}