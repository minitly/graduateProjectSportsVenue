package com.tly.service.impl;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.WarehouseItem;
import com.tly.mapper.WarehouseItemMapper;
import com.tly.service.WarehouseItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class WarehouseItemServiceImpl implements WarehouseItemService {

    @Autowired
    private WarehouseItemMapper warehouseItemMapper;

    @Override
    public Result<WarehouseItem> create(WarehouseItem item) {
        if (!StringUtils.hasText(item.getName())) {
            return Result.fail(400, "器材名称不能为空");
        }
        if (item.getTotalQuantity() == null || item.getTotalQuantity() < 0) {
            return Result.fail(400, "总数量不能为空且不能为负数");
        }
        if (item.getAvailableQuantity() == null || item.getAvailableQuantity() < 0) {
            item.setAvailableQuantity(item.getTotalQuantity());
        }
        if (item.getDamagedQuantity() == null || item.getDamagedQuantity() < 0) {
            item.setDamagedQuantity(0);
        }

        warehouseItemMapper.insert(item);
        WarehouseItem dbItem = warehouseItemMapper.selectById(item.getId());
        return Result.success("创建器材成功", dbItem);
    }

    @Override
    public Result<WarehouseItem> update(Long id, WarehouseItem item) {
        if (id == null || item.getId() == null || !id.equals(item.getId())) {
            return Result.fail(400, "路径ID与请求体ID不一致");
        }
        WarehouseItem exists = warehouseItemMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "器材不存在");
        }
        if (!StringUtils.hasText(item.getName())) {
            return Result.fail(400, "器材名称不能为空");
        }

        warehouseItemMapper.update(item);
        WarehouseItem dbItem = warehouseItemMapper.selectById(id);
        return Result.success("更新器材成功", dbItem);
    }

    @Override
    public Result<WarehouseItem> getById(Long id) {
        WarehouseItem item = warehouseItemMapper.selectById(id);
        if (item == null) {
            return Result.fail(404, "器材不存在");
        }
        return Result.success("查询成功", item);
    }

    @Override
    public Result<PageResult<WarehouseItem>> query(String type, String keyword, Boolean onlyAvailable,
                                                   long pageNo, long pageSize) {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        long total = warehouseItemMapper.countByCondition(type, keyword, onlyAvailable);
        List<WarehouseItem> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = warehouseItemMapper.listByCondition(type, keyword, onlyAvailable, offset, pageSize);
        }
        PageResult<WarehouseItem> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }
}

