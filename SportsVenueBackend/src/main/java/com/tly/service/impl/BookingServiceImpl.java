package com.tly.service.impl;

import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.booking.BookingAllReservationRecord;
import com.tly.entity.BookingReservation;
import com.tly.entity.BookingReservationSlot;
import com.tly.entity.SysUser;
import com.tly.entity.Venue;
import com.tly.entity.WalletTransaction;
import com.tly.mapper.BookingReservationMapper;
import com.tly.mapper.BookingReservationSlotMapper;
import com.tly.mapper.SysUserMapper;
import com.tly.mapper.VenueMapper;
import com.tly.mapper.WalletTransactionMapper;
import com.tly.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingServiceImpl implements BookingService {

    private static final int SLOT_MINUTES = 60;
    private static final int BOOKING_DAYS = 7;

    @Autowired
    private BookingReservationMapper reservationMapper;

    @Autowired
    private BookingReservationSlotMapper slotMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Override
    public Result<List<BookingReservationSlot>> occupied(Long venueId, LocalDate startDate, LocalDate endDate) {
        if (venueId == null) {
            return Result.fail(400, "venueId不能为空");
        }
        if (startDate == null || endDate == null) {
            return Result.fail(400, "startDate和endDate不能为空");
        }
        if (endDate.isBefore(startDate)) {
            return Result.fail(400, "endDate不能早于startDate");
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<BookingReservationSlot> slots = slotMapper.listOccupied(venueId, start, end);
        return Result.success("查询成功", slots);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BookingReservation> create(BookingReservation request) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }

        if (request == null || request.getVenueId() == null || request.getStartTime() == null || request.getEndTime() == null) {
            return Result.fail(400, "venueId、startTime、endTime为必填");
        }

        SysUser user = sysUserMapper.findById(currentUser.getUserId());
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }

        if (user.getBookingBannedUntil() != null && user.getBookingBannedUntil().isAfter(LocalDateTime.now())) {
            return Result.fail(403, "本月已被取消预约资格，请下月再试");
        }

        Venue venue = venueMapper.selectById(request.getVenueId());
        if (venue == null) {
            return Result.fail(404, "场地不存在");
        }
        if (!"AVAILABLE".equalsIgnoreCase(venue.getStatus())) {
            return Result.fail(400, "场地当前不可预约");
        }

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        Result<Void> validateTime = validateBookingTime(venue, startTime, endTime);
        if (validateTime.getCode() != 200) {
            return Result.fail(validateTime.getCode(), validateTime.getMessage());
        }

        BigDecimal fee = calculateBookingFee(venue, startTime, endTime);
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            SysUser locked = sysUserMapper.findByIdForUpdate(currentUser.getUserId());
            if (locked == null) {
                return Result.fail(401, "用户不存在");
            }
            BigDecimal balance = locked.getBalance() == null ? BigDecimal.ZERO : locked.getBalance();
            if (balance.compareTo(fee) < 0) {
                return Result.fail(400, "余额不足，无法预约");
            }
        }

        BookingReservation toSave = new BookingReservation();
        toSave.setUserId(currentUser.getUserId());
        toSave.setVenueId(request.getVenueId());
        toSave.setStartTime(startTime);
        toSave.setEndTime(endTime);
        toSave.setStatus("APPLIED");
        reservationMapper.insert(toSave);

        try {
            createSlotsForReservation(toSave.getId(), toSave.getVenueId(), startTime, endTime);
        } catch (DuplicateKeyException e) {
            // 并发下唯一约束冲突：回滚本次预约创建
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(409, "该时段已被预约，请选择其他时段");
        }

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Result<Void> charge = applyBookingCharge(currentUser.getUserId(), toSave.getId(), fee);
            if (charge.getCode() != 200) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.fail(charge.getCode(), charge.getMessage());
            }
        }

        BookingReservation db = reservationMapper.selectById(toSave.getId());
        return Result.success("预约申请成功", db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BookingReservation> cancel(Long id, String remark) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (id == null) {
            return Result.fail(400, "id不能为空");
        }

        BookingReservation exists = reservationMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "预约记录不存在");
        }
        if (!currentUser.getUserId().equals(exists.getUserId())) {
            return Result.fail(403, "无权取消他人的预约");
        }
        if (!"APPLIED".equalsIgnoreCase(exists.getStatus())) {
            return Result.fail(400, "仅申请中的预约允许取消");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lateDeadline = exists.getStartTime().minusHours(2);

        if (now.isAfter(lateDeadline)) {
            int updated = reservationMapper.updateStatusToViolationByCancel(id, now, "USER_CANCEL", remark, now, "CANCEL_LATE");
            if (updated <= 0) {
                return Result.fail(400, "取消失败，请刷新后重试");
            }
            slotMapper.deleteByReservationId(id);
            applyViolationForUser(exists.getUserId(), now);
            refundBookingPayment(id, "预约取消退款");
            BookingReservation db = reservationMapper.selectById(id);
            return Result.success("取消成功", db);
        } else {
            int updated = reservationMapper.updateStatusToCanceled(id, now, "USER_CANCEL", remark);
            if (updated <= 0) {
                return Result.fail(400, "取消失败，请刷新后重试");
            }
            slotMapper.deleteByReservationId(id);
            refundBookingPayment(id, "预约取消退款");
            BookingReservation db = reservationMapper.selectById(id);
            return Result.success("取消成功", db);
        }
    }

    @Override
    public Result<PageResult<BookingReservation>> my(String venueName, String status, LocalDate startDate, LocalDate endDate,
                                                    long pageNo, long pageSize) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        long total = reservationMapper.countMyByCondition(currentUser.getUserId(), venueName, status, startTime, endTime);
        List<BookingReservation> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = reservationMapper.listMyByCondition(currentUser.getUserId(), venueName, status, startTime, endTime, offset, pageSize);
        }
        PageResult<BookingReservation> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    @Override
    public Result<PageResult<BookingAllReservationRecord>> query(String venueName, String username, String status, LocalDate startDate, LocalDate endDate,
                                                                     long pageNo, long pageSize) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (!"OWNER".equalsIgnoreCase(currentUser.getRole()) && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return Result.fail(403, "无权限查询全部预约记录");
        }
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        long total = reservationMapper.countAllByCondition(venueName, status, startTime, endTime, username);
        List<BookingAllReservationRecord> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = reservationMapper.listAllByCondition(venueName, status, startTime, endTime, username, offset, pageSize);
        }
        PageResult<BookingAllReservationRecord> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BookingReservation> verify(Long id) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (!"OWNER".equalsIgnoreCase(currentUser.getRole()) && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return Result.fail(403, "无权限核销");
        }
        if (id == null) {
            return Result.fail(400, "id不能为空");
        }
        BookingReservation exists = reservationMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "预约记录不存在");
        }
        if (!"APPLIED".equalsIgnoreCase(exists.getStatus())) {
            return Result.fail(400, "仅申请中的预约允许核销");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(exists.getStartTime().plusMinutes(SLOT_MINUTES))) {
            return Result.fail(400, "已超时未核销，无法核销");
        }

        int updated = reservationMapper.updateStatusToVerified(id, now);
        if (updated <= 0) {
            return Result.fail(400, "核销失败，请刷新后重试");
        }
        BookingReservation db = reservationMapper.selectById(id);
        return Result.success("核销成功", db);
    }

    @Override
    public Result<Map<String, Object>> myViolationStatus() {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        SysUser user = sysUserMapper.findById(currentUser.getUserId());
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        LocalDateTime bannedUntil = user.getBookingBannedUntil();
        boolean isViolationUser = bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now());

        Map<String, Object> data = new HashMap<>();
        data.put("isViolationUser", isViolationUser);
        data.put("violationCountMonth", user.getViolationCountMonth() == null ? 0 : user.getViolationCountMonth());
        data.put("violationMonth", user.getViolationMonth());
        data.put("bookingBannedUntil", bannedUntil);
        return Result.success("查询成功", data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppliedByVenueStatusChange(Long venueId, String cancelReason, String cancelRemark) {
        if (venueId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = reservationMapper.listAppliedIdsByVenueId(venueId);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long reservationId : ids) {
            refundBookingPayment(reservationId, "预约取消退款");
        }
        reservationMapper.cancelAppliedByVenue(venueId, now, cancelReason, cancelRemark);
        slotMapper.deleteByReservationIds(ids);
    }

    private Result<Void> validateBookingTime(Venue venue, LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            return Result.fail(400, "endTime必须大于startTime");
        }
        if (startTime.getMinute() != 0 || startTime.getSecond() != 0 || startTime.getNano() != 0
                || endTime.getMinute() != 0 || endTime.getSecond() != 0 || endTime.getNano() != 0) {
            return Result.fail(400, "startTime和endTime必须为整点");
        }
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            return Result.fail(400, "不允许跨日预约");
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0 || minutes % SLOT_MINUTES != 0) {
            return Result.fail(400, "预约时长必须为60分钟的倍数");
        }

        LocalDate today = LocalDate.now();
        LocalDate date = startTime.toLocalDate();
        if (date.isBefore(today) || date.isAfter(today.plusDays(BOOKING_DAYS - 1))) {
            return Result.fail(400, "仅支持预约未来7天内的时段");
        }

        if (StringUtils.hasText(venue.getOpenTime()) && StringUtils.hasText(venue.getCloseTime())) {
            LocalTime open = LocalTime.parse(venue.getOpenTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime close = LocalTime.parse(venue.getCloseTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime start = startTime.toLocalTime();
            LocalTime end = endTime.toLocalTime();
            if (start.isBefore(open) || end.isAfter(close)) {
                return Result.fail(400, "预约时间不在场地开放时间范围内");
            }
        }

        return Result.success(null);
    }

    private static BigDecimal calculateBookingFee(Venue venue, LocalDateTime startTime, LocalDateTime endTime) {
        if (venue.getPrice() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return venue.getPrice().multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private Result<Void> applyBookingCharge(Long userId, Long reservationId, BigDecimal fee) {
        SysUser user = sysUserMapper.findByIdForUpdate(userId);
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }
        BigDecimal before = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        if (before.compareTo(fee) < 0) {
            return Result.fail(400, "余额不足，无法预约");
        }
        BigDecimal after = before.subtract(fee);
        sysUserMapper.updateBalanceById(userId, after);

        LocalDateTime txnTime = LocalDateTime.now();
        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setTxnNo(genTxnNo());
        txn.setTxnType("BOOKING_DEBIT");
        txn.setBizType("BOOKING");
        txn.setBizId(reservationId);
        txn.setAmount(fee.negate());
        txn.setBeforeBalance(before);
        txn.setAfterBalance(after);
        txn.setRemark("场地预约扣费");
        txn.setOperatorId(null);
        txn.setCreateTime(txnTime);
        walletTransactionMapper.insert(txn);
        return Result.success(null);
    }

    /**
     * 退还该预约对应的预约扣费（幂等：同一预约仅退一次）
     */
    private void refundBookingPayment(Long reservationId, String remark) {
        if (reservationId == null) {
            return;
        }
        if (walletTransactionMapper.countBookingRefundByReservationId(reservationId) > 0) {
            return;
        }
        WalletTransaction debit = walletTransactionMapper.findBookingDebitByReservationId(reservationId);
        if (debit == null || debit.getAmount() == null) {
            return;
        }
        BigDecimal refundAmt = debit.getAmount().abs();
        if (refundAmt.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Long userId = debit.getUserId();
        SysUser user = sysUserMapper.findByIdForUpdate(userId);
        if (user == null) {
            return;
        }
        BigDecimal before = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal after = before.add(refundAmt);
        sysUserMapper.updateBalanceById(userId, after);

        LocalDateTime txnTime = LocalDateTime.now();
        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setTxnNo(genTxnNo());
        txn.setTxnType("REFUND");
        txn.setBizType("BOOKING");
        txn.setBizId(reservationId);
        txn.setAmount(refundAmt);
        txn.setBeforeBalance(before);
        txn.setAfterBalance(after);
        txn.setRemark(remark);
        txn.setOperatorId(null);
        txn.setCreateTime(txnTime);
        walletTransactionMapper.insert(txn);
    }

    private static String genTxnNo() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private void createSlotsForReservation(Long reservationId, Long venueId, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime cur = startTime;
        while (cur.isBefore(endTime)) {
            BookingReservationSlot slot = new BookingReservationSlot();
            slot.setReservationId(reservationId);
            slot.setVenueId(venueId);
            slot.setSlotStartTime(cur);
            slot.setSlotEndTime(cur.plusMinutes(SLOT_MINUTES));
            slotMapper.insert(slot);
            cur = cur.plusMinutes(SLOT_MINUTES);
        }
    }

    private void applyViolationForUser(Long userId, LocalDateTime now) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            return;
        }
        String ym = now.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Integer cnt = user.getViolationCountMonth();
        String month = user.getViolationMonth();
        if (!ym.equals(month)) {
            cnt = 0;
            month = ym;
        }
        cnt = (cnt == null ? 0 : cnt) + 1;

        LocalDateTime bannedUntil = user.getBookingBannedUntil();
        if (cnt >= 3) {
            LocalDate firstDayNextMonth = now.toLocalDate().withDayOfMonth(1).plusMonths(1);
            bannedUntil = firstDayNextMonth.atStartOfDay();
        }
        sysUserMapper.updateBookingCredit(userId, cnt, month, bannedUntil);
    }

    @Transactional(rollbackFor = Exception.class)
    public void processNoShowViolations(int batchSize) {
        if (batchSize <= 0) {
            batchSize = 2000;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(SLOT_MINUTES);
        List<Long> ids = reservationMapper.listAppliedIdsToNoShow(deadline, batchSize);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            BookingReservation r = reservationMapper.selectById(id);
            if (r == null) {
                continue;
            }
            int updated = reservationMapper.updateStatusToViolationByNoShow(id, now, "NO_SHOW");
            if (updated > 0) {
                slotMapper.deleteByReservationId(id);
                applyViolationForUser(r.getUserId(), now);
            }
        }
    }
}

