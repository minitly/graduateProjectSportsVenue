package com.tly.controller;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.WarehouseItem;
import com.tly.service.WarehouseItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 仓库器材管理接口
 */
@RestController
@RequestMapping("/items")
public class WarehouseItemController {

    @Autowired
    private WarehouseItemService warehouseItemService;

    /**
     * 新增器材：POST /sportsVenue/items
     */
    @PostMapping
    public Result<WarehouseItem> create(@RequestBody WarehouseItem item) {
        return warehouseItemService.create(item);
    }

    /**
     * 修改器材信息：PUT /sportsVenue/items/{id}
     */
    @PutMapping("/{id}")
    public Result<WarehouseItem> update(@PathVariable("id") Long id, @RequestBody WarehouseItem item) {
        return warehouseItemService.update(id, item);
    }

    /**
     * 根据 ID 查询单个器材详情：GET /sportsVenue/items/{id}
     */
    @GetMapping("/{id}")
    public Result<WarehouseItem> getById(@PathVariable("id") Long id) {
        return warehouseItemService.getById(id);
    }

    /**
     * 器材列表分页查询：GET /sportsVenue/items
     */
    @GetMapping
    public Result<PageResult<WarehouseItem>> query(@RequestParam(value = "type", required = false) String type,
                                                   @RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "onlyAvailable", required = false) Boolean onlyAvailable,
                                                   @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                                   @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return warehouseItemService.query(type, keyword, onlyAvailable, pageNo, pageSize);
    }
}

