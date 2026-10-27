package com.nhom.parking.slot.dto;

import java.time.LocalDateTime;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.slot.entity.SlotStatus;

public record SlotResponse(Long id, String code, VehicleType vehicleType, SlotStatus status, LocalDateTime createdAt) {

    // Dùng khi không cần thời điểm tạo (nghiệp vụ nội bộ, test).
    public SlotResponse(Long id, String code, VehicleType vehicleType, SlotStatus status) {
        this(id, code, vehicleType, status, null);
    }

}
