package com.campus.repair;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园报修预约系统后端启动类。
 *
 * @author campus
 */
@SpringBootApplication
@MapperScan("com.campus.repair.mapper")
public class RepairServerApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RepairServerApplication.class, args);
    }
}
