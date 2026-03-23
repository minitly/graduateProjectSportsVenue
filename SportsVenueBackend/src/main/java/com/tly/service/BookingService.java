package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.booking.BookingAllReservationRecord;
import com.tly.entity.BookingReservation;
import com.tly.entity.BookingReservationSlot;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    Result<List<BookingReservationSlot>> occupied(Long venueId, LocalDate startDate, LocalDate endDate);

    Result<BookingReservation> create(BookingReservation request);

    Result<BookingReservation> cancel(Long id, String remark);

    Result<PageResult<BookingReservation>> my(Long venueId, String status, LocalDate startDate, LocalDate endDate,
                                              long pageNo, long pageSize);

    Result<PageResult<BookingAllReservationRecord>> query(Long venueId, String username, String status, LocalDate startDate, LocalDate endDate,
                                                           long pageNo, long pageSize);

    Result<BookingReservation> verify(Long id);

    /**
     * 场地变更为不可用时联动取消申请中的预约（供场地模块调用）
     */
    void cancelAppliedByVenueStatusChange(Long venueId, String cancelReason, String cancelRemark);
}

