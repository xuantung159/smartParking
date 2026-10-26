package com.nhom.parking.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.reservation.entity.Reservation;
import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.reservation.repository.ReservationRepository;
import com.nhom.parking.session.dto.CheckInRequest;
import com.nhom.parking.session.entity.ParkingSession;
import com.nhom.parking.session.entity.SessionStatus;
import com.nhom.parking.session.mapper.SessionMapper;
import com.nhom.parking.session.repository.ParkingSessionRepository;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.entity.SlotStatus;
import com.nhom.parking.slot.service.SlotService;

@ExtendWith(MockitoExtension.class)
class ParkingSessionServiceTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SlotService slotService;

    private ParkingSessionService service;

    @BeforeEach
    void setUp() {
        service = new ParkingSessionService(sessionRepository, reservationRepository, slotService, new SessionMapper());
    }

    @Test
    void checkInAssignsAvailableSlotAndNormalizesLicensePlate() {
        CheckInRequest request = checkInRequest("  59a-12345 ", VehicleType.MOTORBIKE, null);
        when(sessionRepository.existsByLicensePlateIgnoreCaseAndStatus("59A-12345", SessionStatus.ACTIVE))
                .thenReturn(false);
        when(slotService.findUsableByVehicleType(VehicleType.MOTORBIKE, false))
                .thenReturn(List.of(new SlotResponse(3L, "M-03", VehicleType.MOTORBIKE, SlotStatus.AVAILABLE)));
        when(sessionRepository.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.checkIn(request);

        ArgumentCaptor<ParkingSession> sessionCaptor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(result.licensePlate()).isEqualTo("59A-12345");
        assertThat(sessionCaptor.getValue().getSlotId()).isEqualTo(3L);
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(sessionCaptor.getValue().getCheckInTime()).isNotNull();
        verify(slotService).markOccupied(3L);
    }

    @Test
    void checkInRejectsVehicleTypeThatDoesNotMatchReservedSlot() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setLicensePlate("59A-12345");
        reservation.setSlotId(7L);
        reservation.setStartTime(LocalDateTime.now().minusMinutes(1));
        reservation.setEndTime(LocalDateTime.now().plusHours(1));

        when(sessionRepository.existsByLicensePlateIgnoreCaseAndStatus("59A-12345", SessionStatus.ACTIVE))
                .thenReturn(false);
        when(reservationRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(reservation));
        when(slotService.get(7L))
                .thenReturn(new SlotResponse(7L, "C-01", VehicleType.CAR, SlotStatus.AVAILABLE));

        assertThatThrownBy(() -> service.checkIn(checkInRequest("59A-12345", VehicleType.MOTORBIKE, 12L)))
                .isInstanceOf(AppException.class);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        verify(slotService, never()).markOccupied(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void checkInRejectsAnAlreadyParkedLicensePlate() {
        when(sessionRepository.existsByLicensePlateIgnoreCaseAndStatus("59A-12345", SessionStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.checkIn(
                checkInRequest(" 59a-12345 ", VehicleType.MOTORBIKE, null)))
                .isInstanceOf(AppException.class);

        verify(slotService, never()).findUsableByVehicleType(VehicleType.MOTORBIKE, false);
    }

    @Test
    void checkOutRoundsPartialHoursUpAndReleasesSlot() {
        ParkingSession session = new ParkingSession();
        session.setSlotId(3L);
        session.setVehicleType(VehicleType.MOTORBIKE);
        session.setCheckInTime(LocalDateTime.now().minusMinutes(61));
        session.setStatus(SessionStatus.ACTIVE);
        when(sessionRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(session));

        var result = service.checkOut(21L);

        assertThat(result.fee()).isEqualTo(10_000L);
        assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED.name());
        assertThat(session.getCheckOutTime()).isNotNull();
        verify(slotService).markAvailable(3L);
    }

    private CheckInRequest checkInRequest(String licensePlate, VehicleType vehicleType, Long reservationId) {
        CheckInRequest request = new CheckInRequest();
        request.setLicensePlate(licensePlate);
        request.setVehicleType(vehicleType);
        request.setReservationId(reservationId);
        return request;
    }
}
