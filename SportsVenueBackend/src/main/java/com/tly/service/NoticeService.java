package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.Notice;

public interface NoticeService {

    Result<Notice> create(Notice notice);

    Result<Notice> update(Long id, Notice notice);

    Result<Void> delete(Long id);

    Result<Notice> updateStatus(Long id, String status);

    Result<PageResult<Notice>> manageList(String keyword, String status, long pageNo, long pageSize);

    Result<Notice> manageDetail(Long id);

    Result<PageResult<Notice>> userList(String keyword, long pageNo, long pageSize);

    Result<Notice> userDetail(Long id);
}

