package com.nhom.parking.session.dto;

import java.time.LocalDateTime;

public record CheckOutResponse(
        Long sessionId,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        Long fee,
        String status) {
}