package com.tly.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletTransactionListItem {
    private Long id;
    private String type;
    private BigDecimal amount;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private String bizType;
    private Long bizId;
    private String remark;
    private LocalDateTime createTime;
}
