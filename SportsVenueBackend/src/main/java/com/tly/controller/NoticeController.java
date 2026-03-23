package com.tly.controller;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.notice.NoticeStatusUpdateRequest;
import com.tly.entity.Notice;
import com.tly.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * OWNER 新建公告（可存草稿 / 可直接发布）：POST /sportsVenue/notices
     */
    @PostMapping
    public Result<Notice> create(@RequestBody Notice notice) {
        return noticeService.create(notice);
    }

    /**
     * OWNER 修改公告：PUT /sportsVenue/notices/{id}
     */
    @PutMapping("/{id}")
    public Result<Notice> update(@PathVariable("id") Long id, @RequestBody Notice notice) {
        return noticeService.update(id, notice);
    }

    /**
     * OWNER 删除公告（逻辑删除）：DELETE /sportsVenue/notices/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        return noticeService.delete(id);
    }

    /**
     * OWNER 发布/下线公告（状态切换）：PUT /sportsVenue/notices/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<Notice> updateStatus(@PathVariable("id") Long id,
                                       @RequestBody(required = false) NoticeStatusUpdateRequest body) {
        String status = body != null ? body.getStatus() : null;
        return noticeService.updateStatus(id, status);
    }

    /**
     * OWNER 公告管理分页查询（标题 + 状态）：GET /sportsVenue/notices/manage
     */
    @GetMapping("/manage")
    public Result<PageResult<Notice>> manageList(@RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "status", required = false) String status,
                                                 @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                                 @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return noticeService.manageList(keyword, status, pageNo, pageSize);
    }

    /**
     * OWNER 公告管理详情（按 ID）：GET /sportsVenue/notices/manage/{id}
     */
    @GetMapping("/manage/{id}")
    public Result<Notice> manageDetail(@PathVariable("id") Long id) {
        return noticeService.manageDetail(id);
    }

    /**
     * 用户公告分页列表（仅已发布）：GET /sportsVenue/notices
     */
    @GetMapping
    public Result<PageResult<Notice>> userList(@RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                               @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return noticeService.userList(keyword, pageNo, pageSize);
    }

    /**
     * 用户公告详情（仅已发布）：GET /sportsVenue/notices/{id}
     */
    @GetMapping("/{id}")
    public Result<Notice> userDetail(@PathVariable("id") Long id) {
        return noticeService.userDetail(id);
    }
}

