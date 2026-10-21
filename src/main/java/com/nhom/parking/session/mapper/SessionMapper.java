package com.nhom.parking.session.mapper;

import org.springframework.stereotype.Component;

import com.nhom.parking.session.dto.SessionResponse;
import com.nhom.parking.session.entity.ParkingSession;

@Component
public class SessionMapper {

    public SessionResponse toResponse(ParkingSession entity) {
        return new SessionResponse(
                entity.getId(),
                entity.getSlotId(),
                entity.getReservationId(),
                entity.getLicensePlate(),
                entity.getVehicleType(),
                entity.getCheckInTime(),
                entity.getCheckOutTime(),
                entity.getFee(),
                entity.getStatus());
    }
}