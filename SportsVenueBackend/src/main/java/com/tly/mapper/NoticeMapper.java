package com.tly.mapper;

import com.tly.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NoticeMapper {

    int insert(Notice notice);

    Notice selectById(@Param("id") Long id);

    int update(Notice notice);

    int logicalDelete(@Param("id") Long id, @Param("updateBy") Long updateBy);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("publishTime") LocalDateTime publishTime,
                     @Param("updateBy") Long updateBy);

    long countManage(@Param("keyword") String keyword, @Param("status") String status);

    List<Notice> listManage(@Param("keyword") String keyword,
                            @Param("status") String status,
                            @Param("offset") long offset,
                            @Param("pageSize") long pageSize);

    long countPublished(@Param("keyword") String keyword);

    List<Notice> listPublished(@Param("keyword") String keyword,
                               @Param("offset") long offset,
                               @Param("pageSize") long pageSize);

    Notice selectPublishedById(@Param("id") Long id);
}

