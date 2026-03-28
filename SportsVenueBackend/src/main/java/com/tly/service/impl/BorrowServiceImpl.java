package com.tly.service.impl;

import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.borrow.BorrowRecordListItem;
import com.tly.entity.BorrowRecord;
import com.tly.entity.WarehouseItem;
import com.tly.mapper.BorrowRecordMapper;
import com.tly.mapper.WarehouseItemMapper;
import com.tly.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private WarehouseItemMapper warehouseItemMapper;

    @Override
    public Result<BorrowRecord> apply(BorrowRecord record) {
        if (record.getItemId() == null) {
            return Result.fail(400, "器材ID不能为空");
        }
        if (record.getQuantity() == null || record.getQuantity() <= 0) {
            return Result.fail(400, "借用数量必须大于0");
        }

        // 使用当前登录用户作为借用人
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

        // 记录押金快照：单件押金 * 数量
        if (item.getDepositAmount() != null) {
            BigDecimal snapshot = item.getDepositAmount()
                    .multiply(BigDecimal.valueOf(record.getQuantity()));
            record.setDepositSnapshot(snapshot);
        }

        // 申请阶段不填写 conditionOnBorrow / conditionOnReturn
        record.setConditionOnBorrow(null);
        record.setConditionOnReturn(null);

        borrowRecordMapper.insert(record);
        BorrowRecord dbRecord = borrowRecordMapper.selectById(record.getId());
        return Result.success("借用申请已提交", dbRecord);
    }

    @Override
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

        // 默认借出状况为 GOOD
        if (!StringUtils.hasText(conditionOnBorrow)) {
            conditionOnBorrow = "GOOD";
        }

        exists.setStatus("USING");
        exists.setApprovedTime(LocalDateTime.now());
        exists.setConditionOnBorrow(conditionOnBorrow);
        if (remark != null) {
            exists.setRemark(remark);
        }

        // 扣减可用数量
        item.setAvailableQuantity(item.getAvailableQuantity() - exists.getQuantity());
        warehouseItemMapper.update(item);
        borrowRecordMapper.update(exists);

        BorrowRecord dbRecord = borrowRecordMapper.selectById(id);
        return Result.success("确认借出成功", dbRecord);
    }

    @Override
    public Result<BorrowRecord> confirmReturn(Long id, String conditionOnReturn, String remark) {
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

        // 默认归还状况为 GOOD
        if (!StringUtils.hasText(conditionOnReturn)) {
            conditionOnReturn = "GOOD";
        }

        exists.setStatus("RETURNED");
        exists.setReturnedTime(LocalDateTime.now());
        exists.setConditionOnReturn(conditionOnReturn);
        if (remark != null) {
            exists.setRemark(remark);
        }

        // 根据归还状况调整库存
        int quantity = exists.getQuantity() == null ? 0 : exists.getQuantity();
        int availableQuantity = item.getAvailableQuantity() == null ? 0 : item.getAvailableQuantity();
        int damagedQuantity = item.getDamagedQuantity() == null ? 0 : item.getDamagedQuantity();
        if ("GOOD".equalsIgnoreCase(conditionOnReturn)) {
            item.setAvailableQuantity(availableQuantity + quantity);
        } else if ("DAMAGED".equalsIgnoreCase(conditionOnReturn)) {
            item.setDamagedQuantity(damagedQuantity + quantity);
            // 损坏不恢复可借数量
        } else if ("LOST".equalsIgnoreCase(conditionOnReturn)) {
            // 丢失：总数和可借数均不恢复，只作为损失记入 damagedQuantity
            item.setDamagedQuantity(damagedQuantity + quantity);
        }

        warehouseItemMapper.update(item);
        borrowRecordMapper.update(exists);

        BorrowRecord dbRecord = borrowRecordMapper.selectById(id);
        return Result.success("确认归还成功", dbRecord);
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

