package hyc.codegen.core.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties 配置加载：全键解析、缺省默认值、启停顺序、校验错误。
 */
class ConfigTest {

    private final PropertiesConfigLoader loader = new PropertiesConfigLoader();

    // JUnit @TempDir 注入，语法层不保证非 null：标 @Nullable，使用点显式判空。
    @TempDir
    @Nullable
    Path tmpDir;

    private DdlConfig load(String content) throws Exception {
        Path dir = tmpDir;
        assertNotNull(dir);
        Path file = dir.resolve("ddlgen.properties");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return loader.load(file);
    }

    @Test
    void fullKeysParsed() throws Exception {
        DdlConfig config = load(String.join("\n",
                "entity.generator=pojo",
                "entity.module=core",
                "entity.package=com.myapp.core.entity",
                "entity.suffix=",
                "entity.lombok=true",
                "entity.jsr303=true",
                "entity.enums=true",
                "xml.generator=mybatisXml",
                "xml.module=service",
                "xml.path=src/main/resources/mapper",
                "xml.target=po",
                "repositoryImpl.generator=mybatisRepositoryImpl",
                "repositoryImpl.module=service",
                "repositoryImpl.package=com.myapp.service.repository.impl",
                "repositoryImpl.suffix=RepositoryImpl",
                "repositoryImpl.target=entity",
                "repositoryImpl.di=field",
                "naming.table.stripPrefixes=t_,tmp_",
                "naming.table.stripShardSuffix=true",
                "naming.column.keywordSuffix=_",
                "naming.method.prefix=query",
                "naming.enum.style=tableColumn",
                "annotations.custom=com.myapp.MyHandler"));

        Path rootDir = tmpDir;
        assertNotNull(rootDir);
        assertEquals(rootDir.toAbsolutePath(), config.getRoot());

        ArtifactConfig entity = config.artifact("entity").orElseThrow();
        assertEquals("pojo", entity.getGenerator());
        assertEquals("core", entity.getModule());
        assertEquals("com.myapp.core.entity", entity.getPkg());
        assertEquals("", entity.getSuffix());
        assertEquals("true", entity.getOption("lombok"));
        assertEquals("true", entity.getOption("enums"));

        ArtifactConfig xml = config.artifact("xml").orElseThrow();
        assertEquals("mybatisXml", xml.getGenerator());
        assertEquals("src/main/resources/mapper", xml.getPath());
        assertNull(xml.getPkg());
        assertFalse(xml.isJavaArtifact());
        assertEquals("po", xml.getTarget());

        ArtifactConfig impl = config.artifact("repositoryImpl").orElseThrow();
        assertEquals("mybatisRepositoryImpl", impl.getGenerator());
        assertEquals("entity", impl.getTarget());
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
        DdlConfig config = load("entity.generator=pojo\nentity.package=com.x.entity");

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
        assertTrue(entity.getOptions().isEmpty());
    }

    @Test
    void artifactNamesKeepConfigOrder() throws Exception {
        DdlConfig config = load(String.join("\n",
                "po.generator=pojo",
                "po.package=com.x.pojo",
                "entity.generator=pojo",
                "entity.package=com.x.entity",
                "mapper.generator=mybatisMapper",
                "mapper.package=com.x.mapper"));

        assertEquals(Arrays.asList("po", "entity", "mapper"), config.artifactNames());
    }

    @Test
    void missingPackageThrows() throws Exception {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> load("entity.generator=pojo\nentity.module=core"));
        String message = e.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("entity"));
    }

    @Test
    void reservedNamespaceRejectedAsArtifact() throws Exception {
        // naming.* 是保留命名空间，不会被当作产物
        DdlConfig config = load("naming.table.stripPrefixes=t_\nentity.package=com.x.entity");
        assertFalse(config.artifact("naming").isPresent());
        assertTrue(config.artifact("entity").isPresent());
    }

    @Test
    void unknownKeysStoredAsOptions() throws Exception {
        DdlConfig config = load("entity.package=com.x.entity\nentity.customKey=customValue");
        ArtifactConfig entity = config.artifact("entity").orElseThrow();
        assertEquals("customValue", entity.getOption("customKey"));
    }

}
