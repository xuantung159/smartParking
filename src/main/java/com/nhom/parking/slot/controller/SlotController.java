package com.nhom.parking.slot.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nhom.parking.slot.dto.CreateSlotRequest;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.service.SlotService;

import jakarta.validation.Valid;

// Chỉ ADMIN.
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/slots")
public class SlotController {

	private final SlotService slotService;

	public SlotController(SlotService slotService) {
		this.slotService = slotService;
	}

	// API 4: Khai báo chỗ đỗ mới
	@PostMapping
	public ResponseEntity<SlotResponse> create(@Valid @RequestBody CreateSlotRequest request) {
		SlotResponse response = slotService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	// API 5: Ngừng sử dụng một chỗ đỗ
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> disable(@PathVariable Long id) {
		slotService.disable(id);
		return ResponseEntity.noContent().build();
	}

}
