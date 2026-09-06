package com.example.sample;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 真实 Spring Boot 消费样例（20260906-03）：由 ddl-codegen 生成的 Mapper/Repository/Converter 与
 * 手写样例共存；@MapperScan 覆盖生成包（生成物无 Spring 注解，装配靠显式配置）。
 */
@SpringBootApplication
@MapperScan("com.example.sample.mapper")
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

}
