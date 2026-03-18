package com.tly.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    /**
     * USER / OWNER / ADMIN
     */
    private String role;
    /**
     * 1-正常，0-禁用
     */
    private Integer status;
    /**
     * 本月违规次数（自然月累计，由预约模块维护）
     */
    private Integer violationCountMonth;
    /**
     * 违规次数所属月份（yyyy-MM，由预约模块维护）
     */
    private String violationMonth;
    /**
     * 预约禁用截止时间（到下月1日00:00；NULL 表示未禁用，由预约模块维护）
     */
    private LocalDateTime bookingBannedUntil;
    private String phone;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

