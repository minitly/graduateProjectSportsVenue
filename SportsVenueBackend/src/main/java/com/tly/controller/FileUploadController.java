package com.tly.controller;

import com.tly.common.Result;
import com.tly.dto.file.FileServeResult;
import com.tly.service.FileStorageService;
import com.tly.dto.file.FileUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传与图片获取：单张/多张走同一接口，返回均为数组。封面等单张场景前端取第一个元素。
 */
@RestController
@RequestMapping("/files")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * POST /sportsVenue/files/upload
     * Content-Type: multipart/form-data
     * 表单字段：file（可传多个，同名 file）, biz（可选，如 venue）
     * 返回 data.urls 数组，单张时长度为 1，封面请取 urls[0]。
     */
    @PostMapping("/upload")
    public Result<FileUploadResult> upload(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam(value = "biz", required = false, defaultValue = "venue") String biz) {
        if (files == null || files.isEmpty()) {
            return Result.fail(400, "未选择文件");
        }
        List<byte[]> bytesList = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                bytesList.add(file.getBytes());
                names.add(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
            } catch (IOException e) {
                return Result.fail(400, "读取文件失败");
            }
        }
        return fileStorageService.saveBatch(bytesList, names, biz);
    }

    /**
     * GET /sportsVenue/files/serve?path=/upload/venue/xxx.jpg&path=/upload/venue/yyy.jpg
     * 支持单张或多张 path，返回 data.images 数组，单张时长度为 1；封面等取 images[0]，content 为 Base64。
     * 统一使用自定义 Result 返回。
     */
    @GetMapping("/serve")
    public Result<FileServeResult> serve(@RequestParam("path") List<String> path) {
        return fileStorageService.getByPaths(path);
    }
}
