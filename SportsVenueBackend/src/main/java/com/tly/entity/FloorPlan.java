package com.tly.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FloorPlan {
    private Long id;
    private String title;
    private String description;
    /**
     * DRAFT / PUBLISHED / OFFLINE
     */
    private String status;
    /**
     * 画布JSON字符串
     */
    private String contentJson;
    /**
     * 0-未删除，1-已删除
     */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

