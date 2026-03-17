package com.tly.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BorrowRecord {
    private Long id;
    private Long userId;
    private Long itemId;
    private Integer quantity;
    /**
     * 借用状态：REQUESTED / USING / RETURNED
     */
    private String status;
    private LocalDateTime requestedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime returnedTime;
    private BigDecimal depositSnapshot;
    /**
     * 器材状况：GOOD / DAMAGED / LOST
     */
    private String conditionOnBorrow;
    /**
     * 器材状况：GOOD / DAMAGED / LOST
     */
    private String conditionOnReturn;
    /**
     * 不同阶段含义不同：申请用途 / 借出补充说明 / 归还补充说明（覆盖写入）
     */
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

