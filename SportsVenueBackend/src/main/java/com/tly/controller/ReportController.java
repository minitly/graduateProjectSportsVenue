package com.tly.controller;

import com.tly.common.Result;
import com.tly.dto.report.BookingTrendRecord;
import com.tly.dto.report.DashboardStatResponse;
import com.tly.dto.report.VenueRankRecord;
import com.tly.service.ReportService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 运营总览统计：GET /sportsVenue/reports/dashboard
     */
    @GetMapping("/dashboard")
    public Result<DashboardStatResponse> dashboard(
            @RequestParam("startDate") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.dashboard(startDate, endDate);
    }

    /**
     * 预约趋势统计（按天）：GET /sportsVenue/reports/bookings/trend
     */
    @GetMapping("/bookings/trend")
    public Result<List<BookingTrendRecord>> bookingTrend(
            @RequestParam("startDate") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.bookingTrend(startDate, endDate);
    }

    /**
     * 场地热度排行（按预约次数）：GET /sportsVenue/reports/bookings/venue-rank
     */
    @GetMapping("/bookings/venue-rank")
    public Result<List<VenueRankRecord>> venueRank(
            @RequestParam("startDate") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "topN", required = false, defaultValue = "10") long topN) {
        return reportService.venueRank(startDate, endDate, topN);
    }
}

