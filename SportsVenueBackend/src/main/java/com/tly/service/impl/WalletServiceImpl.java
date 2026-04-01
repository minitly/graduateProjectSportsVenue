package com.tly.service.impl;

import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.wallet.WalletRechargeRequest;
import com.tly.dto.wallet.WalletRechargeResponse;
import com.tly.dto.wallet.WalletTransactionListItem;
import com.tly.entity.SysUser;
import com.tly.entity.WalletTransaction;
import com.tly.mapper.SysUserMapper;
import com.tly.mapper.WalletTransactionMapper;
import com.tly.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    private static final String TXN_RECHARGE = "RECHARGE";
    private static final String TXN_BOOKING_DEBIT = "BOOKING_DEBIT";
    private static final String TXN_BORROW_RENT_DEBIT = "BORROW_RENT_DEBIT";
    private static final String TXN_BORROW_DEPOSIT_DEBIT = "BORROW_DEPOSIT_DEBIT";
    private static final String TXN_REFUND = "REFUND";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<WalletRechargeResponse> recharge(WalletRechargeRequest request) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (request == null || request.getAmount() == null) {
            return Result.fail(400, "充值金额不能为空");
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "充值金额必须大于0");
        }

        SysUser user = sysUserMapper.findByIdForUpdate(currentUser.getUserId());
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        BigDecimal before = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal after = before.add(amount);

        int updated = sysUserMapper.updateBalanceById(user.getId(), after);
        if (updated <= 0) {
            return Result.fail(400, "充值失败，请稍后重试");
        }

        WalletTransaction txn = new WalletTransaction();
        LocalDateTime now = LocalDateTime.now();
        txn.setUserId(user.getId());
        txn.setTxnNo(genTxnNo());
        txn.setTxnType(TXN_RECHARGE);
        txn.setAmount(amount);
        txn.setBeforeBalance(before);
        txn.setAfterBalance(after);
        txn.setRemark(trimToNull(request.getRemark()));
        txn.setOperatorId(user.getId());
        txn.setCreateTime(now);
        walletTransactionMapper.insert(txn);

        WalletRechargeResponse resp = new WalletRechargeResponse();
        resp.setTransactionId(txn.getId());
        resp.setTransactionType(txn.getTxnType());
        resp.setAmount(txn.getAmount());
        resp.setBeforeBalance(txn.getBeforeBalance());
        resp.setAfterBalance(txn.getAfterBalance());
        resp.setCreateTime(txn.getCreateTime());
        return Result.success("充值成功", resp);
    }

    @Override
    public Result<PageResult<WalletTransactionListItem>> myTransactions(String type, LocalDate startDate, LocalDate endDate, long pageNo, long pageSize) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            return Result.fail(400, "endDate不能早于startDate");
        }
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        String typeFilter = normalizeType(type);
        if (typeFilter == null && StringUtils.hasText(type)) {
            return Result.fail(400, "不支持的流水类型");
        }

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        long total = walletTransactionMapper.countMyByCondition(currentUser.getUserId(), typeFilter, startTime, endTime);
        List<WalletTransaction> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = walletTransactionMapper.listMyByCondition(currentUser.getUserId(), typeFilter, startTime, endTime, offset, pageSize);
        }
        List<WalletTransactionListItem> items = records.stream().map(this::toListItem).collect(Collectors.toList());
        return Result.success("查询成功", new PageResult<>(total, pageNo, pageSize, items));
    }

    private static String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        String value = type.trim().toUpperCase(Locale.ROOT);
        if (TXN_RECHARGE.equals(value)
                || TXN_BOOKING_DEBIT.equals(value)
                || TXN_BORROW_RENT_DEBIT.equals(value)
                || TXN_BORROW_DEPOSIT_DEBIT.equals(value)
                || TXN_REFUND.equals(value)) {
            return value;
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String genTxnNo() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private WalletTransactionListItem toListItem(WalletTransaction txn) {
        WalletTransactionListItem item = new WalletTransactionListItem();
        item.setId(txn.getId());
        item.setType(txn.getTxnType());
        item.setAmount(txn.getAmount());
        item.setBeforeBalance(txn.getBeforeBalance());
        item.setAfterBalance(txn.getAfterBalance());
        item.setBizType(txn.getBizType());
        item.setBizId(txn.getBizId());
        item.setRemark(txn.getRemark());
        item.setCreateTime(txn.getCreateTime());
        return item;
    }
}
