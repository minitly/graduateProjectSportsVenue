package com.tly.controller;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.FloorPlan;
import com.tly.service.FloorPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/floor-plans")
public class FloorPlanController {

    @Autowired
    private FloorPlanService floorPlanService;

    /**
     * 新增场地图：POST /sportsVenue/floor-plans
     */
    @PostMapping
    public Result<FloorPlan> create(@RequestBody FloorPlan floorPlan) {
        return floorPlanService.create(floorPlan);
    }

    /**
     * 修改场地图：PUT /sportsVenue/floor-plans/{id}
     */
    @PutMapping("/{id}")
    public Result<FloorPlan> update(@PathVariable("id") Long id, @RequestBody FloorPlan floorPlan) {
        return floorPlanService.update(id, floorPlan);
    }

    /**
     * 删除场地图（逻辑删除）：DELETE /sportsVenue/floor-plans/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        return floorPlanService.delete(id);
    }

    /**
     * 条件列表查询（标题模糊 + 状态）：GET /sportsVenue/floor-plans
     */
    @GetMapping
    public Result<PageResult<FloorPlan>> query(@RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                               @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return floorPlanService.query(keyword, status, pageNo, pageSize);
    }

    /**
     * 按ID查询详情：GET /sportsVenue/floor-plans/{id}
     */
    @GetMapping("/{id}")
    public Result<FloorPlan> getById(@PathVariable("id") Long id) {
        return floorPlanService.getById(id);
    }
}

