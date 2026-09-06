package com.example.sample;

import com.example.sample.converter.UserConverter;
import com.example.sample.mapper.UserMapper;
import com.example.sample.repository.UserRepository;
import com.example.sample.repository.impl.UserRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 生成物无 Spring 注解：Mapper 走 {@code @MapperScan}，repositoryImpl/converter 在此显式注册 Bean——
 * 演示真实装配（20260906-03）。@Bean 产物仍经容器后处理器注入/@Lazy 自引用并可按需代理
 * （20260906-13：springCache=true 时 impl 含 @Autowired @Lazy 自引用字段，字段注入对 @Bean 手工 new 同样生效）。
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
