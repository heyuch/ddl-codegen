package hyc.codegen.core.gen;

import java.nio.file.Path;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MybatisRepositoryImplGenerator golden 契约测试：{@code di=field}（@Resource）与
 * {@code di=constructor} 两注入形态、converter 桥接、converter 缺失/不一致错误路径。
 * DDL 输入见 {@code fixtures/gen/repository-impl/<case>/input}，期望产物见同 case 的 {@code expected}。
 */
class MybatisRepositoryImplGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    @Test
    void constructorDiInjectsMapperAndConverter() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(fullConfig(support, "constructor"), "repository-impl/constructor-di",
                "com/demo/repository/impl/UserRepositoryImpl.java");

        String impl = support.readGenerated("com/demo/repository/impl/UserRepositoryImpl.java");
        assertTrue(impl.contains("public UserRepositoryImpl(UserMapper userMapper, "
                + "UserConverter userConverter)"), impl);
        assertFalse(impl.contains("@Resource"), impl);
    }

    @Test
    void converterMissingWhenMapperTargetDiffersFails() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = support.tableConfig();
        support.addArtifact(config, "entity", "pojo", "com.demo.entity", "");
        support.addArtifact(config, "enum", "enum", "com.demo.enums", "");
        support.addArtifact(config, "po", "pojo", "com.demo.pojo", "Po");
        support.addArtifact(config, "mapper", "mybatisMapper", "com.demo.mapper", "Mapper").setTarget("po");
        support.addArtifact(config, "repository", "repository", "com.demo.repository", "Repository")
                .setTarget("entity");
        // 无 converter 产物、mapper.target(po) != impl.target(entity) → 报错
        support.addArtifact(config, "repositoryImpl", "mybatisRepositoryImpl",
                "com.demo.repository.impl", "RepositoryImpl").setTarget("entity");
        assertThrows(IllegalStateException.class, () -> support.generate(config,
                "create table t_user (id bigint not null auto_increment comment '主键',"
                        + " name varchar(50) not null comment '用户名', primary key (id),"
                        + " unique key uk_name (name))"));
    }

    @Test
    void converterTargetMismatchFails() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = fullConfig(support, "field");
        // converter.target 与 impl.target(entity) 不一致 → 校验报错
        support.artifact(config, "entityConverter").setTarget("po");
        assertThrows(IllegalStateException.class, () -> support.generate(config,
                "create table t_user (id bigint not null auto_increment comment '主键',"
                        + " name varchar(50) not null comment '用户名', primary key (id),"
                        + " unique key uk_name (name))"));
    }

    @Test
    void fieldDiBridgeWithConverter() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(fullConfig(support, "field"), "repository-impl/field-di",
                "com/demo/repository/impl/UserRepositoryImpl.java");

        String impl = support.readGenerated("com/demo/repository/impl/UserRepositoryImpl.java");
        assertTrue(impl.contains("@Resource"), impl);
        assertFalse(impl.contains("public UserRepositoryImpl("), impl);
        // 唯一键 findById/findByName → 单值 converter 桥接（临时变量空安全守卫）；无普通索引故无 toUserList
        assertTrue(impl.contains("UserPo po = userMapper.findById(id);"), impl);
        assertTrue(impl.contains("return po == null ? null : userConverter.toUser(po);"), impl);
        assertTrue(impl.contains("UserPo po = userMapper.findByName(name);"), impl);
        assertFalse(impl.contains("toUserList"), impl);
        assertFalse(impl.contains("findByStatus"), impl);
    }

    /** 标准全链路配置（entity 视图 / po 视图 / mapper / repository / converter / repositoryImpl）。 */
    private DdlConfig fullConfig(GeneratorTestSupport support, String di) {
        DdlConfig config = support.tableConfig();
        ArtifactConfig entity = support.addArtifact(config, "entity", "pojo", "com.demo.entity", "");
        entity.putOption("enums", "true");
        support.addArtifact(config, "enum", "enum", "com.demo.enums", "");
        support.addArtifact(config, "po", "pojo", "com.demo.pojo", "Po");
        support.addArtifact(config, "mapper", "mybatisMapper", "com.demo.mapper", "Mapper").setTarget("po");
        support.addArtifact(config, "repository", "repository", "com.demo.repository", "Repository")
                .setTarget("entity");
        ArtifactConfig converter = support.addArtifact(config, "entityConverter", "converter",
                "com.demo.converter", "Converter");
        converter.setSource("po");
        converter.setTarget("entity");
        ArtifactConfig impl = support.addArtifact(config, "repositoryImpl", "mybatisRepositoryImpl",
                "com.demo.repository.impl", "RepositoryImpl");
        impl.setTarget("entity");
        impl.putOption("mapper", "mapper");
        impl.putOption("converter", "entityConverter");
        impl.putOption("di", di);
        return config;
    }

    private GeneratorTestSupport support() {
        // 全链路 kind 注册（impl 引用 mapper/repository/converter，其产物会一并生成；golden 子集断言 impl）
        return new GeneratorTestSupport(temp,
                new MybatisRepositoryImplGenerator(), new PojoGenerator(), new MapperGenerator(),
                new RepositoryGenerator(), new ConverterGenerator());
    }

}
