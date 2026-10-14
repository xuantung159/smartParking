package com.nhom.parking.reservation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom.parking.reservation.entity.Reservation;
import com.nhom.parking.reservation.entity.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query(
			"""
			select distinct r.slotId from Reservation r
			where r.slotId in :slotIds
			  and r.status = :status
			  and r.startTime < :endTime
			  and r.endTime > :startTime"""
	)
    List<Long> findBookedSlotIds(@Param("slotIds") Collection<Long> slotIds,
                                 @Param("status") ReservationStatus status,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

}