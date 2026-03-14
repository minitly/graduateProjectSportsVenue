package com.tly.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String realName;

    /**
     * USER 或 OWNER
     */
    @NotBlank
    private String role;

    /**
     * 当 role = OWNER 时必填，
     * 当 role = USER 时前端可传空字符串或不传（后端会忽略）
     */
    private String adminPassword;

    private String phone;

    private String email;
}

