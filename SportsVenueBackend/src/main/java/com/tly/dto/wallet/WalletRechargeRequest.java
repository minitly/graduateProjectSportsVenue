package com.tly.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletRechargeRequest {
    private BigDecimal amount;
    private String remark;
}
