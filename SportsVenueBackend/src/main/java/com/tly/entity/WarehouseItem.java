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
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

