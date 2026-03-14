package com.tly.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图片获取接口返回：支持单张/多张，images 为数组，单张时长度为 1。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileServeResult {
    private List<FileServeItem> images;
}

