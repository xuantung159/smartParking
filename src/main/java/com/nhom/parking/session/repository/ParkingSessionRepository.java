package com.nhom.parking.session.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom.parking.session.entity.ParkingSession;
import com.nhom.parking.session.entity.SessionStatus;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    boolean existsByLicensePlateAndStatus(String licensePlate, SessionStatus status);

    List<ParkingSession> findByStatusOrderByCheckInTimeDesc(SessionStatus status);

    List<ParkingSession> findAllByOrderByCheckInTimeDesc();

    Optional<ParkingSession> findByIdAndStatus(Long id, SessionStatus status);

    List<ParkingSession> findBySlotIdAndStatus(Long slotId, SessionStatus status);

    List<ParkingSession> findBySlotIdInAndStatus(Collection<Long> slotIds, SessionStatus status);
}