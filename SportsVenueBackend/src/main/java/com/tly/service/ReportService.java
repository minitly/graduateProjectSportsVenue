package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.report.BookingTrendRecord;
import com.tly.dto.report.DashboardStatResponse;
import com.tly.dto.report.VenueRankRecord;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    Result<DashboardStatResponse> dashboard(LocalDate startDate, LocalDate endDate);

    Result<List<BookingTrendRecord>> bookingTrend(LocalDate startDate, LocalDate endDate);

    Result<List<VenueRankRecord>> venueRank(LocalDate startDate, LocalDate endDate, long topN);
}

