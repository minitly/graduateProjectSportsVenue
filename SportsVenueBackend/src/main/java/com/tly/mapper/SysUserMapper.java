package com.tly.mapper;

import com.tly.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper {

    SysUser findByUsername(@Param("username") String username);

    int insert(SysUser user);

    /**
     * 根据角色查询权限编码列表
     */
    List<String> findPermissionCodesByRole(@Param("role") String role);
}

