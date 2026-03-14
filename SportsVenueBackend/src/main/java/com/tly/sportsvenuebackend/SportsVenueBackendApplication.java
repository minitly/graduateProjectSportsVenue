package com.tly.sportsvenuebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类：仅负责启动，业务代码统一放在 com.tly.* 包下
 */
@SpringBootApplication(scanBasePackages = "com.tly")
@MapperScan("com.tly.mapper")
public class SportsVenueBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsVenueBackendApplication.class, args);
    }

}
