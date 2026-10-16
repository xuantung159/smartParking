package com.nhom.parking.reservation.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        reservation.setLicensePlate(request.licensePlate().strip());
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(ReservationStatus.ACTIVE);

        return reservationMapper.toResponse(reservationRepository.save(reservation), slot.code());
    }

    /** API 8: list reservations for the user with the given userId, newest first. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listMine(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdOrderByStartTimeDesc(userId);
        Set<Long> slotIds = reservations.stream().map(Reservation::getSlotId).collect(Collectors.toSet());
        Map<Long, String> slotCodes = slotCodes(slotIds);
        return reservations.stream()
                .map(reservation -> reservationMapper.toResponse(reservation, slotCodes.get(reservation.getSlotId())))
                .toList();
    }

    /** API 9: reservation details; only the reservation owner may view them. */
    @Transactional(readOnly = true)
    public ReservationResponse get(Long userId, Long reservationId) {
        Reservation reservation = findOwned(userId, reservationId);
        String slotCode = slotCodes(Set.of(reservation.getSlotId())).get(reservation.getSlotId());
        return reservationMapper.toResponse(reservation, slotCode);
    }

    /** API 10: cancel a reservation (change its status to CANCELLED);
     * only the owner may do so, and only while it is ACTIVE. */
    @Transactional
    public void cancel(Long userId, Long reservationId) {
        Reservation reservation = findOwned(userId, reservationId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new AppException(ErrorCode.RESERVATION_NOT_ACTIVE);
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
    }

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

    private Reservation findOwned(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!reservation.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return reservation;
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

    // TODO(C): add a method for the session module when a vehicle enters the lot when implementing API 11.

}