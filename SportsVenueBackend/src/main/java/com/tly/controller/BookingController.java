package com.tly.controller;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.booking.BookingCancelRequest;
import com.tly.dto.booking.BookingAllReservationRecord;
import com.tly.entity.BookingReservation;
import com.tly.entity.BookingReservationSlot;
import com.tly.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 预约管理模块接口
 */
@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * 查询场地已被预约（占用）的时段：GET /sportsVenue/bookings/occupied
     */
    @GetMapping("/occupied")
    public Result<List<BookingReservationSlot>> occupied(@RequestParam("venueId") Long venueId,
                                                         @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return bookingService.occupied(venueId, startDate, endDate);
    }

    /**
     * 用户创建预约：POST /sportsVenue/bookings
     */
    @PostMapping
    public Result<BookingReservation> create(@RequestBody BookingReservation request) {
        return bookingService.create(request);
    }

    /**
     * 用户取消预约：PUT /sportsVenue/bookings/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public Result<BookingReservation> cancel(@PathVariable("id") Long id,
                                             @RequestBody(required = false) BookingCancelRequest body) {
        String remark = body != null ? body.getRemark() : null;
        return bookingService.cancel(id, remark);
    }

    /**
     * 查询我的预约（分页 + 条件查询）：GET /sportsVenue/bookings/my
     */
    @GetMapping("/my")
    public Result<PageResult<BookingReservation>> my(@RequestParam(value = "venueName", required = false) String venueName,
                                                     @RequestParam(value = "status", required = false) String status,
                                                     @RequestParam(value = "startDate", required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                     @RequestParam(value = "endDate", required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                     @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                                     @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return bookingService.my(venueName, status, startDate, endDate, pageNo, pageSize);
    }

    /**
     * OWNER 查询全部预约记录（分页 + 条件查询）：GET /sportsVenue/bookings
     */
    @GetMapping
    public Result<PageResult<BookingAllReservationRecord>> query(@RequestParam(value = "venueName", required = false) String venueName,
                                                                    @RequestParam(value = "username", required = false) String username,
                                                                    @RequestParam(value = "status", required = false) String status,
                                                                    @RequestParam(value = "startDate", required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                    @RequestParam(value = "endDate", required = false)
                                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                    @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                                                    @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return bookingService.query(venueName, username, status, startDate, endDate, pageNo, pageSize);
    }

    /**
     * OWNER 核销预约：PUT /sportsVenue/bookings/{id}/verify
     */
    @PutMapping("/{id}/verify")
    public Result<BookingReservation> verify(@PathVariable("id") Long id) {
        return bookingService.verify(id);
    }

    /**
     * 查询当前用户是否违规：GET /sportsVenue/bookings/my/violation-status
     */
    @GetMapping("/my/violation-status")
    public Result<Map<String, Object>> myViolationStatus() {
        return bookingService.myViolationStatus();
    }
}

