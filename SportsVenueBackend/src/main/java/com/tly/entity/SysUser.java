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
    private String phone;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

