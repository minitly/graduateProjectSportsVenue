package com.tly.mapper;

import com.tly.dto.report.BookingTrendRecord;
import com.tly.dto.report.DashboardStatResponse;
import com.tly.dto.report.VenueRankRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReportMapper {

    DashboardStatResponse selectDashboard(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    List<BookingTrendRecord> selectBookingTrend(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    List<VenueRankRecord> selectVenueRank(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("topN") long topN);
}

