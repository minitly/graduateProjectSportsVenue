package com.tly.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingReservation {
    private Long id;
    private Long userId;
    private Long venueId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /**
     * APPLIED / CANCELED / VERIFIED / VIOLATION
     */
    private String status;

    private LocalDateTime cancelTime;
    /**
     * USER_CANCEL / ADMIN_CANCEL / VENUE_DISABLED / VENUE_MAINTAIN / VENUE_SUSPEND
     */
    private String cancelReason;
    private String cancelRemark;

    private LocalDateTime verifyTime;

    private LocalDateTime violationTime;
    /**
     * CANCEL_LATE / NO_SHOW
     */
    private String violationType;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

