package com.nhom.parking.reservation.mapper;

import org.springframework.stereotype.Component;

import com.nhom.parking.reservation.dto.ReservationResponse;
import com.nhom.parking.reservation.entity.Reservation;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation, String slotCode)
    {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCode(),
                reservation.getSlotId(),
                slotCode,
                reservation.getLicensePlate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus()
        );
    }

}