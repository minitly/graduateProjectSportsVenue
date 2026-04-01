package com.tly.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
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
     * 平台余额（元）
     */
    private BigDecimal balance;
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

    /**
     * 管理端创建 OWNER 时请求体携带，与系统 ADMIN 登录密码比对；不落库、不序列化到响应。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String adminPassword;
}

