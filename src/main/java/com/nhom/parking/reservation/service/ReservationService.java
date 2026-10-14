package com.nhom.parking.reservation.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.reservation.repository.ReservationRepository;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public Set<Long> findBookedSlotIds(Collection<Long> slotIds, LocalDateTime startTime, LocalDateTime endTime) {
        if (slotIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(
                reservationRepository.findBookedSlotIds(slotIds, ReservationStatus.ACTIVE, startTime, endTime));
    }

    // TODO(B): create, view, and cancel reservations (APIs 7–10)

}