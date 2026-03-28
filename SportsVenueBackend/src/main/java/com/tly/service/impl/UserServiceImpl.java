package com.tly.service.impl;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.SysUser;
import com.tly.mapper.SysUserMapper;
import com.tly.service.UserService;
import com.tly.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public Result<SysUser> create(SysUser request) {
        String role = request.getRole() == null ? null : request.getRole().toUpperCase(Locale.ROOT);
        if (!"USER".equals(role) && !"OWNER".equals(role)) {
            return Result.fail(400, "不支持的用户角色");
        }

        if (!StringUtils.hasText(request.getUsername())) {
            return Result.fail(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            return Result.fail(400, "密码不能为空");
        }

        SysUser exists = sysUserMapper.findByUsername(request.getUsername());
        if (exists != null) {
            return Result.fail(400, "用户名已存在");
        }

        if ("OWNER".equals(role)) {
            if (!StringUtils.hasText(request.getAdminPassword())) {
                return Result.fail(400, "创建场地管理员时必须提供 ADMIN 密码");
            }
            SysUser admin = sysUserMapper.findByUsername("admin");
            if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
                return Result.fail(400, "系统 ADMIN 账号不存在，请先初始化管理员");
            }
            if (!PasswordUtil.matches(request.getAdminPassword(), admin.getPassword())) {
                return Result.fail(403, "ADMIN 密码错误，禁止创建场地管理员");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(role);
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());

        sysUserMapper.insert(user);

        SysUser dbUser = sysUserMapper.findById(user.getId());
        if (dbUser != null) {
            dbUser.setPassword(null);
        }
        return Result.success("创建用户成功", dbUser);
    }

    @Override
    public Result<PageResult<SysUser>> listAll(long pageNo, long pageSize) {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        long total = sysUserMapper.countAll();
        List<SysUser> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = sysUserMapper.listAll(offset, pageSize);
        }

        for (SysUser u : records) {
            u.setPassword(null);
        }
        PageResult<SysUser> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    @Override
    public Result<SysUser> getById(Long id) {
        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success("查询成功", user);
    }

    @Override
    public Result<PageResult<SysUser>> search(Integer status, String keyword, long pageNo, long pageSize) {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        long total = sysUserMapper.countByCondition(status, keyword);
        List<SysUser> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = sysUserMapper.listByCondition(status, keyword, offset, pageSize);
        }

        for (SysUser u : records) {
            u.setPassword(null);
        }
        PageResult<SysUser> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    @Override
    public Result<SysUser> update(Long id, SysUser request) {
        if (id == null || request.getId() == null || !id.equals(request.getId())) {
            return Result.fail(400, "路径ID与请求体ID不一致");
        }

        SysUser exists = sysUserMapper.findById(id);
        if (exists == null) {
            return Result.fail(404, "用户不存在");
        }

        String role = request.getRole() == null ? null : request.getRole().toUpperCase(Locale.ROOT);
        if (!"USER".equals(role) && !"OWNER".equals(role) && !"ADMIN".equals(role)) {
            return Result.fail(400, "不支持的用户角色");
        }

        // 用户名保持唯一约束：如果修改了用户名，需要检查是否与其他用户冲突
        if (StringUtils.hasText(request.getUsername())
                && !request.getUsername().equals(exists.getUsername())) {
            SysUser another = sysUserMapper.findByUsername(request.getUsername());
            if (another != null && !another.getId().equals(id)) {
                return Result.fail(400, "用户名已存在");
            }
        }

        exists.setUsername(request.getUsername());
        if (StringUtils.hasText(request.getPassword())) {
            exists.setPassword(PasswordUtil.encode(request.getPassword()));
        }
        exists.setRealName(request.getRealName());
        exists.setRole(role);
        exists.setStatus(request.getStatus());
        exists.setPhone(request.getPhone());
        exists.setEmail(request.getEmail());

        sysUserMapper.update(exists);

        SysUser dbUser = sysUserMapper.findById(id);
        if (dbUser != null) {
            dbUser.setPassword(null);
        }
        return Result.success("更新用户成功", dbUser);
    }
}

