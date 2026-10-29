package com.nhom.parking.slot.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.reservation.repository.ReservationRepository;
import com.nhom.parking.slot.dto.CreateSlotRequest;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.entity.ParkingSlot;
import com.nhom.parking.slot.entity.SlotStatus;
import com.nhom.parking.slot.mapper.SlotMapper;
import com.nhom.parking.slot.repository.ParkingSlotRepository;

import jakarta.persistence.EntityManager;

@Service
public class SlotService {

    private final ParkingSlotRepository parkingSlotRepository;
    private final SlotMapper slotMapper;
    private final ReservationRepository reservationRepository;
    private final EntityManager entityManager;

    public SlotService(ParkingSlotRepository parkingSlotRepository, SlotMapper slotMapper,
                       ReservationRepository reservationRepository, EntityManager entityManager) {
        this.parkingSlotRepository = parkingSlotRepository;
        this.slotMapper = slotMapper;
        this.reservationRepository = reservationRepository;
        this.entityManager = entityManager;
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

    @Transactional(readOnly = true)
    public SlotResponse get(Long slotId) {
        return parkingSlotRepository.findById(slotId)
                .map(slotMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));
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
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
        }
        return slotMapper.toResponse(slot);
    }

    /** API 4: register a new parking slot; code is trimmed and upper-cased, new slot is AVAILABLE. */
    @Transactional
    public SlotResponse create(CreateSlotRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (parkingSlotRepository.existsByCode(code)) {
            throw new AppException(ErrorCode.SLOT_CODE_EXISTED);
        }

        ParkingSlot slot = new ParkingSlot();
        slot.setCode(code);
        slot.setVehicleType(request.vehicleType());
        slot.setStatus(SlotStatus.AVAILABLE);
        try {
            parkingSlotRepository.saveAndFlush(slot);
        } catch (DataIntegrityViolationException ex) {
            // Hai request cùng mã lọt qua bước kiểm tra trên: ràng buộc UNIQUE chặn lại.
            throw new AppException(ErrorCode.SLOT_CODE_EXISTED);
        }
        // created_at do DB điền, nạp lại để trả về.
        entityManager.refresh(slot);
        return slotMapper.toResponse(slot);
    }

    /**
     * API 5: stop using a parking slot (status DISABLED, the record is kept for history).
     * Rejected while a vehicle is parked there or an ACTIVE reservation has not ended yet.
     * Disabling an already DISABLED slot is a no-op.
     */
    @Transactional
    public void disable(Long slotId) {
        // Khoá bản ghi: check-in và đặt chỗ cũng khoá chỗ này nên không thể chen vào giữa lúc kiểm tra.
        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));
        if (slot.getStatus() == SlotStatus.DISABLED) {
            return;
        }
        if (slot.getStatus() == SlotStatus.OCCUPIED) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
        }
        boolean hasUpcomingReservation = !reservationRepository.findSlotIdsWithReservationEndingAfter(
                List.of(slotId), ReservationStatus.ACTIVE, LocalDateTime.now()).isEmpty();
        if (hasUpcomingReservation) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
        }
        slot.setStatus(SlotStatus.DISABLED);
        parkingSlotRepository.save(slot);
    }


    @Transactional
    public void markOccupied(Long slotId) {
        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
        }
        slot.setStatus(SlotStatus.OCCUPIED);
        parkingSlotRepository.save(slot);
    }

    @Transactional
    public void markAvailable(Long slotId) {
        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));
        if (slot.getStatus() != SlotStatus.OCCUPIED) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
        }
        slot.setStatus(SlotStatus.AVAILABLE);
        parkingSlotRepository.save(slot);
    }
}