package com.nhom.parking.reservation.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.reservation.dto.CreateReservationRequest;
import com.nhom.parking.reservation.dto.ReservationResponse;
import com.nhom.parking.reservation.entity.Reservation;
import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.reservation.mapper.ReservationMapper;
import com.nhom.parking.reservation.repository.ReservationRepository;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.service.SlotService;

@Service
public class ReservationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String CODE_PREFIX = "RSV-";
    private static final int CODE_RANDOM_LENGTH = 8;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final SlotService slotService;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationMapper reservationMapper,
                              SlotService slotService) {
        this.reservationRepository = reservationRepository;
        this.reservationMapper = reservationMapper;
        this.slotService = slotService;
    }

    /** API 7: create a reservation for the user with the given userId. */
    @Transactional
    public ReservationResponse create(Long userId, CreateReservationRequest request) {
        SlotResponse slot = slotService.lockBookableSlot(request.slotId());

        long overlapping = reservationRepository.countOverlapping(slot.id(), ReservationStatus.ACTIVE,
                request.startTime(), request.endTime());
        if (overlapping > 0) {
            throw new AppException(ErrorCode.RESERVATION_TIME_CONFLICT);
        }

        Reservation reservation = new Reservation();
        reservation.setCode(generateUniqueCode());
        reservation.setUserId(userId);
        reservation.setSlotId(slot.id());
        reservation.setLicensePlate(normalizePlate(request.licensePlate()));
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(ReservationStatus.ACTIVE);

        return reservationMapper.toResponse(reservationRepository.save(reservation), slot.code());
    }

    /** API 8: list the user's reservations, newest first; if status = null, return all. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listMine(Long userId, ReservationStatus status) {
        List<Reservation> reservations = (status == null)
                ? reservationRepository.findByUserIdOrderByStartTimeDesc(userId)
                : reservationRepository.findByUserIdAndStatusOrderByStartTimeDesc(userId, status);
        Set<Long> slotIds = reservations.stream().map(Reservation::getSlotId).collect(Collectors.toSet());
        Map<Long, String> slotCodes = slotCodes(slotIds);
        return reservations.stream()
                .map(reservation -> reservationMapper.toResponse(reservation, slotCodes.get(reservation.getSlotId())))
                .toList();
    }

    /** API 9: reservation details; only the reservation owner may view them. */
    @Transactional(readOnly = true)
    public ReservationResponse get(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));
        ensureOwner(userId, reservation);
        String slotCode = slotCodes(Set.of(reservation.getSlotId())).get(reservation.getSlotId());
        return reservationMapper.toResponse(reservation, slotCode);
    }

    /**
     * API 10: cancel a reservation (change it to CANCELLED); only the reservation owner may cancel it.
     * ACTIVE: cancel. CANCELLED: cancelling again still succeeds (204). USED: 409.
     */
    @Transactional
    public void cancel(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));
        ensureOwner(userId, reservation);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }
        if (reservation.getStatus() == ReservationStatus.USED) {
            throw new AppException(ErrorCode.RESERVATION_NOT_ACTIVE);
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    // ===== Used by other modules (through the service, without using this module's repository/entity) =====

    /** Used by availability: slots with an ACTIVE reservation overlapping [startTime, endTime). */
    @Transactional(readOnly = true)
    public Set<Long> findBookedSlotIds(Collection<Long> slotIds,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime) {
        if (slotIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(
                reservationRepository.findBookedSlotIds(slotIds, ReservationStatus.ACTIVE, startTime, endTime));
    }

    /**
     * Used by session (API 11, walk-in customer) and slot (API 5, discontinue slot use):
     * slots with an ACTIVE reservation ending after {@code now}.
     */
    @Transactional(readOnly = true)
    public Set<Long> findSlotIdsReservedAfter(Collection<Long> slotIds, LocalDateTime now) {
        if (slotIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(reservationRepository.findSlotIdsWithReservationEndingAfter(
                slotIds, ReservationStatus.ACTIVE, now));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ReservationResponse useForCheckIn(String reservationCode, String licensePlate,
                                             VehicleType vehicleType, LocalDateTime now) {
        Reservation reservation = reservationRepository
                .findByCodeForUpdate(reservationCode.strip().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new AppException(ErrorCode.RESERVATION_NOT_ACTIVE);
        }
        if (now.isBefore(reservation.getStartTime()) || !now.isBefore(reservation.getEndTime())) {
            throw new AppException(ErrorCode.RESERVATION_NOT_IN_TIME);
        }
        if (!reservation.getLicensePlate().equalsIgnoreCase(licensePlate.strip())) {
            throw new AppException(ErrorCode.RESERVATION_VEHICLE_MISMATCH);
        }

        SlotResponse slot = slotService.findByIds(Set.of(reservation.getSlotId())).stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));
        if (slot.vehicleType() != vehicleType) {
            throw new AppException(ErrorCode.RESERVATION_VEHICLE_MISMATCH);
        }

        reservation.setStatus(ReservationStatus.USED);
        return reservationMapper.toResponse(reservation, slot.code());
    }

    // ===== Internal methods =====

    private static void ensureOwner(Long userId, Reservation reservation) {
        if (!reservation.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private static String normalizePlate(String licensePlate) {
        return licensePlate.strip().toUpperCase(Locale.ROOT);
    }

    private Map<Long, String> slotCodes(Collection<Long> slotIds) {
        return slotService.findByIds(slotIds).stream()
                .collect(Collectors.toMap(SlotResponse::id, SlotResponse::code));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!reservationRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new AppException(ErrorCode.INTERNAL_ERROR);
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_PREFIX);
        for (int i = 0; i < CODE_RANDOM_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

}