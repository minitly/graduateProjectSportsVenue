package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.wallet.WalletRechargeRequest;
import com.tly.dto.wallet.WalletRechargeResponse;
import com.tly.dto.wallet.WalletTransactionListItem;

import java.time.LocalDate;

public interface WalletService {

    Result<WalletRechargeResponse> recharge(WalletRechargeRequest request);

    Result<PageResult<WalletTransactionListItem>> myTransactions(String type,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate,
                                                                 long pageNo,
                                                                 long pageSize);
}
