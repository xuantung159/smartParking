package com.nhom.parking.slot.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.slot.entity.ParkingSlot;
import com.nhom.parking.slot.entity.SlotStatus;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByVehicleTypeAndStatusInOrderByCodeAsc(
            VehicleType vehicleType,
            Collection<SlotStatus> statuses
    );

}