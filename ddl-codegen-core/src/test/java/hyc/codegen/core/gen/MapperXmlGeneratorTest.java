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
 * MapperXmlGenerator golden 契约测试：主路径整文件（自增主键 insert 排除 + useGeneratedKeys +
 * findById/findBy*）、无主键、非自增主键 insert、缺 path 报错。DDL 输入见
 * {@code fixtures/gen/xml/<case>/input}，期望产物见同 case 的 {@code expected}。
 */
class MapperXmlGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    @Test
    void fullCrudXmlWithAutoIncrementId() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(xmlConfig(support), "xml/full",
                "src/main/resources/mapper/UserMapper.xml");

        String xml = support.readGenerated("src/main/resources/mapper/UserMapper.xml");
        assertTrue(xml.contains("useGeneratedKeys=\"true\""), xml);
        assertFalse(xml.contains("<id property=\"id\" column=\"id\" key"), xml);
        assertTrue(xml.contains("<select id=\"findByRegionAndLevel\""), xml);
    }

    @Test
    void missingPathFails() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = support.tableConfig();
        support.addArtifact(config, "po", "pojo", "com.demo.pojo", "Po");
        support.addArtifact(config, "mapper", "mybatisMapper", "com.demo.mapper", "Mapper").setTarget("po");
        // xml 产物不设 path → 生成时报错
        support.addArtifact(config, "xml", "mybatisXml", null, null).setTarget("po");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> support.generate(config, "create table t_user (\n"
                        + "    id bigint not null auto_increment comment '主键',\n"
                        + "    name varchar(50) not null comment '用户名',\n"
                        + "    primary key (id))"));
        String message = e.getMessage();
        assertTrue(message != null && message.contains("path"), message == null ? "无异常消息" : message);
    }

    @Test
    void noPrimaryKeyXmlHasNoIdFragments() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(xmlConfig(support), "xml/no-primary-key",
                "src/main/resources/mapper/UserMapper.xml");
    }

    @Test
    void nonAutoIncrementPrimaryKeyKeptInInsert() throws Exception {
        GeneratorTestSupport support = support();
        support.generateAndAssertSubset(xmlConfig(support), "xml/non-auto-pk",
                "src/main/resources/mapper/UserMapper.xml");

        String xml = support.readGenerated("src/main/resources/mapper/UserMapper.xml");
        assertTrue(xml.contains("useGeneratedKeys=\"false\""), xml);
        assertFalse(xml.contains("keyProperty"), xml);
        assertTrue(xml.contains("#{id,jdbcType=BIGINT}"), xml);
    }

    private GeneratorTestSupport support() {
        // po(pojo) + mapper(mybatisMapper) + xml(mybatisXml)：XML 引用两者，CodeGenerator 要求被引用 kind 已注册
        return new GeneratorTestSupport(temp,
                new MapperXmlGenerator(), new MapperGenerator(), new PojoGenerator());
    }

    /** po + mapper(target=po) + xml(path=src/main/resources/mapper, target=po)。 */
    private DdlConfig xmlConfig(GeneratorTestSupport support) {
        DdlConfig config = support.tableConfig();
        support.addArtifact(config, "po", "pojo", "com.demo.pojo", "Po");
        support.addArtifact(config, "mapper", "mybatisMapper", "com.demo.mapper", "Mapper")
                .setTarget("po");
        ArtifactConfig xml = support.addArtifact(config, "xml", "mybatisXml", null, null);
        xml.setPath("src/main/resources/mapper");
        xml.setTarget("po");
        return config;
    }

}
