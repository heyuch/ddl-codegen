package hyc.codegen.core.gen;

import java.nio.file.Path;

import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.model.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MapperGenerator golden 契约测试：主路径（CRUD + 唯一键单值 + 多列最左前缀拆分 + 多列 List）、
 * 无主键、无唯一键/索引三形态；期望产物见 {@code fixtures/gen/mapper/**}。
 */
class MapperGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    @Test
    void crudWithUniqueAndMultiColumnSplits() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = mapperConfig(support);
        // 普通索引经 create 内联不进入模型（解析现状），用 alter 加索引驱动 findBy* 派生
        Schema schema = new Schema();
        support.generate(config, schema, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    name varchar(50) not null comment '用户名',\n"
                + "    status int not null comment '状态',\n"
                + "    region varchar(20) not null comment '区域',\n"
                + "    level tinyint not null comment '级别',\n"
                + "    ext varchar(100) comment '忽略 @ignore',\n"
                + "    primary key (id),\n"
                + "    unique key uk_name (name)\n"
                + ") comment '用户表'");
        support.generate(config, schema, "alter table t_user add index idx_status (status)");
        support.generate(config, schema, "alter table t_user add index idx_region_level (region, level)");
        support.assertGoldenSubset("mapper/crud-finders", "com/demo/mapper/UserMapper.java");
    }

    /** 配置：po（target 引用存在即可，其生成器不注册）+ mapper(target=po)。 */
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
        support.generate(mapperConfig(support), "create table t_user (\n"
                + "    id bigint not null comment '逻辑 id',\n"
                + "    name varchar(50) not null comment '用户名',\n"
                + "    amount decimal(10,2) comment '金额'\n"
                + ") comment '无主键表'");
        support.assertGoldenSubset("mapper/no-primary-key", "com/demo/mapper/UserMapper.java");

        String mapper = support.readGenerated("com/demo/mapper/UserMapper.java");
        assertTrue(mapper.contains("int insert(UserPo userPo)"), mapper);
        assertTrue(mapper.contains("int update(UserPo userPo)"), mapper);
        assertFalse(mapper.contains("deleteById"), mapper);
        assertFalse(mapper.contains("findBy"), mapper);
    }

    @Test
    void onlyPrimaryKeyNoSecondaryIndexes() throws Exception {
        GeneratorTestSupport support = support();
        support.generate(mapperConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    name varchar(50) not null comment '用户名',\n"
                + "    note varchar(100) not null comment '备注',\n"
                + "    primary key (id)\n"
                + ") comment '仅主键表'");
        support.assertGoldenSubset("mapper/primary-key-only", "com/demo/mapper/UserMapper.java");
    }

    private GeneratorTestSupport support() {
        // mapper 引用 po（target）；CodeGenerator 要求被引用产物的生成器已注册 → 同注册 PojoGenerator
        return new GeneratorTestSupport(temp, new MapperGenerator(), new PojoGenerator());
    }

}
