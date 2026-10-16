package com.nhom.parking.reservation.dto;

import java.time.LocalDateTime;

import com.nhom.parking.reservation.entity.ReservationStatus;

public record ReservationResponse(
        Long id,
        String code,
        Long slotId,
        String slotCode,
        String licensePlate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status) {
}