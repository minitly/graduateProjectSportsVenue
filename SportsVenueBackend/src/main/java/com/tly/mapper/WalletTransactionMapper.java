package com.tly.mapper;

import com.tly.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WalletTransactionMapper {

    int insert(WalletTransaction transaction);

    long countMyByCondition(@Param("userId") Long userId,
                            @Param("txnType") String txnType,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    List<WalletTransaction> listMyByCondition(@Param("userId") Long userId,
                                              @Param("txnType") String txnType,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("offset") long offset,
                                              @Param("limit") long limit);

    WalletTransaction findBookingDebitByReservationId(@Param("reservationId") Long reservationId);

    long countBookingRefundByReservationId(@Param("reservationId") Long reservationId);

    WalletTransaction findBorrowDepositDebitByBorrowId(@Param("borrowId") Long borrowId);

    long countBorrowRefundByBorrowId(@Param("borrowId") Long borrowId);
}
