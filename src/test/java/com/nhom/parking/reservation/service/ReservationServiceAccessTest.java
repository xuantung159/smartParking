package com.nhom.parking.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.reservation.entity.Reservation;
import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.reservation.mapper.ReservationMapper;
import com.nhom.parking.reservation.repository.ReservationRepository;
import com.nhom.parking.slot.service.SlotService;

/** Ai được xem / huỷ một lịch đặt: chủ lịch, hoặc STAFF/ADMIN (staffAccess = true). */
@ExtendWith(MockitoExtension.class)
class ReservationServiceAccessTest {

    private static final Long OWNER = 3L;
    private static final Long OTHER_CUSTOMER = 9L;
    private static final Long STAFF = 2L;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SlotService slotService;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepository, new ReservationMapper(), slotService);
        lenient().when(slotService.findByIds(anyCollection())).thenReturn(List.of());
    }

    @Test
    void ownerAndStaffCanViewButOtherCustomerCannot() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation()));

        assertThat(service.get(OWNER, false, 1L).code()).isEqualTo("RSV-1");
        assertThat(service.get(STAFF, true, 1L).code()).isEqualTo("RSV-1");
        assertThatThrownBy(() -> service.get(OTHER_CUSTOMER, false, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void staffCanCancelSomeoneElsesReservation() {
        Reservation reservation = reservation();
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        service.cancel(STAFF, true, 1L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void otherCustomerCannotCancel() {
        Reservation reservation = reservation();
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancel(OTHER_CUSTOMER, false, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    private static Reservation reservation() {
        Reservation reservation = new Reservation();
        reservation.setCode("RSV-1");
        reservation.setUserId(OWNER);
        reservation.setSlotId(5L);
        reservation.setLicensePlate("59A-12345");
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        reservation.setStatus(ReservationStatus.ACTIVE);
        return reservation;
    }

}
