package com.nhom.parking.slot.dto;

import com.nhom.parking.common.enums.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSlotRequest(

        @NotBlank(message = "Mã chỗ đỗ không được để trống")
        @Size(max = 20, message = "Mã chỗ đỗ tối đa 20 ký tự")
        String code,

        @NotNull(message = "Loại xe không được để trống")
        VehicleType vehicleType) {

}
