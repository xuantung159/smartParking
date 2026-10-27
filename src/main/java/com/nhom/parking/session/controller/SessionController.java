package com.nhom.parking.session.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom.parking.session.dto.CheckInRequest;
import com.nhom.parking.session.dto.CheckOutResponse;
import com.nhom.parking.session.dto.SessionResponse;
import com.nhom.parking.session.entity.SessionStatus;
import com.nhom.parking.session.service.ParkingSessionService;

import jakarta.validation.Valid;

// Chỉ STAFF.
@RestController
@PreAuthorize("hasRole('STAFF')")
@RequestMapping("/api/sessions")
public class SessionController {

    private final ParkingSessionService sessionService;

    public SessionController(ParkingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    // API 11: Xác nhận xe vào bãi
    @PostMapping("/check-in")
    public ResponseEntity<SessionResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        SessionResponse response = sessionService.checkIn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // API 12: Xác nhận xe ra, tính phí
    @PostMapping("/{id}/check-out")
    public ResponseEntity<CheckOutResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.checkOut(id));
    }

    // API 13: Danh sách lượt gửi, lọc theo trạng thái
    @GetMapping
    public ResponseEntity<List<SessionResponse>> list(
            @RequestParam(required = false) SessionStatus status) {
        return ResponseEntity.ok(sessionService.list(status));
    }

    // API 14: Chi tiết một lượt gửi xe
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.get(id));
    }

    // API 15: Huỷ lượt gửi nhập nhằm, khi xe chưa ra
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        sessionService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}