package com.nhom.parking.slot.mapper;

import org.springframework.stereotype.Component;

import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.entity.ParkingSlot;

@Component
public class SlotMapper {

    public SlotResponse toResponse(ParkingSlot slot) {
        return new SlotResponse(slot.getId(), slot.getCode(), slot.getVehicleType(), slot.getStatus());
    }

}