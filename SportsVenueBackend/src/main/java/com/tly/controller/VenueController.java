package com.tly.controller;

import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.dto.venue.VenueDeleteRequest;
import com.tly.entity.Venue;
import com.tly.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/venues")
public class VenueController {

    @Autowired
    private VenueService venueService;

    /**
     * 场地基本信息录入：POST /sportsVenue/venues
     */
    @PostMapping
    public Result<Venue> create(@Valid @RequestBody Venue venue) {
        return venueService.create(venue);
    }

    /**
     * 场地信息修改：PUT /sportsVenue/venues/{id}
     */
    @PutMapping("/{id}")
    public Result<Venue> update(@PathVariable("id") Long id, @Valid @RequestBody Venue venue) {
        return venueService.update(id, venue);
    }

    /**
     * 场地信息删除：DELETE /sportsVenue/venues/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id,
                               @RequestBody(required = false) VenueDeleteRequest body) {
        String adminPassword = body != null ? body.getAdminPassword() : null;
        return venueService.delete(id, adminPassword);
    }

    /**
     * 场地详情查看：GET /sportsVenue/venues/{id}
     */
    @GetMapping("/{id}")
    public Result<Venue> getById(@PathVariable("id") Long id) {
        return venueService.getById(id);
    }

    /**
     * 场地分类查询：GET /sportsVenue/venues
     */
    @GetMapping
    public Result<PageResult<Venue>> query(@RequestParam(value = "type", required = false) String type,
                                           @RequestParam(value = "status", required = false) String status,
                                           @RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "pageNo", required = false, defaultValue = "1") long pageNo,
                                           @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize) {
        return venueService.query(type, status, keyword, pageNo, pageSize);
    }
}

