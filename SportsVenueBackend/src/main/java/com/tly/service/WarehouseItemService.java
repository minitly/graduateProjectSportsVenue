package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.WarehouseItem;

public interface WarehouseItemService {

    Result<WarehouseItem> create(WarehouseItem item);

    Result<WarehouseItem> update(Long id, WarehouseItem item);

    Result<WarehouseItem> getById(Long id);

    Result<PageResult<WarehouseItem>> query(String type, String keyword, Boolean onlyAvailable,
                                            long pageNo, long pageSize);
}

