package com.tly.controller;

import com.tly.common.Result;
import com.tly.dto.auth.LoginRequest;
import com.tly.dto.auth.LoginResponse;
import com.tly.dto.auth.RegisterRequest;
import com.tly.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 注册接口：POST /sportsVenue/auth/register
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 登录接口：POST /sportsVenue/auth/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}

