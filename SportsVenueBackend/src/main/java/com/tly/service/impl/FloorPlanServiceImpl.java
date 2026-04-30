package com.tly.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tly.auth.UserContext;
import com.tly.common.PageResult;
import com.tly.common.Result;
import com.tly.entity.FloorPlan;
import com.tly.entity.FloorPlanItem;
import com.tly.mapper.FloorPlanMapper;
import com.tly.service.FloorPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FloorPlanServiceImpl implements FloorPlanService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    @Autowired
    private FloorPlanMapper floorPlanMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        ParsedContent parsedContent;
        try {
            parsedContent = parseAndSplitContent(request.getContentJson());
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        }

        FloorPlan toSave = new FloorPlan();
        toSave.setTitle(request.getTitle().trim());
        toSave.setDescription(trimOrNull(request.getDescription()));
        toSave.setStatus(status);
        toSave.setContentJson(parsedContent.canvasContentJson);

        floorPlanMapper.insert(toSave);
        try {
            syncFloorPlanItems(toSave.getId(), parsedContent.items);
        } catch (IllegalStateException ex) {
            return Result.fail(400, ex.getMessage());
        }
        FloorPlan db = floorPlanMapper.selectById(toSave.getId());
        hydrateContentJson(db);
        return Result.success("新增成功", db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        ParsedContent parsedContent;
        try {
            parsedContent = parseAndSplitContent(request.getContentJson());
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        }

        exists.setTitle(request.getTitle().trim());
        exists.setDescription(trimOrNull(request.getDescription()));
        exists.setStatus(status);
        exists.setContentJson(parsedContent.canvasContentJson);
        floorPlanMapper.update(exists);
        try {
            syncFloorPlanItems(id, parsedContent.items);
        } catch (IllegalStateException ex) {
            return Result.fail(400, ex.getMessage());
        }

        FloorPlan db = floorPlanMapper.selectById(id);
        hydrateContentJson(db);
        return Result.success("修改成功", db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        List<FloorPlanItem> items = floorPlanMapper.listItemsByFloorPlanId(id);
        List<String> boundLabels = new ArrayList<>();
        for (FloorPlanItem item : items) {
            if (item.getVenueId() != null) {
                boundLabels.add(StringUtils.hasText(item.getLabel()) ? item.getLabel() : item.getItemUid());
            }
        }
        if (!boundLabels.isEmpty()) {
            return Result.fail(400, "存在已绑定场地的区域，无法删除："
                    + String.join("，", boundLabels)
                    + "。请先解绑后再删除。");
        }
        floorPlanMapper.logicalDelete(id);
        floorPlanMapper.deleteItemsByFloorPlanId(id);
        floorPlanMapper.deleteItemRelationsByFloorPlanId(id);
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
        for (FloorPlan record : records) {
            hydrateContentJson(record);
        }
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
        hydrateContentJson(floorPlan);
        return Result.success("查询成功", floorPlan);
    }

    @Override
    public Result<List<FloorPlanItem>> listSelectableItems(Long floorPlanId, Long venueId) {
        Result<Void> roleCheck = requireOwnerOrAdmin("无权限查询场地图item");
        if (roleCheck != null) {
            return Result.fail(roleCheck.getCode(), roleCheck.getMessage());
        }
        if (floorPlanId == null) {
            return Result.fail(400, "floorPlanId不能为空");
        }
        FloorPlan fp = floorPlanMapper.selectById(floorPlanId);
        if (fp == null) {
            return Result.fail(404, "场地图不存在");
        }
        List<FloorPlanItem> items = floorPlanMapper.listSelectableItemsByFloorPlanId(floorPlanId, venueId);
        return Result.success("查询成功", items);
    }

    private void syncFloorPlanItems(Long floorPlanId, List<FloorPlanItem> incomingItems) {
        List<FloorPlanItem> existingItems = floorPlanMapper.listItemsByFloorPlanId(floorPlanId);
        Map<String, FloorPlanItem> existingByUid = new HashMap<>();
        for (FloorPlanItem item : existingItems) {
            if (StringUtils.hasText(item.getItemUid())) {
                existingByUid.put(item.getItemUid(), item);
            }
        }

        Map<String, FloorPlanItem> incomingByUid = new HashMap<>();
        for (FloorPlanItem item : incomingItems) {
            if (StringUtils.hasText(item.getItemUid())) {
                incomingByUid.put(item.getItemUid(), item);
            }
        }

        List<String> forbidden = new ArrayList<>();
        List<FloorPlanItem> toDelete = new ArrayList<>();
        for (FloorPlanItem oldItem : existingItems) {
            if (!incomingByUid.containsKey(oldItem.getItemUid())) {
                if (oldItem.getVenueId() != null) {
                    forbidden.add(StringUtils.hasText(oldItem.getLabel()) ? oldItem.getLabel() : oldItem.getItemUid());
                } else {
                    toDelete.add(oldItem);
                }
            }
        }
        if (!forbidden.isEmpty()) {
            throw new IllegalStateException("已绑定场地的区域不可删除："
                    + String.join("，", forbidden)
                    + "。请先解绑后再删除。");
        }

        // 清空旧关系，按新顺序重建
        floorPlanMapper.deleteItemRelationsByFloorPlanId(floorPlanId);

        for (FloorPlanItem incoming : incomingItems) {
            FloorPlanItem old = existingByUid.get(incoming.getItemUid());
            if (old != null) {
                incoming.setId(old.getId());
                incoming.setVenueId(old.getVenueId());
                floorPlanMapper.updateFloorPlanItem(incoming);
                floorPlanMapper.insertFloorPlanItemRel(floorPlanId, incoming.getId());
            } else {
                incoming.setVenueId(null);
                floorPlanMapper.insertFloorPlanItem(incoming);
                floorPlanMapper.insertFloorPlanItemRel(floorPlanId, incoming.getId());
            }
        }

        for (FloorPlanItem oldItem : toDelete) {
            floorPlanMapper.deleteFloorPlanItemById(oldItem.getId());
        }
    }

    private void hydrateContentJson(FloorPlan floorPlan) {
        if (floorPlan == null) {
            return;
        }
        String canvasOnly = floorPlan.getContentJson();
        List<FloorPlanItem> items = floorPlanMapper.listItemsByFloorPlanId(floorPlan.getId());
        floorPlan.setContentJson(mergeCanvasAndItems(canvasOnly, items));
    }

    private ParsedContent parseAndSplitContent(String contentJson) {
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            JsonNode canvasNode = root != null ? root.get("canvas") : null;
            if (canvasNode == null || !canvasNode.isObject()) {
                throw new IllegalArgumentException("contentJson缺少canvas对象");
            }

            ObjectNode canvasRoot = objectMapper.createObjectNode();
            canvasRoot.set("canvas", canvasNode);
            String canvasContentJson = objectMapper.writeValueAsString(canvasRoot);

            List<FloorPlanItem> items = new ArrayList<>();
            JsonNode itemsNode = root.get("items");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    if (!itemNode.isObject()) {
                        continue;
                    }
                    FloorPlanItem item = new FloorPlanItem();
                    item.setItemUid(readString(itemNode, "id", generateItemUid()));
                    item.setType(readString(itemNode, "type", "rect"));
                    item.setX(readInt(itemNode, "x", 0));
                    item.setY(readInt(itemNode, "y", 0));
                    item.setW(readInt(itemNode, "w", 200));
                    item.setH(readInt(itemNode, "h", 120));
                    item.setRotation(readInt(itemNode, "rotation", 0));
                    item.setLabel(readNullableString(itemNode, "label"));
                    item.setColor(readNullableString(itemNode, "color"));
                    item.setVenueId(readNullableLong(itemNode, "venueId"));
                    items.add(item);
                }
            }
            return new ParsedContent(canvasContentJson, items);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("contentJson格式不合法");
        }
    }

    private String mergeCanvasAndItems(String canvasOnlyContentJson, List<FloorPlanItem> items) {
        try {
            JsonNode canvasRootNode = objectMapper.readTree(
                    StringUtils.hasText(canvasOnlyContentJson) ? canvasOnlyContentJson : "{\"canvas\":{}}"
            );
            ObjectNode mergedRoot = objectMapper.createObjectNode();
            JsonNode canvasNode = canvasRootNode != null ? canvasRootNode.get("canvas") : null;
            mergedRoot.set("canvas", canvasNode != null && canvasNode.isObject() ? canvasNode : objectMapper.createObjectNode());

            ArrayNode itemArray = objectMapper.createArrayNode();
            for (FloorPlanItem item : items) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("id", item.getItemUid());
                node.put("type", defaultString(item.getType(), "rect"));
                node.put("x", item.getX() == null ? 0 : item.getX());
                node.put("y", item.getY() == null ? 0 : item.getY());
                node.put("w", item.getW() == null ? 200 : item.getW());
                node.put("h", item.getH() == null ? 120 : item.getH());
                node.put("rotation", item.getRotation() == null ? 0 : item.getRotation());
                node.put("label", defaultString(item.getLabel(), ""));
                if (StringUtils.hasText(item.getColor())) {
                    node.put("color", item.getColor());
                }
                if (item.getVenueId() != null) {
                    node.put("venueId", item.getVenueId());
                }
                itemArray.add(node);
            }
            mergedRoot.set("items", itemArray);
            return objectMapper.writeValueAsString(mergedRoot);
        } catch (JsonProcessingException ex) {
            return "{\"canvas\":{},\"items\":[]}";
        }
    }

    private String readString(JsonNode node, String key, String defaultValue) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : defaultValue;
    }

    private String readNullableString(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private Integer readInt(JsonNode node, String key, int defaultValue) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        return value.isNumber() ? value.intValue() : defaultValue;
    }

    private Long readNullableLong(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.longValue() : null;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String generateItemUid() {
        return "item_" + UUID.randomUUID().toString().replace("-", "");
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

    private static class ParsedContent {
        private final String canvasContentJson;
        private final List<FloorPlanItem> items;

        private ParsedContent(String canvasContentJson, List<FloorPlanItem> items) {
            this.canvasContentJson = canvasContentJson;
            this.items = items;
        }
    }
}

