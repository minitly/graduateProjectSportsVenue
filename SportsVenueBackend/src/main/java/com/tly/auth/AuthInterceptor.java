package com.tly.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tly.common.Result;
import com.tly.entity.SysUser;
import com.tly.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 鉴权拦截器：统一解析并校验 JWT，除白名单外的接口均需要携带有效 Token。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/error"
    );

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserMapper sysUserMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 放行白名单和预检请求
        if (isWhiteList(uri) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeJson(response, Result.fail(401, "未登录或Token缺失，请先登录"));
            return false;
        }

        String token = authHeader.substring(7);

        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            writeJson(response, Result.fail(401, "Token无效或已过期，请重新登录"));
            return false;
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        if (userId == null || username == null || role == null) {
            writeJson(response, Result.fail(401, "Token信息不完整，请重新登录"));
            return false;
        }

        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null) {
            writeJson(response, Result.fail(401, "用户不存在，请重新登录"));
            return false;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            writeJson(response, Result.fail(403, "账号已被禁用"));
            return false;
        }

        // 设置当前用户上下文
        UserContext.set(new UserContext.CurrentUser(user.getId(), user.getUsername(), user.getRole()));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isWhiteList(String uri) {
        for (String pattern : WHITE_LIST) {
            if (uri.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    private void writeJson(HttpServletResponse response, Result<?> result) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
    }
}

