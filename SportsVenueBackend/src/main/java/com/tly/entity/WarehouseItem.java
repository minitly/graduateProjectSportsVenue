package com.tly.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WarehouseItem {
    private Long id;
    private String name;
    private String type;
    private String model;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer damagedQuantity;
    private BigDecimal depositAmount;
    /**
     * 借用租金（元/件），单次借用的租借费用；与押金 depositAmount 含义不同。
     */
    private BigDecimal borrowAmount;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

