package com.nhom.parking.reservation.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(

        @NotNull(message = "Mã chỗ đỗ không được để trống")
        Long slotId,

        @NotBlank(message = "Biển số xe không được để trống")
        @Size(max = 20, message = "Biển số xe tối đa 20 ký tự")
        String licensePlate,

        @NotNull(message = "Giờ bắt đầu không được để trống")
        @Future(message = "Giờ bắt đầu phải ở tương lai")
        LocalDateTime startTime,

        @NotNull(message = "Giờ kết thúc không được để trống")
        LocalDateTime endTime) {

    @AssertTrue(message = "Giờ kết thúc phải sau giờ bắt đầu")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

}