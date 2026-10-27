package com.nhom.parking.reservation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhom.parking.reservation.entity.Reservation;
import com.nhom.parking.reservation.entity.ReservationStatus;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	boolean existsByCode(String code);

	List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);

	List<Reservation> findByUserIdAndStatusOrderByStartTimeDesc(Long userId, ReservationStatus status);

	// SELECT ... FOR UPDATE: huỷ và check-in cùng lúc trên một đặt chỗ phải chạy lần lượt,
	// để một đặt chỗ không thể vừa CANCELLED vừa được dùng để gửi xe, hay được dùng hai lần.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Reservation r where r.id = :id")
	Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Reservation r where r.code = :code")
	Optional<Reservation> findByCodeForUpdate(@Param("code") String code);

	// Chỗ có lịch trong [startTime, endTime) giao với khung giờ đang xét.
	@Query("""
			select distinct r.slotId from Reservation r
			where r.slotId in :slotIds
			  and r.status = :status
			  and r.startTime < :endTime
			  and r.endTime > :startTime
			""")
	List<Long> findBookedSlotIds(@Param("slotIds") Collection<Long> slotIds,
								 @Param("status") ReservationStatus status,
								 @Param("startTime") LocalDateTime startTime,
								 @Param("endTime") LocalDateTime endTime
	);

	@Query("""
			select count(r) from Reservation r
			where r.slotId = :slotId
			  and r.status = :status
			  and r.startTime < :endTime
			  and r.endTime > :startTime
			""")
	long countOverlapping(@Param("slotId") Long slotId,
						  @Param("status") ReservationStatus status,
						  @Param("startTime") LocalDateTime startTime,
						  @Param("endTime") LocalDateTime endTime
	);

	// Chỗ còn lịch ACTIVE kết thúc sau thời điểm `now` (kể cả lịch chưa bắt đầu).
	@Query("""
			select distinct r.slotId from Reservation r
			where r.slotId in :slotIds
			  and r.status = :status
			  and r.endTime > :now
			""")
	List<Long> findSlotIdsWithReservationEndingAfter(@Param("slotIds") Collection<Long> slotIds,
													 @Param("status") ReservationStatus status,
													 @Param("now") LocalDateTime now
	);

}