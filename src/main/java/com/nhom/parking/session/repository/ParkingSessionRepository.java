package com.nhom.parking.session.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom.parking.session.entity.ParkingSession;
import com.nhom.parking.session.entity.SessionStatus;

import jakarta.persistence.LockModeType;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    boolean existsByLicensePlateIgnoreCaseAndStatus(String licensePlate, SessionStatus status);

    List<ParkingSession> findByStatusOrderByCheckInTimeDesc(SessionStatus status);

    List<ParkingSession> findAllByOrderByCheckInTimeDesc();

    Optional<ParkingSession> findByIdAndStatus(Long id, SessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSession s where s.id = :id")
    Optional<ParkingSession> findByIdForUpdate(@Param("id") Long id);

    List<ParkingSession> findBySlotIdAndStatus(Long slotId, SessionStatus status);

    List<ParkingSession> findBySlotIdInAndStatus(Collection<Long> slotIds, SessionStatus status);
}