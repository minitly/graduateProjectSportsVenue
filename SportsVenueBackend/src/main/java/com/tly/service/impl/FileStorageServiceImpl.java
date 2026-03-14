package com.tly.service.impl;

import com.tly.common.Result;
import com.tly.config.FileUploadProperties;
import com.tly.dto.file.FileServeItem;
import com.tly.dto.file.FileServeResult;
import com.tly.service.FileStorageService;
import com.tly.dto.file.FileUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private FileUploadProperties fileUploadProperties;

    @Override
    public Result<FileUploadResult> saveBatch(List<byte[]> fileBytesList, List<String> originalFilenames, String biz) {
        if (fileBytesList == null || fileBytesList.isEmpty()) {
            return Result.fail(400, "未选择文件或文件为空");
        }
        if (originalFilenames == null || originalFilenames.size() != fileBytesList.size()) {
            return Result.fail(400, "文件与文件名数量不一致");
        }
        String bizSafe = StringUtils.hasText(biz) ? biz.trim() : "default";
        bizSafe = bizSafe.replaceAll("[^a-zA-Z0-9_-]", "_");
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < fileBytesList.size(); i++) {
            byte[] fileBytes = fileBytesList.get(i);
            String originalFilename = originalFilenames.get(i);
            if (fileBytes == null || fileBytes.length == 0) {
                return Result.fail(400, "第" + (i + 1) + "个文件为空");
            }
            String ext = getExtension(originalFilename);
            if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
                return Result.fail(400, "仅支持图片格式：jpg、jpeg、png、gif、webp，第" + (i + 1) + "个文件格式不合法");
            }
            String dateDir = LocalDate.now().format(DATE_FORMAT);
            String fileName = bizSafe + "_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(10000, 99999) + "." + ext;
            Path relativePath = Path.of(bizSafe, dateDir, fileName);
            Path absolutePath = fileUploadProperties.getUploadRootPath().resolve(relativePath);
            try {
                Files.createDirectories(absolutePath.getParent());
                Files.write(absolutePath, fileBytes);
            } catch (IOException e) {
                return Result.fail(500, "文件保存失败：" + e.getMessage());
            }
            urls.add("/upload/" + relativePath.toString().replace("\\", "/"));
        }
        return Result.success("上传成功", new FileUploadResult(urls));
    }

    @Override
    public Result<FileServeResult> getByPaths(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return Result.fail(400, "path 不能为空");
        }
        List<FileServeItem> images = new ArrayList<>();
        for (String path : paths) {
            if (!StringUtils.hasText(path) || !path.startsWith("/upload/")) {
                return Result.fail(400, "path 必须以 /upload/ 开头");
            }
            String relative = path.substring("/upload/".length()).trim();
            if (relative.isEmpty() || relative.contains("..")) {
                return Result.fail(400, "path 非法");
            }
            Path absolutePath = fileUploadProperties.getUploadRootPath().resolve(relative).normalize();
            Path root = fileUploadProperties.getUploadRootPath().toAbsolutePath().normalize();
            if (!absolutePath.startsWith(root)) {
                return Result.fail(403, "path 越界");
            }
            if (!Files.isRegularFile(absolutePath)) {
                return Result.fail(404, "文件不存在：" + path);
            }
            String ext = getExtension(absolutePath.getFileName().toString());
            String contentType = getContentType(ext);
            try {
                byte[] content = Files.readAllBytes(absolutePath);
                String contentBase64 = Base64.getEncoder().encodeToString(content);
                images.add(new FileServeItem(path, contentType, contentBase64));
            } catch (IOException e) {
                return Result.fail(500, "读取文件失败");
            }
        }
        return Result.success("OK", new FileServeResult(images));
    }

    private String getContentType(String ext) {
        if (ext == null) return "application/octet-stream";
        switch (ext.toLowerCase(Locale.ROOT)) {
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png":  return "image/png";
            case "gif":  return "image/gif";
            case "webp": return "image/webp";
            default:     return "application/octet-stream";
        }
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return null;
        }
        return filename.substring(i + 1);
    }
}
