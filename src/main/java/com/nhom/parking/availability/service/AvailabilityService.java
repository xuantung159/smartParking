package com.nhom.parking.availability.service;

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

    /**
     * Searches for available slots during [startTime, endTime). Does not hold a slot.
     * DISABLED and OCCUPIED slots do not accept new reservations, so only AVAILABLE
     * slots are considered, then slots with an ACTIVE reservation overlapping the time
     * range are excluded. Adjacent schedules are not considered overlapping.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse search(AvailabilityQuery query) {
        List<SlotResponse> candidates = slotService.findUsableByVehicleType(query.getVehicleType(), false);
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