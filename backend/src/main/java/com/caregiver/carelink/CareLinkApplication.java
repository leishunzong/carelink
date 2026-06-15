package com.caregiver.carelink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 护联系统启动类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@SpringBootApplication
@MapperScan("com.caregiver.carelink.mapper")
@EnableScheduling
public class CareLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareLinkApplication.class, args);
        System.out.println("========================================");
        System.out.println("护联系统启动成功！");
        System.out.println("接口文档地址：http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}
