package hyc.codegen.core.gen;

import java.nio.file.Path;

import hyc.codegen.core.config.DdlConfig;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MapperGenerator golden 契约测试：主路径（CRUD + 唯一键单值 + 多列最左前缀拆分 + List 返回）、
 * 无主键、无唯一键/索引三形态。DDL 输入见 {@code fixtures/gen/mapper/<case>/input}，
 * 期望产物见同 case 的 {@code expected}。
 */
class MapperGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    @Test
    void crudWithUniqueAndMultiColumnSplits() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(mapperConfig(support), "mapper/crud-finders",
                "com/demo/mapper/UserMapper.java");
    }

    /** 配置：po（target 引用存在即可）+ mapper(target=po)。 */
    private DdlConfig mapperConfig(GeneratorTestSupport support) {
        DdlConfig config = support.tableConfig();
        support.addArtifact(config, "po", "pojo", "com.demo.pojo", "Po");
        support.addArtifact(config, "mapper", "mybatisMapper", "com.demo.mapper", "Mapper")
                .setTarget("po");
        return config;
    }

    @Test
    void noPrimaryKeyProducesCrudOnly() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(mapperConfig(support), "mapper/no-primary-key",
                "com/demo/mapper/UserMapper.java");

        String mapper = support.readGenerated("com/demo/mapper/UserMapper.java");
        assertTrue(mapper.contains("int insert(UserPo userPo)"), mapper);
        assertTrue(mapper.contains("int update(UserPo userPo)"), mapper);
        assertFalse(mapper.contains("deleteById"), mapper);
        assertFalse(mapper.contains("findBy"), mapper);
    }

    @Test
    void onlyPrimaryKeyNoSecondaryIndexes() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(mapperConfig(support), "mapper/primary-key-only",
                "com/demo/mapper/UserMapper.java");
    }

    private GeneratorTestSupport support() {
        // mapper 引用 po（target）；CodeGenerator 要求被引用产物的生成器已注册 → 同注册 PojoGenerator
        return new GeneratorTestSupport(temp, new MapperGenerator(), new PojoGenerator());
    }

}
