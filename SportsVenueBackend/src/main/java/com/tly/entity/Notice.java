package com.tly.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notice {
    private Long id;
    private String title;
    private String content;
    /**
     * DRAFT / PUBLISHED / OFFLINE
     */
    private String status;
    /**
     * 最新一次发布时间
     */
    private LocalDateTime publishTime;
    /**
     * 0-未删除，1-已删除
     */
    private Integer isDeleted;
    private Long createBy;
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

