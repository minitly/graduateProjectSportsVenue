package com.tly.dto.booking;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * OWNER 查询全部预约记录：不返回 userId，改为返回 userName（用户名），便于前端展示。
 */
@Data
public class BookingAllReservationRecord {
    private Long id;
    private String userName;
    private String venueName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    private LocalDateTime cancelTime;
    private String cancelReason;
    private String cancelRemark;

    private LocalDateTime verifyTime;
    private LocalDateTime violationTime;
    private String violationType;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

