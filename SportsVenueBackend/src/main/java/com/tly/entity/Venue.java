package com.tly.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Venue {
    private Long id;
    private String name;
    private String code;
    private String type;
    private Integer capacity;
    private BigDecimal price;
    private String openTimeDesc;
    private String description;
    /**
     * AVAILABLE / DISABLED / MAINTAIN / SUSPEND
     */
    private String status;
    private String coverImageUrl;
    /**
     * 数据库 image_urls 列映射为此字段（JSON 字符串），MyBatis 不直接映射 List；不返回给前端
     */
    @JsonIgnore
    private String imageUrlsJson;
    /**
     * 业务使用：图片 URL 列表，由 Service 从 imageUrlsJson 解析/序列化
     */
    private List<String> imageUrls;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

