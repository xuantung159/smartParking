package com.nhom.parking.reservation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import com.nhom.parking.reservation.entity.Reservation;
import com.nhom.parking.reservation.entity.ReservationStatus;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	boolean existsByCode(String code);

	List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);

	List<Reservation> findByUserIdAndStatusOrderByStartTimeDesc(Long userId, ReservationStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Reservation r where r.id = :id")
	Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Reservation r where r.code = :code")
	Optional<Reservation> findByCodeForUpdate(@Param("code") String code);

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

	@Query("""
			select count(r) from Reservation r
			where r.licensePlate = :licensePlate
			  and r.status = :status
			  and r.startTime < :endTime
			  and r.endTime > :startTime
			""")
	long countOverlappingByLicensePlate(@Param("licensePlate") String licensePlate,
										@Param("status") ReservationStatus status,
										@Param("startTime") LocalDateTime startTime,
										@Param("endTime") LocalDateTime endTime
	);

	@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
	@Modifying
	@Query(value = """
			INSERT INTO license_plate_locks (license_plate) VALUES (:licensePlate)
			ON DUPLICATE KEY UPDATE license_plate = license_plate
			""", nativeQuery = true)
	void lockLicensePlate(@Param("licensePlate") String licensePlate);

}