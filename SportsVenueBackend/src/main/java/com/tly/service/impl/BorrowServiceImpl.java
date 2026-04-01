package com.tly.service.impl;

import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.borrow.BorrowRecordListItem;
import com.tly.entity.BorrowRecord;
import com.tly.entity.SysUser;
import com.tly.entity.WarehouseItem;
import com.tly.entity.WalletTransaction;
import com.tly.mapper.BorrowRecordMapper;
import com.tly.mapper.SysUserMapper;
import com.tly.mapper.WalletTransactionMapper;
import com.tly.mapper.WarehouseItemMapper;
import com.tly.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private WarehouseItemMapper warehouseItemMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Override
    public Result<BorrowRecord> apply(BorrowRecord record) {
        if (record.getItemId() == null) {
            return Result.fail(400, "器材ID不能为空");
        }
        if (record.getQuantity() == null || record.getQuantity() <= 0) {
            return Result.fail(400, "借用数量必须大于0");
        }

        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录，无法发起借用申请");
        }
        record.setUserId(currentUser.getUserId());

        WarehouseItem item = warehouseItemMapper.selectById(record.getItemId());
        if (item == null) {
            return Result.fail(404, "器材不存在");
        }
        if (item.getAvailableQuantity() == null || item.getAvailableQuantity() < record.getQuantity()) {
            return Result.fail(400, "可借数量不足");
        }

        record.setStatus("REQUESTED");
        record.setRequestedTime(LocalDateTime.now());
        // 确认借出扣费后再写入 deposit_snapshot
        record.setDepositSnapshot(null);

        record.setConditionOnBorrow(null);
        record.setConditionOnReturn(null);
        record.setDamagedLostCount(null);

        borrowRecordMapper.insert(record);
        BorrowRecord dbRecord = borrowRecordMapper.selectById(record.getId());
        return Result.success("借用申请已提交", dbRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BorrowRecord> approve(Long id, String conditionOnBorrow, String remark) {
        BorrowRecord exists = borrowRecordMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "借用记录不存在");
        }
        if (!"REQUESTED".equalsIgnoreCase(exists.getStatus())) {
            return Result.fail(400, "当前状态不支持确认借出");
        }

        WarehouseItem item = warehouseItemMapper.selectById(exists.getItemId());
        if (item == null) {
            return Result.fail(404, "器材不存在");
        }
        if (item.getAvailableQuantity() == null || item.getAvailableQuantity() < exists.getQuantity()) {
            return Result.fail(400, "可借数量不足，无法确认借出");
        }

        if (!StringUtils.hasText(conditionOnBorrow)) {
            conditionOnBorrow = "GOOD";
        }

        Long borrowerId = exists.getUserId();
        if (borrowerId == null) {
            return Result.fail(400, "借用记录缺少用户");
        }

        int qty = exists.getQuantity();
        BigDecimal depositUnit = item.getDepositAmount() != null ? item.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal borrowUnit = item.getBorrowAmount() != null ? item.getBorrowAmount() : BigDecimal.ZERO;
        BigDecimal rent = borrowUnit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pledge = depositUnit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = rent.add(pledge);

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            SysUser borrower = sysUserMapper.findByIdForUpdate(borrowerId);
            if (borrower == null) {
                return Result.fail(400, "借用人用户不存在");
            }
            BigDecimal bal = borrower.getBalance() == null ? BigDecimal.ZERO : borrower.getBalance();
            if (bal.compareTo(total) < 0) {
                return Result.fail(400, "用户余额不足，无法确认借出");
            }
        }

        Long borrowId = exists.getId();
        Result<Void> rentDebit = debitBorrowWallet(borrowerId, borrowId, rent, "BORROW_RENT_DEBIT", "器材借用租金");
        if (rentDebit.getCode() != 200) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(rentDebit.getCode(), rentDebit.getMessage());
        }
        Result<Void> pledgeDebit = debitBorrowWallet(borrowerId, borrowId, pledge, "BORROW_DEPOSIT_DEBIT", "器材借用押金");
        if (pledgeDebit.getCode() != 200) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(pledgeDebit.getCode(), pledgeDebit.getMessage());
        }

        exists.setDepositSnapshot(total);
        exists.setStatus("USING");
        exists.setApprovedTime(LocalDateTime.now());
        exists.setConditionOnBorrow(conditionOnBorrow);
        if (remark != null) {
            exists.setRemark(remark);
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - exists.getQuantity());
        warehouseItemMapper.update(item);
        borrowRecordMapper.update(exists);

        BorrowRecord dbRecord = borrowRecordMapper.selectById(id);
        return Result.success("确认借出成功", dbRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BorrowRecord> confirmReturn(Long id, String conditionOnReturn, String remark, Integer damagedLostCount) {
        BorrowRecord exists = borrowRecordMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "借用记录不存在");
        }
        if (!"USING".equalsIgnoreCase(exists.getStatus())) {
            return Result.fail(400, "当前状态不支持确认归还");
        }

        WarehouseItem item = warehouseItemMapper.selectById(exists.getItemId());
        if (item == null) {
            return Result.fail(404, "器材不存在");
        }

        if (!StringUtils.hasText(conditionOnReturn)) {
            conditionOnReturn = "GOOD";
        }

        int quantity = exists.getQuantity() == null ? 0 : exists.getQuantity();
        if (quantity <= 0) {
            return Result.fail(400, "借用数量异常");
        }

        if ("GOOD".equalsIgnoreCase(conditionOnReturn)) {
            if (damagedLostCount != null && damagedLostCount > 0) {
                return Result.fail(400, "完好归还时不应填写损坏/丢失个数");
            }
            exists.setDamagedLostCount(null);
        } else if ("DAMAGED".equalsIgnoreCase(conditionOnReturn) || "LOST".equalsIgnoreCase(conditionOnReturn)) {
            int lost = damagedLostCount == null ? quantity : damagedLostCount;
            if (lost < 1 || lost > quantity) {
                return Result.fail(400, "损坏/丢失个数须在1到借用数量之间");
            }
            exists.setDamagedLostCount(lost);
        } else {
            return Result.fail(400, "归还状况不合法");
        }

        exists.setStatus("RETURNED");
        exists.setReturnedTime(LocalDateTime.now());
        exists.setConditionOnReturn(conditionOnReturn);
        if (remark != null) {
            exists.setRemark(remark);
        }

        int availableQuantity = item.getAvailableQuantity() == null ? 0 : item.getAvailableQuantity();
        int damagedQuantity = item.getDamagedQuantity() == null ? 0 : item.getDamagedQuantity();
        if ("GOOD".equalsIgnoreCase(conditionOnReturn)) {
            item.setAvailableQuantity(availableQuantity + quantity);
        } else {
            int lost = exists.getDamagedLostCount() == null ? quantity : exists.getDamagedLostCount();
            int backGood = quantity - lost;
            item.setAvailableQuantity(availableQuantity + backGood);
            item.setDamagedQuantity(damagedQuantity + lost);
        }

        warehouseItemMapper.update(item);
        borrowRecordMapper.update(exists);

        refundBorrowPledge(exists);

        BorrowRecord dbRecord = borrowRecordMapper.selectById(id);
        return Result.success("确认归还成功", dbRecord);
    }

    /**
     * 退还押金：租金不退；以实际「押金扣款」流水为准。GOOD 全退押金；DAMAGED/LOST 按 (quantity-lost)/quantity 比例退押金。
     */
    private void refundBorrowPledge(BorrowRecord br) {
        if (br.getId() == null) {
            return;
        }
        if (walletTransactionMapper.countBorrowRefundByBorrowId(br.getId()) > 0) {
            return;
        }
        WalletTransaction depDebit = walletTransactionMapper.findBorrowDepositDebitByBorrowId(br.getId());
        if (depDebit == null || depDebit.getAmount() == null) {
            return;
        }
        BigDecimal pledgeCharged = depDebit.getAmount().abs();
        if (pledgeCharged.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int qty = br.getQuantity() == null ? 0 : br.getQuantity();
        if (qty <= 0) {
            return;
        }

        String cor = br.getConditionOnReturn();
        BigDecimal refundAmt;
        if ("GOOD".equalsIgnoreCase(cor)) {
            refundAmt = pledgeCharged;
        } else {
            int lost = br.getDamagedLostCount() == null ? qty : br.getDamagedLostCount();
            refundAmt = pledgeCharged
                    .multiply(BigDecimal.valueOf(qty - lost))
                    .divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
        }
        if (refundAmt.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Long userId = br.getUserId();
        SysUser user = sysUserMapper.findByIdForUpdate(userId);
        if (user == null) {
            return;
        }
        BigDecimal before = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal after = before.add(refundAmt);
        sysUserMapper.updateBalanceById(userId, after);

        LocalDateTime now = LocalDateTime.now();
        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setTxnNo(genTxnNo());
        txn.setTxnType("REFUND");
        txn.setBizType("BORROW");
        txn.setBizId(br.getId());
        txn.setAmount(refundAmt);
        txn.setBeforeBalance(before);
        txn.setAfterBalance(after);
        txn.setRemark("借用归还退押金");
        txn.setOperatorId(null);
        txn.setCreateTime(now);
        walletTransactionMapper.insert(txn);
    }

    private Result<Void> debitBorrowWallet(Long userId, Long borrowId, BigDecimal amount, String txnType, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.success(null);
        }
        SysUser user = sysUserMapper.findByIdForUpdate(userId);
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }
        BigDecimal before = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        if (before.compareTo(amount) < 0) {
            return Result.fail(400, "余额不足，无法借用");
        }
        BigDecimal after = before.subtract(amount);
        sysUserMapper.updateBalanceById(userId, after);

        LocalDateTime now = LocalDateTime.now();
        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setTxnNo(genTxnNo());
        txn.setTxnType(txnType);
        txn.setBizType("BORROW");
        txn.setBizId(borrowId);
        txn.setAmount(amount.negate());
        txn.setBeforeBalance(before);
        txn.setAfterBalance(after);
        txn.setRemark(remark);
        txn.setOperatorId(null);
        txn.setCreateTime(now);
        walletTransactionMapper.insert(txn);
        return Result.success(null);
    }

    private static String genTxnNo() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    @Override
    public Result<PageResult<BorrowRecordListItem>> query(String userName, String itemName, String status,
                                                          LocalDateTime startTime, LocalDateTime endTime,
                                                          long pageNo, long pageSize) {
        return pageBorrowRecords(trimOrNull(userName), trimOrNull(itemName), null, status, startTime, endTime, pageNo, pageSize);
    }

    @Override
    public Result<PageResult<BorrowRecordListItem>> my(String itemName, String status,
                                                       LocalDateTime startTime, LocalDateTime endTime,
                                                       long pageNo, long pageSize) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        return pageBorrowRecords(null, trimOrNull(itemName), currentUser.getUserId(), status, startTime, endTime, pageNo, pageSize);
    }

    private static String trimOrNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim();
    }

    private Result<PageResult<BorrowRecordListItem>> pageBorrowRecords(String userNameLike,
                                                                       String itemNameLike,
                                                                       Long scopeUserId,
                                                                       String status,
                                                                       LocalDateTime startTime,
                                                                       LocalDateTime endTime,
                                                                       long pageNo,
                                                                       long pageSize) {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        String statusFilter = trimOrNull(status);
        if (statusFilter != null) {
            statusFilter = statusFilter.toUpperCase();
        }

        long total = borrowRecordMapper.countByCondition(userNameLike, itemNameLike, scopeUserId, statusFilter, startTime, endTime);
        List<BorrowRecordListItem> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = borrowRecordMapper.listByCondition(userNameLike, itemNameLike, scopeUserId, statusFilter, startTime, endTime, offset, pageSize);
        }

        PageResult<BorrowRecordListItem> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }
}
