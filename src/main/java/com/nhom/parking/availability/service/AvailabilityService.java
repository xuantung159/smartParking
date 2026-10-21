package com.nhom.parking.availability.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom.parking.availability.dto.AvailabilityQuery;
import com.nhom.parking.availability.dto.AvailabilityResponse;
import com.nhom.parking.reservation.service.ReservationService;
import com.nhom.parking.slot.dto.SlotResponse;
import com.nhom.parking.slot.service.SlotService;

@Service
public class AvailabilityService {

    private final SlotService slotService;
    private final ReservationService reservationService;

    public AvailabilityService(SlotService slotService, ReservationService reservationService) {
        this.slotService = slotService;
        this.reservationService = reservationService;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse search(AvailabilityQuery query) {
        boolean startsInFuture = query.getStartTime().isAfter(LocalDateTime.now());

        List<SlotResponse> candidates = slotService.findUsableByVehicleType(query.getVehicleType(), startsInFuture);
        List<Long> candidateIds = candidates.stream().map(SlotResponse::id).toList();
        Set<Long> bookedIds = reservationService.findBookedSlotIds(candidateIds, query.getStartTime(),
                query.getEndTime());

        List<AvailabilityResponse.Slot> slots = candidates.stream()
                .filter(slot -> !bookedIds.contains(slot.id()))
                .map(slot -> new AvailabilityResponse.Slot(slot.id(), slot.code()))
                .toList();

        return AvailabilityResponse.of(query.getVehicleType(), query.getStartTime(), query.getEndTime(), slots);
    }

}