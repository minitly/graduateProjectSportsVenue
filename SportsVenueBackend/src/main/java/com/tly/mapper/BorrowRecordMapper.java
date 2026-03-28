package com.tly.mapper;

import com.tly.dto.borrow.BorrowRecordListItem;
import com.tly.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BorrowRecordMapper {

    int insert(BorrowRecord record);

    int update(BorrowRecord record);

    BorrowRecord selectById(@Param("id") Long id);

    long countByCondition(@Param("userNameLike") String userNameLike,
                          @Param("itemNameLike") String itemNameLike,
                          @Param("scopeUserId") Long scopeUserId,
                          @Param("status") String status,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    List<BorrowRecordListItem> listByCondition(@Param("userNameLike") String userNameLike,
                                                 @Param("itemNameLike") String itemNameLike,
                                                 @Param("scopeUserId") Long scopeUserId,
                                                 @Param("status") String status,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("offset") long offset,
                                                 @Param("pageSize") long pageSize);
}

