package com.tly.controller;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.wallet.WalletRechargeRequest;
import com.tly.dto.wallet.WalletRechargeResponse;
import com.tly.dto.wallet.WalletTransactionListItem;
import com.tly.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping("/recharge")
    public Result<WalletRechargeResponse> recharge(@RequestBody WalletRechargeRequest request) {
        return walletService.recharge(request);
    }

    @GetMapping("/transactions/my")
    public Result<PageResult<WalletTransactionListItem>> myTransactions(@RequestParam(value = "type", required = false) String type,
                                                                        @RequestParam(value = "startDate", required = false)
                                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                        @RequestParam(value = "endDate", required = false)
                                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                        @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                                                        @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return walletService.myTransactions(type, startDate, endDate, pageNo, pageSize);
    }
}
