package com.tly.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /**
     * 仅存库与请求体内部分赋值使用，不序列化到 JSON。
     */
    @JsonIgnore
    private Long createBy;
    @JsonIgnore
    private Long updateBy;
    /**
     * 由后端根据 create_by 关联 sys_user.username 填充。
     */
    private String createByUsername;
    /**
     * 由后端根据 update_by 关联 sys_user.username 填充。
     */
    private String updateByUsername;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

