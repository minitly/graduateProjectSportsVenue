package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.borrow.BorrowRecordListItem;
import com.tly.entity.BorrowRecord;

import java.time.LocalDateTime;

public interface BorrowService {

    Result<BorrowRecord> apply(BorrowRecord record);

    Result<BorrowRecord> approve(Long id, String conditionOnBorrow, String remark);

    Result<BorrowRecord> confirmReturn(Long id, String conditionOnReturn, String remark, Integer damagedLostCount);

    Result<PageResult<BorrowRecordListItem>> query(String userName, String itemName, String status,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   long pageNo, long pageSize);

    Result<PageResult<BorrowRecordListItem>> my(String itemName, String status,
                                                LocalDateTime startTime, LocalDateTime endTime,
                                                long pageNo, long pageSize);
}

