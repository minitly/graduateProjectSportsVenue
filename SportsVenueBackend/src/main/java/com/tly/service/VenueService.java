package com.tly.service;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.Venue;

public interface VenueService {

    Result<Venue> create(Venue venue);

    Result<Venue> update(Long id, Venue venue);

    Result<Void> delete(Long id);

    Result<Venue> getById(Long id);

    Result<PageResult<Venue>> query(String type, String status, String keyword, long pageNo, long pageSize);
}

