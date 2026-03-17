package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.BorrowRecord;

import java.time.LocalDateTime;

public interface BorrowService {

    Result<BorrowRecord> apply(BorrowRecord record);

    Result<BorrowRecord> approve(Long id, String conditionOnBorrow, String remark);

    Result<BorrowRecord> confirmReturn(Long id, String conditionOnReturn, String remark);

    Result<PageResult<BorrowRecord>> query(Long userId, Long itemId, String status,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           long pageNo, long pageSize);
}

