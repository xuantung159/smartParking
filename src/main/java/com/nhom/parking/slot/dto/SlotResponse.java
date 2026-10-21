package com.nhom.parking.slot.dto;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.slot.entity.SlotStatus;

public record SlotResponse(Long id, String code, VehicleType vehicleType, SlotStatus status) {
}