package com.tly.service.impl;

import com.tly.auth.JwtUtil;
import com.tly.common.Result;
import com.tly.dto.auth.LoginRequest;
import com.tly.dto.auth.LoginResponse;
import com.tly.dto.auth.RegisterRequest;
import com.tly.entity.SysUser;
import com.tly.mapper.SysUserMapper;
import com.tly.service.AuthService;
import com.tly.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Result<Map<String, Object>> register(RegisterRequest request) {
        String role = request.getRole().toUpperCase(Locale.ROOT);
        if (!"USER".equals(role) && !"OWNER".equals(role)) {
            return Result.fail(400, "不支持的注册角色");
        }

        SysUser exists = sysUserMapper.findByUsername(request.getUsername());
        if (exists != null) {
            return Result.fail(400, "用户名已存在");
        }

        if ("OWNER".equals(role)) {
            // OWNER 注册必须校验 ADMIN 密码
            if (!StringUtils.hasText(request.getAdminPassword())) {
                return Result.fail(400, "注册场地管理员必须提供 ADMIN 密码");
            }
            SysUser admin = sysUserMapper.findByUsername("admin");
            if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
                return Result.fail(400, "系统 ADMIN 账号不存在，请先初始化管理员");
            }
            String rawAdminPassword = request.getAdminPassword();
            if (!PasswordUtil.matches(rawAdminPassword, admin.getPassword())) {
                return Result.fail(403, "ADMIN 密码错误，禁止注册场地管理员");
            }
        }

        SysUser toSave = new SysUser();
        toSave.setUsername(request.getUsername());
        toSave.setPassword(PasswordUtil.encode(request.getPassword()));
        toSave.setRealName(request.getRealName());
        toSave.setRole(role);
        toSave.setStatus(1);
        toSave.setPhone(request.getPhone());
        toSave.setEmail(request.getEmail());

        sysUserMapper.insert(toSave);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", toSave.getId());
        data.put("username", toSave.getUsername());
        data.put("realName", toSave.getRealName());
        data.put("role", toSave.getRole());

        return Result.success("注册成功", data);
    }

    @Override
    public Result<LoginResponse> login(LoginRequest request) {
        SysUser user = sysUserMapper.findByUsername(request.getUsername());
        if (user == null) {
            return Result.fail(401, "用户名不存在");
        }

        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            return Result.fail(401, "密码错误");
        }

        String role = request.getRole().toUpperCase(Locale.ROOT);
        if (!role.equalsIgnoreCase(user.getRole())) {
            return Result.fail(403, "登录身份不匹配");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        List<String> permissions = sysUserMapper.findPermissionCodesByRole(user.getRole());

        LoginResponse resp = LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .token(token)
                .permissions(permissions)
                .build();

        return Result.success("登录成功", resp);
    }
}

