package com.tly.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LoginResponse {
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private BigDecimal balance;
    private String token;
    private List<String> permissions;
}

