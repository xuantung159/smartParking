package com.nhom.parking.session.dto;

import java.time.LocalDateTime;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.session.entity.SessionStatus;

public record SessionResponse(
        Long id,
        Long slotId,
        Long reservationId,
        String licensePlate,
        VehicleType vehicleType,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        Long fee,
        SessionStatus status) {
}