package com.tly.mapper;

import com.tly.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper {

    SysUser findByUsername(@Param("username") String username);

    int insert(SysUser user);

    SysUser findById(@Param("id") Long id);

    long countAll();

    List<SysUser> listAll(@Param("offset") long offset, @Param("limit") long limit);

    long countByCondition(@Param("status") Integer status, @Param("keyword") String keyword);

    List<SysUser> listByCondition(@Param("status") Integer status,
                                  @Param("keyword") String keyword,
                                  @Param("offset") long offset,
                                  @Param("limit") long limit);

    int update(SysUser user);

    int updateBookingCredit(@Param("id") Long id,
                            @Param("violationCountMonth") Integer violationCountMonth,
                            @Param("violationMonth") String violationMonth,
                            @Param("bookingBannedUntil") java.time.LocalDateTime bookingBannedUntil);

    /**
     * 根据角色查询权限编码列表
     */
    List<String> findPermissionCodesByRole(@Param("role") String role);
}

