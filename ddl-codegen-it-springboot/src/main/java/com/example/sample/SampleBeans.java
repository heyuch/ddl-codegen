package com.example.sample;

import com.example.sample.converter.UserConverter;
import com.example.sample.mapper.UserMapper;
import com.example.sample.repository.UserRepository;
import com.example.sample.repository.impl.UserRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 生成物无 Spring 注解：Mapper 走 {@code @MapperScan}，repositoryImpl/converter 在此显式注册 Bean——
 * 演示真实装配（20260906-03）。Bean 间无相互调用，故可关闭 CGLIB 代理并 final 化。
 */
@Configuration(proxyBeanMethods = false)
public final class SampleBeans {

    @Bean
    public UserConverter userConverter() {
        return new UserConverter();
    }

    @Bean
    public UserRepository userRepository(UserMapper userMapper, UserConverter userConverter) {
        return new UserRepositoryImpl(userMapper, userConverter);
    }

}
