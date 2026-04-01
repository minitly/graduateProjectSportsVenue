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
    /**
     * 管理员确认借出、实际扣费时写入的租金+押金合计（展示用）；资金以 wallet_transaction 为准。
     */
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
     * 损坏/丢失个数（仅归还时填写；损坏与丢失对场馆均按扣押金处理）
     */
    private Integer damagedLostCount;
    /**
     * 不同阶段含义不同：申请用途 / 借出补充说明 / 归还补充说明（覆盖写入）
     */
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

