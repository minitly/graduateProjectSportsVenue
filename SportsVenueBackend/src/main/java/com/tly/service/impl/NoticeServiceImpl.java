package com.tly.service.impl;

import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.Notice;
import com.tly.mapper.NoticeMapper;
import com.tly.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class NoticeServiceImpl implements NoticeService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    @Autowired
    private NoticeMapper noticeMapper;

    @Override
    public Result<Notice> create(Notice request) {
        Result<Void> roleCheck = requireOwner();
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            return Result.fail(400, "公告标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            return Result.fail(400, "公告正文不能为空");
        }

        String status = normalizeCreateStatus(request.getStatus());
        if (status == null) {
            return Result.fail(400, "公告状态不合法");
        }

        UserContext.CurrentUser currentUser = UserContext.get();
        Notice toSave = new Notice();
        toSave.setTitle(request.getTitle());
        toSave.setContent(request.getContent());
        toSave.setStatus(status);
        toSave.setCreateBy(currentUser.getUserId());
        toSave.setUpdateBy(currentUser.getUserId());
        if (STATUS_PUBLISHED.equals(status)) {
            toSave.setPublishTime(LocalDateTime.now());
        } else {
            toSave.setPublishTime(null);
        }

        noticeMapper.insert(toSave);
        Notice db = noticeMapper.selectById(toSave.getId());
        return Result.success("创建公告成功", db);
    }

    @Override
    public Result<Notice> update(Long id, Notice request) {
        Result<Void> roleCheck = requireOwner();
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (id == null || request == null || request.getId() == null || !id.equals(request.getId())) {
            return Result.fail(400, "路径ID与请求体ID不一致");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            return Result.fail(400, "公告标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            return Result.fail(400, "公告正文不能为空");
        }
        if (!StringUtils.hasText(request.getStatus())) {
            return Result.fail(400, "公告状态不能为空");
        }

        String targetStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!isValidStatus(targetStatus)) {
            return Result.fail(400, "公告状态不合法");
        }

        Notice exists = noticeMapper.selectById(id);
        if (exists == null || (exists.getIsDeleted() != null && exists.getIsDeleted() == 1)) {
            return Result.fail(404, "公告不存在");
        }

        String oldStatus = exists.getStatus();
        exists.setTitle(request.getTitle());
        exists.setContent(request.getContent());
        exists.setStatus(targetStatus);
        exists.setUpdateBy(UserContext.get().getUserId());

        // publishTime 表示最新发布时间，仅在状态转换到 PUBLISHED 时覆盖为新值
        if (STATUS_PUBLISHED.equals(targetStatus) && !STATUS_PUBLISHED.equalsIgnoreCase(oldStatus)) {
            exists.setPublishTime(LocalDateTime.now());
        }

        noticeMapper.update(exists);
        Notice db = noticeMapper.selectById(id);
        return Result.success("更新公告成功", db);
    }

    @Override
    public Result<Void> delete(Long id) {
        Result<Void> roleCheck = requireOwner();
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (id == null) {
            return Result.fail(400, "公告ID不能为空");
        }
        Notice exists = noticeMapper.selectById(id);
        if (exists == null || (exists.getIsDeleted() != null && exists.getIsDeleted() == 1)) {
            return Result.fail(404, "公告不存在");
        }
        noticeMapper.logicalDelete(id, UserContext.get().getUserId());
        return Result.success("删除公告成功", null);
    }

    @Override
    public Result<Notice> updateStatus(Long id, String status) {
        Result<Void> roleCheck = requireOwner();
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (id == null) {
            return Result.fail(400, "公告ID不能为空");
        }
        if (!StringUtils.hasText(status)) {
            return Result.fail(400, "公告状态不能为空");
        }
        String targetStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_PUBLISHED.equals(targetStatus) && !STATUS_OFFLINE.equals(targetStatus)) {
            return Result.fail(400, "公告状态不合法");
        }

        Notice exists = noticeMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "公告不存在");
        }
        if (exists.getIsDeleted() != null && exists.getIsDeleted() == 1) {
            return Result.fail(400, "已删除公告不允许发布或下线");
        }

        LocalDateTime publishTime = exists.getPublishTime();
        if (STATUS_PUBLISHED.equals(targetStatus) && !STATUS_PUBLISHED.equalsIgnoreCase(exists.getStatus())) {
            publishTime = LocalDateTime.now();
        }
        noticeMapper.updateStatus(id, targetStatus, publishTime, UserContext.get().getUserId());

        Notice db = noticeMapper.selectById(id);
        return Result.success("状态更新成功", db);
    }

    @Override
    public Result<PageResult<Notice>> manageList(String keyword, String status, long pageNo, long pageSize) {
        Result<Void> roleCheck = requireOwner();
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
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
                return Result.fail(400, "公告状态不合法");
            }
        }

        long offset = (pageNo - 1) * pageSize;
        long total = noticeMapper.countManage(keyword, statusFilter);
        List<Notice> records = total == 0
                ? Collections.emptyList()
                : noticeMapper.listManage(keyword, statusFilter, offset, pageSize);
        PageResult<Notice> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    @Override
    public Result<Notice> manageDetail(Long id) {
        Result<Void> roleCheck = requireOwner();
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (id == null) {
            return Result.fail(400, "公告ID不能为空");
        }
        Notice notice = noticeMapper.selectById(id);
        if (notice == null || (notice.getIsDeleted() != null && notice.getIsDeleted() == 1)) {
            return Result.fail(404, "公告不存在");
        }
        return Result.success("查询成功", notice);
    }

    @Override
    public Result<PageResult<Notice>> userList(String keyword, long pageNo, long pageSize) {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;
        long total = noticeMapper.countPublished(keyword);
        List<Notice> records = total == 0
                ? Collections.emptyList()
                : noticeMapper.listPublished(keyword, offset, pageSize);
        PageResult<Notice> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    @Override
    public Result<Notice> userDetail(Long id) {
        if (id == null) {
            return Result.fail(400, "公告ID不能为空");
        }
        Notice notice = noticeMapper.selectPublishedById(id);
        if (notice == null) {
            return Result.fail(404, "公告不存在或未发布");
        }
        return Result.success("查询成功", notice);
    }

    private Result<Void> requireOwner() {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (!"OWNER".equalsIgnoreCase(currentUser.getRole())) {
            return Result.fail(403, "无权限操作公告");
        }
        return null;
    }

    private boolean isValidStatus(String status) {
        return STATUS_DRAFT.equals(status) || STATUS_PUBLISHED.equals(status) || STATUS_OFFLINE.equals(status);
    }

    private String normalizeCreateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_DRAFT;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_DRAFT.equals(normalized) || STATUS_PUBLISHED.equals(normalized)) {
            return normalized;
        }
        return null;
    }
}

