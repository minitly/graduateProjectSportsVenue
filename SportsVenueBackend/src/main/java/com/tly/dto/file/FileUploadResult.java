package com.tly.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 上传接口返回：url 统一用数组封装，单张时也为长度 1 的数组，封面等场景前端取 urls[0]。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {
    private List<String> urls;
}

