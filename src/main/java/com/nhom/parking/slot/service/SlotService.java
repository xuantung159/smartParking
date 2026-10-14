package com.nhom.parking.slot.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.entity.SlotStatus;
import com.nhom.parking.slot.mapper.SlotMapper;
import com.nhom.parking.slot.repository.ParkingSlotRepository;

@Service
public class SlotService {

    private final ParkingSlotRepository parkingSlotRepository;
    private final SlotMapper slotMapper;

    public SlotService(ParkingSlotRepository parkingSlotRepository, SlotMapper slotMapper) {
        this.parkingSlotRepository = parkingSlotRepository;
        this.slotMapper = slotMapper;
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> findUsableByVehicleType(VehicleType vehicleType, boolean includeOccupied) {
        List<SlotStatus> statuses = includeOccupied
                ? List.of(SlotStatus.AVAILABLE, SlotStatus.OCCUPIED)
                : List.of(SlotStatus.AVAILABLE);
        return parkingSlotRepository.findByVehicleTypeAndStatusInOrderByCodeAsc(vehicleType, statuses).stream()
                .map(slotMapper::toResponse)
                .toList();
    }

    // TODO(A): register new parking slots and discontinue use of parking slots (APIs 4 and 5)

}