package com.tly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileUploadProperties {

    /**
     * 上传文件保存根目录（相对或绝对路径）
     */
    private String uploadDir = "upload";

    /**
     * 获取根目录的 Path（若为相对路径则基于当前工作目录）
     */
    public Path getUploadRootPath() {
        Path p = Paths.get(uploadDir);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir")).resolve(p);
        }
        return p.normalize();
    }
}
