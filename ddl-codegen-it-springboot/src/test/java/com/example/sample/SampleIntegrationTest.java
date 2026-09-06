package com.example.sample;

import com.example.sample.entity.User;
import com.example.sample.enums.Kind;
import com.example.sample.enums.Status;
import com.example.sample.mapper.UserMapper;
import com.example.sample.po.UserPo;
import com.example.sample.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 20260906-03 集成验证：真实 Spring Boot + MySQL（Testcontainers）+ MyBatis 下，
 * 由 ddl-codegen 生成的 Mapper/XML/Repository/Converter 能编译、装配并被调用：
 * DB 存 code（Integer/String），经 repository+converter 还原为枚举（getCode/getDesc）。
 * 无 Docker 环境由 {@code disabledWithoutDocker} 自动跳过。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SampleIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8")
            .withDatabaseName("codegen_sample")
            .withUsername("codegen")
            .withPassword("codegen")
            .withInitScript("ddl/schema.sql");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountingInterceptor countingInterceptor;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> MYSQL.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void generatedMapperStoresCodeAndRepositoryRestoresEnum() {
        UserPo po = new UserPo();
        po.setName("alice");
        po.setGender("male");
        po.setStatus(2);
        po.setKind("TRIAL");
        po.setNote("hello");
        int inserted = userMapper.insert(po);
        assertEquals(1, inserted);
        assertNotNull(po.getId());

        // mapper（po 视图）读回：DB 存的是 code
        UserPo loaded = userMapper.findById(po.getId());
        assertEquals(Integer.valueOf(2), loaded.getStatus());
        assertEquals("TRIAL", loaded.getKind());

        // repository（entity 视图）经 converter 空安全桥接还原枚举
        User user = userRepository.findById(po.getId());
        assertNotNull(user);
        assertEquals(Status.ACTIVE, user.getStatus());
        assertEquals(Integer.valueOf(2), user.getStatus().getCode());
        assertEquals("活跃", user.getStatus().getDesc());
        assertEquals(Kind.TRIAL, user.getKind());
        assertEquals("alice", user.getName());
    }

    /**
     * 20260906-13 cache-aside 行为验证（内存缓存）：
     * 单值查询 null 入缓存（防穿透，二次不落库）；insert 回填行 evict 清掉负条目；
     * update 老行+新值 evict；deleteById 老行 evict——写后对应 key 再查重新落库。
     */
    @Test
    void repositorySpringCacheHitsAndEvicts() {
        countingInterceptor.reset();
        String name = "cache-" + System.nanoTime();

        // NULL 防穿透：两次查不存在的 name 只落库一次（null 已缓存）
        assertNull(userRepository.findByName(name));
        assertNull(userRepository.findByName(name));
        assertEquals(1, countingInterceptor.count("findByName"), "null 应入缓存，二次查不落库");

        // insert（repository 写路径）→ 回填行 evict 清掉负条目 → 再查重新落库且命中行
        User user = User.builder().name(name).status(Status.ACTIVE).kind(Kind.TRIAL).build();
        assertEquals(1, userRepository.insert(user));
        User withId = userRepository.findByName(name);
        assertNotNull(withId, "insert 后负条目被清，findByName 应重新落库并命中");
        assertEquals(2, countingInterceptor.count("findByName"));

        // update（老行+新值 evict）：旧名/新名两侧缓存均失效
        withId.setName(name + "-renamed");
        assertEquals(1, userRepository.update(withId));
        countingInterceptor.reset();
        assertNull(userRepository.findByName(name), "update 后旧名 key 应失效，重新落库");
        assertNotNull(userRepository.findByName(name + "-renamed"));
        assertEquals(2, countingInterceptor.selectCount());

        // deleteById（老行 evict）：删后该名 key 再查重新落库为 null
        User latest = userRepository.findByName(name + "-renamed");
        assertEquals(1, userRepository.deleteById(latest.getId()));
        countingInterceptor.reset();
        assertNull(userRepository.findByName(name + "-renamed"), "deleteById 后 key 应失效，重新落库");
        assertEquals(1, countingInterceptor.selectCount());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CountingConfig {

        @Bean
        CountingInterceptor countingInterceptor() {
            return new CountingInterceptor();
        }

    }

}
