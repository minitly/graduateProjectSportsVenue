package com.tly.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FloorPlanItem {
    private Long id;
    /**
     * 前端画布项ID（原 items[].id）
     */
    private String itemUid;
    private String type;
    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
    private Integer rotation;
    private String label;
    private String color;
    /**
     * 关联场地ID，可为空
     */
    private Long venueId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

