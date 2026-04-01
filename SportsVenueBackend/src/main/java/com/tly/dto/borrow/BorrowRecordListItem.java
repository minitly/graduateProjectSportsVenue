package com.tly.dto.borrow;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借用记录分页查询（5.3.1 / 5.3.2）响应行：返回用户名、器材名，不返回 userId、itemId。
 */
@Data
public class BorrowRecordListItem {
    private Long id;
    private String userName;
    private String itemName;
    private Integer quantity;
    private String status;
    private LocalDateTime requestedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime returnedTime;
    private BigDecimal depositSnapshot;
    private String conditionOnBorrow;
    private String conditionOnReturn;
    private Integer damagedLostCount;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
