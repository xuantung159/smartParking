package com.nhom.parking.session.dto;

import com.nhom.parking.common.enums.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CheckInRequest {

    @NotBlank(message = "License plate must not be empty")
    @Size(max = 20, message = "License plate too long")
    private String licensePlate;

    @NotNull(message = "Vehicle type must not be empty")
    private VehicleType vehicleType;

    // Có thể null nếu khách vãng lai
    private Long reservationId;

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }
}