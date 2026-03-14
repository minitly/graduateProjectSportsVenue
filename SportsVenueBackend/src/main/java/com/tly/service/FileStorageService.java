package com.tly.service;

import com.tly.common.Result;
import com.tly.dto.file.FileServeResult;
import com.tly.dto.file.FileUploadResult;

import java.util.List;

/**
 * 文件存储服务：支持单张/多张上传与按 path 单张/多张获取，同一接口。
 */
public interface FileStorageService {

    /**
     * 保存单张或多张上传文件，返回可访问路径数组（单张时 urls 长度为 1）。
     *
     * @param fileBytesList     文件二进制列表
     * @param originalFilenames 原始文件名列表（与 fileBytesList 一一对应，用于取扩展名）
     * @param biz              业务标识，如 venue
     * @return 成功时 data.urls 为 ["/upload/venue/...", ...]
     */
    Result<FileUploadResult> saveBatch(List<byte[]> fileBytesList, List<String> originalFilenames, String biz);

    /**
     * 根据上传返回的 path 列表（单张或多张）读取文件内容，用于前端查询场地后拉取封面/附图。
     *
     * @param paths 以 /upload/ 开头的 path 列表
     * @return 成功时 data.images 为 [{ path, contentType, content(Base64) }, ...]，单张时长度为 1
     */
    Result<FileServeResult> getByPaths(List<String> paths);
}
