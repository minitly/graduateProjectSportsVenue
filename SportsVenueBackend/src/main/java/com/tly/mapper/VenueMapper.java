package com.tly.mapper;

import com.tly.entity.Venue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VenueMapper {

    int insert(Venue venue);

    int update(Venue venue);

    int deleteById(@Param("id") Long id);

    Venue selectById(@Param("id") Long id);

    long countByCondition(@Param("type") String type,
                          @Param("status") String status,
                          @Param("keyword") String keyword);

    List<Venue> listByCondition(@Param("type") String type,
                                @Param("status") String status,
                                @Param("keyword") String keyword,
                                @Param("offset") long offset,
                                @Param("pageSize") long pageSize);
}

