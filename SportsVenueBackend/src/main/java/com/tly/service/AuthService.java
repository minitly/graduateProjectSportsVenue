package com.tly.service;

import com.tly.common.Result;
import com.tly.dto.auth.LoginRequest;
import com.tly.dto.auth.LoginResponse;
import com.tly.dto.auth.RegisterRequest;

import java.util.Map;

public interface AuthService {

    /**
     * 用户注册
     */
    Result<Map<String, Object>> register(RegisterRequest request);

    /**
     * 用户登录
     */
    Result<LoginResponse> login(LoginRequest request);
}

