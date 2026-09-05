package hyc.codegen.core.gen;

import java.nio.file.Path;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConverterGenerator golden 契约测试：枚举一端视图桥接（nullSafe fromCode/getCode）与纯标量直拷
 * 双向 toX/toXList 整文件。期望产物见 {@code fixtures/gen/converter/**}。
 */
class ConverterGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    /** entity(enums=true) + enum + po + entityConverter(source=po,target=entity)。 */
    private DdlConfig converterConfig(GeneratorTestSupport support) {
        DdlConfig config = support.tableConfig();
        ArtifactConfig entity = support.addArtifact(config, "entity", "pojo", "com.demo.entity", "");
        entity.putOption("enums", "true");
        support.addArtifact(config, "enum", "enum", "com.demo.enums", "");
        support.addArtifact(config, "po", "pojo", "com.demo.pojo", "Po");
        ArtifactConfig converter = support.addArtifact(config, "entityConverter", "converter",
                "com.demo.converter", "Converter");
        converter.setSource("po");
        converter.setTarget("entity");
        return config;
    }

    @Test
    void enumBridgeBothDirections() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = converterConfig(support);
        support.generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint unsigned not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) @enum:Status',\n"
                + "    name varchar(50) comment '用户名',\n"
                + "    primary key (id))");
        support.assertGoldenSubset("converter/enum-bridge", "com/demo/converter/UserConverter.java");

        String converter = support.readGenerated("com/demo/converter/UserConverter.java");
        // 标量(po) → 枚举(entity)：nullSafe + fromCode；枚举 → 标量：nullSafe + getCode()
        assertTrue(converter.contains(
                "user.setStatus(source.getStatus() == null ? null : Status.fromCode(source.getStatus()));"),
                converter);
        assertTrue(converter.contains(
                "userPo.setStatus(target.getStatus() == null ? null : target.getStatus().getCode());"),
                converter);
        // 普通列直拷 + List 空安全守卫
        assertTrue(converter.contains("user.setName(source.getName());"), converter);
        assertTrue(converter.contains("if (sourceList != null) {"), converter);
    }

    @Test
    void plainColumnsCopiedDirectly() throws Exception {
        GeneratorTestSupport support = support();
        support.generate(converterConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    name varchar(50) not null comment '用户名',\n"
                + "    note varchar(100) comment '备注',\n"
                + "    primary key (id))");
        support.assertGoldenSubset("converter/plain", "com/demo/converter/UserConverter.java");

        String converter = support.readGenerated("com/demo/converter/UserConverter.java");
        assertTrue(converter.contains("user.setName(source.getName());"), converter);
        assertFalse(converter.contains("Status."), converter);
        assertTrue(converter.contains("toUserList"), converter);
    }

    private GeneratorTestSupport support() {
        // converter 经 pojo fieldType 计算视图（source/target 产物同注册生成）
        return new GeneratorTestSupport(temp,
                new ConverterGenerator(), new PojoGenerator(), new EnumGenerator());
    }

}
