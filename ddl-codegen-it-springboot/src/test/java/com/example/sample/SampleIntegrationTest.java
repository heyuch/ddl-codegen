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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

}
