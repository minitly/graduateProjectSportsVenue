package com.tly.service.impl;

import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.FloorPlan;
import com.tly.mapper.FloorPlanMapper;
import com.tly.service.FloorPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class FloorPlanServiceImpl implements FloorPlanService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    @Autowired
    private FloorPlanMapper floorPlanMapper;

    @Override
    public Result<FloorPlan> create(FloorPlan request) {
        Result<Void> roleCheck = requireOwnerOrAdmin("无权限新增场地图");
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            return Result.fail(400, "场地图标题不能为空");
        }
        if (!StringUtils.hasText(request.getContentJson())) {
            return Result.fail(400, "contentJson不能为空");
        }

        String status = normalizeCreateStatus(request.getStatus());
        if (status == null) {
            return Result.fail(400, "状态参数不合法");
        }

        FloorPlan toSave = new FloorPlan();
        toSave.setTitle(request.getTitle().trim());
        toSave.setDescription(trimOrNull(request.getDescription()));
        toSave.setStatus(status);
        toSave.setContentJson(request.getContentJson());

        floorPlanMapper.insert(toSave);
        FloorPlan db = floorPlanMapper.selectById(toSave.getId());
        return Result.success("新增成功", db);
    }

    @Override
    public Result<FloorPlan> update(Long id, FloorPlan request) {
        Result<Void> roleCheck = requireOwnerOrAdmin("无权限修改场地图");
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (id == null) {
            return Result.fail(400, "场地图ID不能为空");
        }
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            return Result.fail(400, "场地图标题不能为空");
        }
        if (!StringUtils.hasText(request.getStatus())) {
            return Result.fail(400, "状态不能为空");
        }
        if (!StringUtils.hasText(request.getContentJson())) {
            return Result.fail(400, "contentJson不能为空");
        }

        String status = request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!isValidStatus(status)) {
            return Result.fail(400, "状态参数不合法");
        }

        FloorPlan exists = floorPlanMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "场地图不存在");
        }

        exists.setTitle(request.getTitle().trim());
        exists.setDescription(trimOrNull(request.getDescription()));
        exists.setStatus(status);
        exists.setContentJson(request.getContentJson());
        floorPlanMapper.update(exists);

        FloorPlan db = floorPlanMapper.selectById(id);
        return Result.success("修改成功", db);
    }

    @Override
    public Result<Void> delete(Long id) {
        Result<Void> roleCheck = requireOwnerOrAdmin("无权限删除场地图");
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (id == null) {
            return Result.fail(400, "场地图ID不能为空");
        }
        FloorPlan exists = floorPlanMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "场地图不存在");
        }
        floorPlanMapper.logicalDelete(id);
        return Result.success("删除成功", null);
    }

    @Override
    public Result<PageResult<FloorPlan>> query(String keyword, String status, long pageNo, long pageSize) {
        Result<Void> loginCheck = requireLogin();
        if (loginCheck != null) {
            return Result.fail(loginCheck.getCode(), loginCheck.getMessage());
        }
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }

        String statusFilter = null;
        if (StringUtils.hasText(status)) {
            statusFilter = status.trim().toUpperCase(Locale.ROOT);
            if (!isValidStatus(statusFilter)) {
                return Result.fail(400, "状态参数不合法");
            }
        }

        long offset = (pageNo - 1) * pageSize;
        long total = floorPlanMapper.countByCondition(trimOrNull(keyword), statusFilter);
        List<FloorPlan> records = total == 0
                ? Collections.emptyList()
                : floorPlanMapper.listByCondition(trimOrNull(keyword), statusFilter, offset, pageSize);
        PageResult<FloorPlan> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    @Override
    public Result<FloorPlan> getById(Long id) {
        Result<Void> loginCheck = requireLogin();
        if (loginCheck != null) {
            return Result.fail(loginCheck.getCode(), loginCheck.getMessage());
        }
        if (id == null) {
            return Result.fail(400, "场地图ID不能为空");
        }
        FloorPlan floorPlan = floorPlanMapper.selectById(id);
        if (floorPlan == null) {
            return Result.fail(404, "场地图不存在");
        }
        return Result.success("查询成功", floorPlan);
    }

    private Result<Void> requireLogin() {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        return null;
    }

    private Result<Void> requireOwnerOrAdmin(String forbiddenMessage) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        String role = currentUser.getRole();
        if (!"OWNER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return Result.fail(403, forbiddenMessage);
        }
        return null;
    }

    private boolean isValidStatus(String status) {
        return STATUS_DRAFT.equals(status) || STATUS_PUBLISHED.equals(status) || STATUS_OFFLINE.equals(status);
    }

    private String normalizeCreateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_PUBLISHED;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return isValidStatus(normalized) ? normalized : null;
    }

    private String trimOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

