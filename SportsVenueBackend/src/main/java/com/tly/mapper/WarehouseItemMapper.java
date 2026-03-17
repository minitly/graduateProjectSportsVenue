package com.tly.mapper;

import com.tly.entity.WarehouseItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseItemMapper {

    int insert(WarehouseItem item);

    int update(WarehouseItem item);

    WarehouseItem selectById(@Param("id") Long id);

    long countByCondition(@Param("type") String type,
                          @Param("keyword") String keyword,
                          @Param("onlyAvailable") Boolean onlyAvailable);

    List<WarehouseItem> listByCondition(@Param("type") String type,
                                        @Param("keyword") String keyword,
                                        @Param("onlyAvailable") Boolean onlyAvailable,
                                        @Param("offset") long offset,
                                        @Param("pageSize") long pageSize);
}

