package com.tly.controller;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.SysUser;
import com.tly.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理模块接口
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册（管理端创建用户）：POST /sportsVenue/users
     */
    @PostMapping
    public Result<SysUser> create(@RequestBody SysUser user) {
        return userService.create(user);
    }

    /**
     * 查询所有用户（分页）：GET /sportsVenue/users
     */
    @GetMapping
    public Result<PageResult<SysUser>> listAll(@RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                               @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return userService.listAll(pageNo, pageSize);
    }

    /**
     * 根据 ID 查询单个用户详情：GET /sportsVenue/users/{id}
     */
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable("id") Long id) {
        return userService.getById(id);
    }

    /**
     * 分类/条件查询用户（状态 + 用户名模糊，分页）：GET /sportsVenue/users/search
     */
    @GetMapping("/search")
    public Result<PageResult<SysUser>> search(@RequestParam(value = "status", required = false) Integer status,
                                              @RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                              @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return userService.search(status, keyword, pageNo, pageSize);
    }

    /**
     * 修改用户信息（含禁用/启用）：PUT /sportsVenue/users/{id}
     */
    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable("id") Long id, @RequestBody SysUser user) {
        return userService.update(id, user);
    }
}

