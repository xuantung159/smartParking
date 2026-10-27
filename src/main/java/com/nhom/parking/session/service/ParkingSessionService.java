package com.nhom.parking.session.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.reservation.entity.Reservation;
import com.nhom.parking.reservation.repository.ReservationRepository;
import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.session.dto.CheckInRequest;
import com.nhom.parking.session.dto.CheckOutResponse;
import com.nhom.parking.session.dto.SessionResponse;
import com.nhom.parking.session.entity.ParkingSession;
import com.nhom.parking.session.entity.SessionStatus;
import com.nhom.parking.session.mapper.SessionMapper;
import com.nhom.parking.session.repository.ParkingSessionRepository;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.service.SlotService;

@Service
public class ParkingSessionService {

    // Giá tối thiểu cho baseline (đồng)
    private static final long FEE_PER_HOUR_MOTORBIKE = 5_000L;
    private static final long FEE_PER_HOUR_CAR = 20_000L;

    private final ParkingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final SlotService slotService;
    private final SessionMapper sessionMapper;

    public ParkingSessionService(ParkingSessionRepository sessionRepository,
                                 ReservationRepository reservationRepository,
                                 SlotService slotService,
                                 SessionMapper sessionMapper) {
        this.sessionRepository = sessionRepository;
        this.reservationRepository = reservationRepository;
        this.slotService = slotService;
        this.sessionMapper = sessionMapper;
    }

    // API 11: check-in 
    @Transactional
    public SessionResponse checkIn(CheckInRequest request) {
        String licensePlate = request.getLicensePlate().strip().toUpperCase(Locale.ROOT);

        // 1. Xe đang gửi trong bãi?
        if (sessionRepository.existsByLicensePlateIgnoreCaseAndStatus(licensePlate, SessionStatus.ACTIVE)) {
            throw new AppException(ErrorCode.VEHICLE_ALREADY_PARKED);
        }

        Long slotId;
        Long reservationId = null;
        Reservation reservation = null;

        // 2. Nếu có reservationId, kiểm tra và lấy slot đã đặt
        if (request.getReservationId() != null) {
            reservation = reservationRepository.findByIdForUpdate(request.getReservationId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));

            if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                throw new AppException(ErrorCode.RESERVATION_NOT_ACTIVE);
            }
            if (!reservation.getLicensePlate().strip().equalsIgnoreCase(licensePlate)) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(reservation.getStartTime()) || !now.isBefore(reservation.getEndTime())) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
            if (slotService.get(reservation.getSlotId()).vehicleType() != request.getVehicleType()) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            slotId = reservation.getSlotId();
            reservationId = reservation.getId();
        } else {
            // 3. Khách vãng lai: tìm slot trống phù hợp
            slotId = findAvailableSlotId(request.getVehicleType());
        }

        // 4. Đổi trạng thái slot sang OCCUPIED
        slotService.markOccupied(slotId);

        if (reservation != null) {
            reservation.setStatus(ReservationStatus.USED);
        }

        // 5. Tạo session
        ParkingSession session = new ParkingSession();
        session.setSlotId(slotId);
        session.setReservationId(reservationId);
        session.setLicensePlate(licensePlate);
        session.setVehicleType(request.getVehicleType());
        session.setCheckInTime(LocalDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);

        ParkingSession saved = sessionRepository.save(session);
        return sessionMapper.toResponse(saved);
    }

    // API 12: check-out 
    @Transactional
    public CheckOutResponse checkOut(Long sessionId) {
        ParkingSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new AppException(ErrorCode.SESSION_ALREADY_CLOSED);
        }

        LocalDateTime now = LocalDateTime.now();
        long fee = calculateFee(session.getVehicleType(), session.getCheckInTime(), now);

        session.setCheckOutTime(now);
        session.setFee(fee);
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        // Giải phóng slot
        slotService.markAvailable(session.getSlotId());

        return new CheckOutResponse(
                session.getId(),
                session.getCheckInTime(),
                session.getCheckOutTime(),
                session.getFee(),
                session.getStatus().name());
    }

    // API 13: list 
    @Transactional(readOnly = true)
    public List<SessionResponse> list(SessionStatus status) {
        List<ParkingSession> sessions = (status == null)
                ? sessionRepository.findAllByOrderByCheckInTimeDesc()
                : sessionRepository.findByStatusOrderByCheckInTimeDesc(status);
        return sessions.stream().map(sessionMapper::toResponse).toList();
    }

    // API 14: get by id 
    @Transactional(readOnly = true)
    public SessionResponse get(Long id) {
        ParkingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        return sessionMapper.toResponse(session);
    }

    // API 15: cancel
    @Transactional
    public void cancel(Long id) {
        ParkingSession session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        // Chỉ huỷ được khi xe chưa ra
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new AppException(ErrorCode.SESSION_ALREADY_CLOSED);
        }

        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);

        // Giải phóng slot
        slotService.markAvailable(session.getSlotId());
    }

    // Helpers 
    private Long findAvailableSlotId(VehicleType vehicleType) {
        List<SlotResponse> slots = slotService.findUsableByVehicleType(vehicleType, false);
        if (slots.isEmpty()) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
        }
        return slots.get(0).id();
    }

    private long calculateFee(VehicleType vehicleType, LocalDateTime in, LocalDateTime out) {
        Duration duration = Duration.between(in, out);
        long hours = duration.toHours();
        if (!duration.minusHours(hours).isZero()) {
            hours++;
        }
        hours = Math.max(1, hours);
        long rate = (vehicleType == VehicleType.MOTORBIKE) ? FEE_PER_HOUR_MOTORBIKE : FEE_PER_HOUR_CAR;
        return hours * rate;
    }
}