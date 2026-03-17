package com.tly.controller;

import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.BorrowRecord;
import com.tly.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 器材借用流程与借用记录查询接口
 */
@RestController
@RequestMapping("/borrows")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    /**
     * 用户发起借用申请：POST /sportsVenue/borrows
     */
    @PostMapping
    public Result<BorrowRecord> apply(@RequestBody BorrowRecord record) {
        return borrowService.apply(record);
    }

    /**
     * 管理者确认借出：PUT /sportsVenue/borrows/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public Result<BorrowRecord> approve(@PathVariable("id") Long id,
                                        @RequestBody(required = false) BorrowRecord body) {
        String conditionOnBorrow = body != null ? body.getConditionOnBorrow() : null;
        String remark = body != null ? body.getRemark() : null;
        return borrowService.approve(id, conditionOnBorrow, remark);
    }

    /**
     * 管理者确认归还：PUT /sportsVenue/borrows/{id}/return
     */
    @PutMapping("/{id}/return")
    public Result<BorrowRecord> confirmReturn(@PathVariable("id") Long id,
                                              @RequestBody(required = false) BorrowRecord body) {
        String conditionOnReturn = body != null ? body.getConditionOnReturn() : null;
        String remark = body != null ? body.getRemark() : null;
        return borrowService.confirmReturn(id, conditionOnReturn, remark);
    }

    /**
     * 借用记录分页查询：GET /sportsVenue/borrows
     */
    @GetMapping
    public Result<PageResult<BorrowRecord>> query(@RequestParam(value = "userId", required = false) Long userId,
                                                  @RequestParam(value = "itemId", required = false) Long itemId,
                                                  @RequestParam(value = "status", required = false) String status,
                                                  @RequestParam(value = "startTime", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                  @RequestParam(value = "endTime", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                  @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                                  @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return borrowService.query(userId, itemId, status, startTime, endTime, pageNo, pageSize);
    }

    /**
     * 当前登录用户的借用记录查询：GET /sportsVenue/borrows/my
     */
    @GetMapping("/my")
    public Result<PageResult<BorrowRecord>> my(@RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "startTime", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                               @RequestParam(value = "endTime", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                               @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                               @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        return borrowService.query(currentUser.getUserId(), null, status, startTime, endTime, pageNo, pageSize);
    }
}

