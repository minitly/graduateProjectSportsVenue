package com.tly.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletTransaction {
    private Long id;
    private Long userId;
    private String txnNo;
    private String txnType;
    private String bizType;
    private Long bizId;
    private BigDecimal amount;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;
}
