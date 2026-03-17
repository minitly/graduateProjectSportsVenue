package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.SysUser;

public interface UserService {

    Result<SysUser> create(SysUser user);

    Result<PageResult<SysUser>> listAll(long pageNo, long pageSize);

    Result<SysUser> getById(Long id);

    Result<PageResult<SysUser>> search(Integer status, String keyword, long pageNo, long pageSize);

    Result<SysUser> update(Long id, SysUser user);
}

