package com.nhom.parking.slot.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.entity.ParkingSlot;
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

    /**
     * Usable parking spaces for a vehicle type, sorted by code.
     * DISABLED spaces are always excluded;
     * OCCUPIED spaces are included only when includeOccupied = true.
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> findUsableByVehicleType(VehicleType vehicleType, boolean includeOccupied) {
        List<SlotStatus> statuses = includeOccupied
                ? List.of(SlotStatus.AVAILABLE, SlotStatus.OCCUPIED)
                : List.of(SlotStatus.AVAILABLE);
        return parkingSlotRepository.findByVehicleTypeAndStatusInOrderByCodeAsc(vehicleType, statuses).stream()
                .map(slotMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> findByIds(Collection<Long> slotIds) {
        return parkingSlotRepository.findAllById(slotIds).stream()
                .map(slotMapper::toResponse)
                .toList();
    }

    /**
     * Locks the parking space record so the caller can check it and create a
     * reservation without another request interfering.

     * Must be called within an existing transaction of the caller
     * (the lock is released when that transaction ends).

     * DISABLED spaces cannot be reserved.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SlotResponse lockBookableSlot(Long slotId) {
        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));
        if (slot.getStatus() == SlotStatus.DISABLED) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
        }
        return slotMapper.toResponse(slot);
    }

    // TODO(A): register new parking slots and discontinue parking slots (APIs 4 and 5)

}