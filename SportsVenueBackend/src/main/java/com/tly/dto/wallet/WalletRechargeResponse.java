package com.tly.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletRechargeResponse {
    private Long transactionId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private LocalDateTime createTime;
}
