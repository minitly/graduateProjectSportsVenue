package com.tly.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.auth.UserContext;
import com.tly.entity.SysUser;
import com.tly.entity.Venue;
import com.tly.mapper.SysUserMapper;
import com.tly.mapper.VenueMapper;
import com.tly.service.BookingService;
import com.tly.service.VenueService;
import com.tly.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VenueServiceImpl implements VenueService {
    private static final DateTimeFormatter CODE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BookingService bookingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result<Venue> create(Venue venue) {
        if (!StringUtils.hasText(venue.getName()) || !StringUtils.hasText(venue.getCode())
                || !StringUtils.hasText(venue.getType()) || !StringUtils.hasText(venue.getStatus())) {
            return Result.fail(400, "场地名称、编号、类型和状态为必填项");
        }

        // 场地编号唯一性校验（避免数据库 unique 约束异常导致返回 500）
        Venue byCode = venueMapper.selectByCode(venue.getCode());
        if (byCode != null) {
            return Result.fail(400, "场地编号已存在");
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
    @Transactional(rollbackFor = Exception.class)
    public Result<Venue> update(Long id, Venue venue) {

        Venue exists = venueMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "场地不存在");
        }

        if (!StringUtils.hasText(venue.getName()) || !StringUtils.hasText(venue.getCode())
                || !StringUtils.hasText(venue.getType()) || !StringUtils.hasText(venue.getStatus())) {
            return Result.fail(400, "场地名称、编号、类型和状态为必填项");
        }

        // 场地编号唯一性校验（update 允许修改 code，但必须不与其他场地冲突）
        Venue byCode = venueMapper.selectByCode(venue.getCode());
        if (byCode != null && !byCode.getId().equals(id)) {
            return Result.fail(400, "场地编号已存在");
        }

        String oldStatus = exists.getStatus();
        String newStatus = venue.getStatus();

        normalizeImageUrlsForSave(venue);
        venueMapper.update(venue);

        // 场地从可用变更为不可用时，联动取消该场地所有申请中的预约
        if ("AVAILABLE".equalsIgnoreCase(oldStatus) && ! "AVAILABLE".equalsIgnoreCase(newStatus)) {
            String reason;
            String remark;
            if ("DISABLED".equalsIgnoreCase(newStatus)) {
                reason = "VENUE_DISABLED";
                remark = "场地停用";
            } else if ("MAINTAIN".equalsIgnoreCase(newStatus)) {
                reason = "VENUE_MAINTAIN";
                remark = "场地维护";
            } else if ("SUSPEND".equalsIgnoreCase(newStatus)) {
                reason = "VENUE_SUSPEND";
                remark = "暂停预约";
            } else {
                reason = "VENUE_DISABLED";
                remark = "场地不可用";
            }
            bookingService.cancelAppliedByVenueStatusChange(id, reason, remark);
        }

        Venue dbVenue = venueMapper.selectById(id);
        normalizeImageUrlsForRead(dbVenue);
        return Result.success("更新场地成功", dbVenue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id, String adminPassword) {
        UserContext.CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        if (!"OWNER".equalsIgnoreCase(currentUser.getRole()) && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return Result.fail(403, "无权限删除场地");
        }
        if (!StringUtils.hasText(adminPassword)) {
            return Result.fail(400, "adminPassword不能为空");
        }

        SysUser admin = sysUserMapper.findByUsername("admin");
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            return Result.fail(400, "系统 ADMIN 账号不存在，请先初始化管理员");
        }
        if (!PasswordUtil.matches(adminPassword, admin.getPassword())) {
            return Result.fail(403, "ADMIN 密码错误，禁止删除场地");
        }

        Venue exists = venueMapper.selectById(id);
        if (exists == null) {
            return Result.fail(404, "场地不存在");
        }

        // 删除场地前联动取消该场地所有申请中的预约，并释放占用时段
        bookingService.cancelAppliedByVenueStatusChange(id, "VENUE_DISABLED", "场地删除");

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

    @Override
    public Result<Map<String, String>> generateCode() {
        // 编号规则：V + yyyyMMddHHmmss + 3位随机数（不足补0）
        // 再通过数据库查重兜底，确保返回可用编号。
        for (int i = 0; i < 20; i++) {
            String timePart = LocalDateTime.now().format(CODE_TIME_FORMATTER);
            int random = ThreadLocalRandom.current().nextInt(0, 1000);
            String candidate = "V" + timePart + String.format("%03d", random);
            if (venueMapper.selectByCode(candidate) == null) {
                Map<String, String> data = new HashMap<>();
                data.put("code", candidate);
                return Result.success("生成场地编号成功", data);
            }
        }
        return Result.fail(500, "生成场地编号失败，请重试");
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

