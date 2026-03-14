package com.tly.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.Venue;
import com.tly.mapper.VenueMapper;
import com.tly.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class VenueServiceImpl implements VenueService {

    @Autowired
    private VenueMapper venueMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result<Venue> create(Venue venue) {
        if (!StringUtils.hasText(venue.getName()) || !StringUtils.hasText(venue.getCode())
                || !StringUtils.hasText(venue.getType()) || !StringUtils.hasText(venue.getStatus())) {
            return Result.fail(400, "场地名称、编号、类型和状态为必填项");
        }

        // imageUrls 转为 JSON 字符串存库
        normalizeImageUrlsForSave(venue);

        venueMapper.insert(venue);
        // 查询一次确保返回数据库中的最终数据
        Venue dbVenue = venueMapper.selectById(venue.getId());
        normalizeImageUrlsForRead(dbVenue);
        return Result.success("新增场地成功", dbVenue);
    }

    @Override
    public Result<Venue> update(Long id, Venue venue) {
        if (id == null || venue.getId() == null || !id.equals(venue.getId())) {
            return Result.fail(400, "路径ID与请求体ID不一致");
        }

        Venue exists = venueMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "场地不存在");
        }

        if (!StringUtils.hasText(venue.getName()) || !StringUtils.hasText(venue.getCode())
                || !StringUtils.hasText(venue.getType()) || !StringUtils.hasText(venue.getStatus())) {
            return Result.fail(400, "场地名称、编号、类型和状态为必填项");
        }

        normalizeImageUrlsForSave(venue);
        venueMapper.update(venue);

        Venue dbVenue = venueMapper.selectById(id);
        normalizeImageUrlsForRead(dbVenue);
        return Result.success("更新场地成功", dbVenue);
    }

    @Override
    public Result<Void> delete(Long id) {
        Venue exists = venueMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "场地不存在");
        }
        venueMapper.deleteById(id);
        return Result.success("删除场地成功", null);
    }

    @Override
    public Result<Venue> getById(Long id) {
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            return Result.fail(404, "场地不存在");
        }
        normalizeImageUrlsForRead(venue);
        return Result.success("查询成功", venue);
    }

    @Override
    public Result<PageResult<Venue>> query(String type, String status, String keyword, long pageNo, long pageSize) {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        long offset = (pageNo - 1) * pageSize;

        long total = venueMapper.countByCondition(type, status, keyword);
        List<Venue> records;
        if (total == 0) {
            records = Collections.emptyList();
        } else {
            records = venueMapper.listByCondition(type, status, keyword, offset, pageSize);
            for (Venue v : records) {
                normalizeImageUrlsForRead(v);
            }
        }

        PageResult<Venue> pageResult = new PageResult<>(total, pageNo, pageSize, records);
        return Result.success("查询成功", pageResult);
    }

    /**
     * 将 List<String> imageUrls 序列化为 JSON 写入 imageUrlsJson，供 MyBatis 存库
     */
    private void normalizeImageUrlsForSave(Venue venue) {
        try {
            List<String> list = venue.getImageUrls();
            if (list == null || list.isEmpty()) {
                venue.setImageUrlsJson(null);
            } else {
                venue.setImageUrlsJson(objectMapper.writeValueAsString(list));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("图片URL序列化失败", e);
        }
    }

    /**
     * 将 imageUrlsJson 解析为 List<String> 写入 imageUrls
     */
    private void normalizeImageUrlsForRead(Venue venue) {
        if (venue == null) {
            return;
        }
        String json = venue.getImageUrlsJson();
        if (!StringUtils.hasText(json)) {
            venue.setImageUrls(Collections.emptyList());
            return;
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            venue.setImageUrls(list);
        } catch (Exception e) {
            venue.setImageUrls(Collections.emptyList());
        }
    }
}

