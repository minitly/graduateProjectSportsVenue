package com.tly.mapper;

import com.tly.entity.FloorPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FloorPlanMapper {

    int insert(FloorPlan floorPlan);

    int update(FloorPlan floorPlan);

    int logicalDelete(@Param("id") Long id);

    FloorPlan selectById(@Param("id") Long id);

    long countByCondition(@Param("keyword") String keyword,
                          @Param("status") String status);

    List<FloorPlan> listByCondition(@Param("keyword") String keyword,
                                    @Param("status") String status,
                                    @Param("offset") long offset,
                                    @Param("pageSize") long pageSize);
}

