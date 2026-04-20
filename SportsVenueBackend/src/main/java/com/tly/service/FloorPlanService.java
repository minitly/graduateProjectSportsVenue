package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.FloorPlan;

public interface FloorPlanService {

    Result<FloorPlan> create(FloorPlan floorPlan);

    Result<FloorPlan> update(Long id, FloorPlan floorPlan);

    Result<Void> delete(Long id);

    Result<PageResult<FloorPlan>> query(String keyword, String status, long pageNo, long pageSize);

    Result<FloorPlan> getById(Long id);
}

