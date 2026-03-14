package com.tly.config;

import com.tly.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private FileUploadProperties fileUploadProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/error"
                );
    }

    /**
     * 上传文件的访问路径：/upload/** -> 本地 file.upload-dir 目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = fileUploadProperties.getUploadRootPath().toAbsolutePath();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(location);
    }
}

