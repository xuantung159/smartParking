package com.nhom.parking.slot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nhom.parking.common.enums.VehicleType;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.reservation.entity.ReservationStatus;
import com.nhom.parking.reservation.repository.ReservationRepository;
import com.nhom.parking.slot.dto.CreateSlotRequest;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.entity.ParkingSlot;
import com.nhom.parking.slot.entity.SlotStatus;
import com.nhom.parking.slot.mapper.SlotMapper;
import com.nhom.parking.slot.repository.ParkingSlotRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    private ParkingSlotRepository slotRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EntityManager entityManager;

    private SlotService service;

    @BeforeEach
    void setUp() {
        service = new SlotService(slotRepository, new SlotMapper(), reservationRepository, entityManager);
    }

    @Test
    void createNormalizesCodeAndStartsAvailable() {
        when(slotRepository.existsByCode("A-01")).thenReturn(false);

        SlotResponse response = service.create(new CreateSlotRequest(" a-01 ", VehicleType.CAR));

        assertThat(response.code()).isEqualTo("A-01");
        assertThat(response.vehicleType()).isEqualTo(VehicleType.CAR);
        assertThat(response.status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(slotRepository.existsByCode("M-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateSlotRequest("m-01", VehicleType.MOTORBIKE)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_CODE_EXISTED);
        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void disableAvailableSlotWithoutReservations() {
        ParkingSlot slot = slot(SlotStatus.AVAILABLE);
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(reservationRepository.findSlotIdsWithReservationEndingAfter(anyCollection(), eq(ReservationStatus.ACTIVE), any()))
                .thenReturn(List.of());

        service.disable(1L);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.DISABLED);
        verify(slotRepository).save(slot);
    }

    @Test
    void disableRejectsOccupiedSlot() {
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(SlotStatus.OCCUPIED)));

        assertThatThrownBy(() -> service.disable(1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_UNAVAILABLE);
    }

    @Test
    void disableRejectsSlotWithUpcomingReservation() {
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(SlotStatus.AVAILABLE)));
        when(reservationRepository.findSlotIdsWithReservationEndingAfter(anyCollection(), eq(ReservationStatus.ACTIVE), any()))
                .thenReturn(List.of(1L));

        assertThatThrownBy(() -> service.disable(1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_UNAVAILABLE);
        verify(slotRepository, never()).save(any());
    }

    @Test
    void disableIsNoOpForDisabledSlotAndFailsForMissingSlot() {
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(SlotStatus.DISABLED)));
        when(slotRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        service.disable(1L);
        verify(slotRepository, never()).save(any());

        assertThatThrownBy(() -> service.disable(99L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_NOT_FOUND);
    }

    private static ParkingSlot slot(SlotStatus status) {
        ParkingSlot slot = new ParkingSlot();
        slot.setCode("M-01");
        slot.setVehicleType(VehicleType.MOTORBIKE);
        slot.setStatus(status);
        return slot;
    }

}
