package com.tly.service.impl;

import com.tly.auth.UserContext;
import com.tly.common.Result;
import com.tly.dto.report.BookingTrendRecord;
import com.tly.dto.report.DashboardStatResponse;
import com.tly.dto.report.VenueRankRecord;
import com.tly.mapper.ReportMapper;
import com.tly.mapper.SysUserMapper;
import com.tly.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private static final String ROLE_OWNER = "OWNER";

    private static final String PERM_DASHBOARD = "REPORT_DASHBOARD";
    private static final String PERM_BOOKING_TREND = "REPORT_BOOKING_TREND";
    private static final String PERM_VENUE_RANK = "REPORT_VENUE_RANK";

    @Override
    public Result<DashboardStatResponse> dashboard(LocalDate startDate, LocalDate endDate) {
        Result<Void> permCheck = requireOwnerAndPermission(PERM_DASHBOARD);
        if (permCheck != null) {
            return Result.fail(permCheck.getCode(), permCheck.getMessage());
        }
        Result<Void> timeCheck = validateTimeRange(startDate, endDate);
        if (timeCheck != null) {
            return Result.fail(timeCheck.getCode(), timeCheck.getMessage());
        }
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        DashboardStatResponse data = reportMapper.selectDashboard(startTime, endTime);
        return Result.success("查询成功", data);
    }

    @Override
    public Result<List<BookingTrendRecord>> bookingTrend(LocalDate startDate, LocalDate endDate) {
        Result<Void> permCheck = requireOwnerAndPermission(PERM_BOOKING_TREND);
        if (permCheck != null) {
            return Result.fail(permCheck.getCode(), permCheck.getMessage());
        }
        Result<Void> timeCheck = validateTimeRange(startDate, endDate);
        if (timeCheck != null) {
            return Result.fail(timeCheck.getCode(), timeCheck.getMessage());
        }
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        List<BookingTrendRecord> list = reportMapper.selectBookingTrend(startTime, endTime);
        return Result.success("查询成功", list);
    }

    @Override
    public Result<List<VenueRankRecord>> venueRank(LocalDate startDate, LocalDate endDate, long topN) {
        Result<Void> permCheck = requireOwnerAndPermission(PERM_VENUE_RANK);
        if (permCheck != null) {
            return Result.fail(permCheck.getCode(), permCheck.getMessage());
        }
        if (topN <= 0) {
            topN = 10;
        }
        Result<Void> timeCheck = validateTimeRange(startDate, endDate);
        if (timeCheck != null) {
            return Result.fail(timeCheck.getCode(), timeCheck.getMessage());
        }
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        List<VenueRankRecord> list = reportMapper.selectVenueRank(startTime, endTime, topN);
        return Result.success("查询成功", list);
    }

    private Result<Void> validateTimeRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return Result.fail(400, "startDate不能为空");
        }
        if (endDate == null) {
            return Result.fail(400, "endDate不能为空");
        }
        if (startDate.isAfter(endDate)) {
            return Result.fail(400, "startDate 不能晚于 endDate");
        }
        return null;
    }

    private Result<Void> requireOwnerAndPermission(String permissionCode) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (!ROLE_OWNER.equalsIgnoreCase(currentUser.getRole())) {
            return Result.fail(403, "无权限访问报表");
        }
        if (!StringUtils.hasText(permissionCode)) {
            return null;
        }
        List<String> perms = sysUserMapper.findPermissionCodesByRole(currentUser.getRole());
        if (perms == null || !perms.contains(permissionCode)) {
            return Result.fail(403, "无权限访问该报表接口");
        }
        return null;
    }
}

