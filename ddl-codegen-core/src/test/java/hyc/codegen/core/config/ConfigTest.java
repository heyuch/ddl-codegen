package hyc.codegen.core.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties 配置加载：全键解析、缺省默认值、启停顺序、校验错误。
 */
class ConfigTest {

    private final PropertiesConfigLoader loader = new PropertiesConfigLoader();

    @TempDir
    Path tmpDir;

    private DdlConfig load(String content) throws Exception {
        Path file = tmpDir.resolve("ddlgen.properties");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return loader.load(file);
    }

    @Test
    void fullKeysParsed() throws Exception {
        DdlConfig config = load(String.join("\n",
                "artifacts.entity.module=core",
                "artifacts.entity.package=com.myapp.core.entity",
                "artifacts.entity.suffix=",
                "artifacts.entity.use=lombok,jsr303",
                "artifacts.mybatisXml.module=service",
                "artifacts.mybatisXml.path=src/main/resources/mapper",
                "artifacts.repositoryImpl.module=service",
                "artifacts.repositoryImpl.package=com.myapp.service.repository.impl",
                "artifacts.repositoryImpl.suffix=RepositoryImpl",
                "artifacts.repositoryImpl.di=field",
                "naming.table.stripPrefixes=t_,tmp_",
                "naming.table.stripShardSuffix=true",
                "naming.column.keywordSuffix=_",
                "naming.method.prefix=query",
                "naming.enum.style=tableColumn",
                "annotations.custom=com.myapp.MyHandler"));

        assertEquals(tmpDir.toAbsolutePath(), config.getRoot());

        ArtifactConfig entity = config.artifact("entity").orElseThrow();
        assertEquals("core", entity.getModule());
        assertEquals("com.myapp.core.entity", entity.getPkg());
        assertEquals("", entity.getSuffix());
        assertEquals(Arrays.asList("lombok", "jsr303"), entity.getUse());

        ArtifactConfig xml = config.artifact("mybatisXml").orElseThrow();
        assertEquals("src/main/resources/mapper", xml.getPath());
        assertNull(xml.getPkg());
        assertFalse(xml.isJavaArtifact());

        ArtifactConfig impl = config.artifact("repositoryImpl").orElseThrow();
        assertEquals("field", impl.getOptions().get("di"));

        assertEquals(Arrays.asList("t_", "tmp_"), config.getTableStripPrefixes());
        assertTrue(config.isTableStripShardSuffix());
        assertEquals("_", config.getColumnKeywordSuffix());
        assertEquals("query", config.getMethodPrefix());
        assertEquals("tableColumn", config.getEnumStyle());
        assertEquals(Arrays.asList("com.myapp.MyHandler"), config.getCustomAnnotationHandlers());
    }

    @Test
    void defaultsApplied() throws Exception {
        DdlConfig config = load("artifacts.entity.package=com.x.entity");

        assertTrue(config.getTableStripPrefixes().isEmpty());
        assertFalse(config.isTableStripShardSuffix());
        assertEquals("_\\d+$", config.getTableShardPattern());
        assertTrue(config.isColumnCamelCase());
        assertEquals("_", config.getColumnKeywordSuffix());
        assertEquals("find", config.getMethodPrefix());
        assertEquals("column", config.getEnumStyle());
        assertTrue(config.getCustomAnnotationHandlers().isEmpty());

        ArtifactConfig entity = config.artifact("entity").orElseThrow();
        assertNull(entity.getModule());
        assertEquals("", entity.getSuffix());
        assertTrue(entity.getUse().isEmpty());
    }

    @Test
    void artifactKindsKeepConfigOrder() throws Exception {
        DdlConfig config = load(String.join("\n",
                "artifacts.pojo.package=com.x.pojo",
                "artifacts.entity.package=com.x.entity",
                "artifacts.mybatisMapper.package=com.x.mapper"));

        assertEquals(Arrays.asList("pojo", "entity", "mybatisMapper"), config.artifactKinds());
    }

    @Test
    void missingPackageThrows() throws Exception {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> load("artifacts.entity.module=core"));
        assertTrue(e.getMessage().contains("entity"));
    }

    @Test
    void emptyUseIgnored() throws Exception {
        DdlConfig config = load("artifacts.entity.package=com.x.entity\nartifacts.entity.use=lombok,,jsr303 ,");
        List<String> use = config.artifact("entity").orElseThrow().getUse();
        assertEquals(Arrays.asList("lombok", "jsr303"), use);
    }

}
