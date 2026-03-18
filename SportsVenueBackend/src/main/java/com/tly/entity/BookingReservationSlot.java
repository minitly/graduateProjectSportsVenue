package com.tly.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingReservationSlot {
    private Long id;
    private Long reservationId;
    private Long venueId;
    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;
    private LocalDateTime createTime;
}

