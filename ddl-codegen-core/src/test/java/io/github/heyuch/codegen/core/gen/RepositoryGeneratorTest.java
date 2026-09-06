package io.github.heyuch.codegen.core.gen;

import java.nio.file.Path;

import io.github.heyuch.codegen.core.config.DdlConfig;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RepositoryGenerator golden 契约测试：findBy 单值 {@code @Nullable} vs List、多列最左前缀拆分、
 * 零索引空接口冒烟、target 缺失报错；参数 = repository 自身 ctx 标量视图。DDL 输入见
 * {@code fixtures/gen/repository/<case>/input}，期望产物见同 case 的 {@code expected}。
 */
class RepositoryGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    @Test
    void finderVariantsSingleListAndMultiColumn() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(repositoryConfig(support), "repository/finders",
                "com/demo/repository/UserRepository.java");

        String repo = support.readGenerated("com/demo/repository/UserRepository.java");
        // 唯一全列 → 单值（方法级 @Nullable）；普通/多列索引 → List；参数为自身 ctx 标量视图
        assertTrue(repo.contains("@Nullable"), repo);
        assertTrue(repo.contains("User findById(Long id);"), repo);
        assertTrue(repo.contains("User findByName(String name);"), repo);
        assertTrue(repo.contains("List<User> findByStatus(Integer status);"), repo);
        assertTrue(repo.contains("List<User> findByRegion(String region);"), repo);
        assertTrue(repo.contains("List<User> findByRegionAndLevel(String region, Integer level);"), repo);
    }

    @Test
    void missingTargetArtifactFails() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = support.tableConfig();
        support.addArtifact(config, "repository", "repository", "com.demo.repository", "Repository")
                .setTarget("ghost");
        assertThrows(IllegalStateException.class, () -> support.generate(config,
                "create table t_user (id bigint not null primary key, name varchar(50) not null)"));
    }

    @Test
    void noIndexesProducesEmptyInterface() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(repositoryConfig(support), "repository/no-indexes",
                "com/demo/repository/LogRepository.java");
    }

    private DdlConfig repositoryConfig(GeneratorTestSupport support) {
        DdlConfig config = support.tableConfig();
        support.addArtifact(config, "entity", "pojo", "com.demo.entity", "");
        support.addArtifact(config, "repository", "repository", "com.demo.repository", "Repository")
                .setTarget("entity");
        return config;
    }

    private GeneratorTestSupport support() {
        // repository 引用 entity(target)；被引用 kind(pojo) 需注册
        return new GeneratorTestSupport(temp, new RepositoryGenerator(), new PojoGenerator());
    }

}
