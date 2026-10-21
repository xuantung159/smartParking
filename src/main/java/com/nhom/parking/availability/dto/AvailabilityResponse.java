package com.nhom.parking.availability.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nhom.parking.common.enums.VehicleType;

public record AvailabilityResponse(
        VehicleType vehicleType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int availableCount,
        List<Slot> slots)
{

    public record Slot(Long id, String code) {
    }

    public static AvailabilityResponse of(
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<Slot> slots)
    {
        return new AvailabilityResponse(vehicleType, startTime, endTime, slots.size(), slots);
    }

}