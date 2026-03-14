package com.tly.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片获取接口返回的单条：path、contentType、Base64 内容。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileServeItem {
    private String path;
    private String contentType;
    /** 图片内容，Base64 编码，前端可拼接为 data URL：data:${contentType};base64,${content} */
    private String content;
}
