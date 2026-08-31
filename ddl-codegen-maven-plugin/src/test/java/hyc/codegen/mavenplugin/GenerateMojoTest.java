package hyc.codegen.mavenplugin;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GenerateMojo 参数与执行逻辑（直接实例化 Mojo + 反射注入参数，不跑真实 mvn）。
 */
class GenerateMojoTest {

    // JUnit @TempDir 注入，语法层不保证非 null：标 @Nullable，使用点经 tempDir() 显式校验。
    @TempDir
    @Nullable
    Path temp;

    @Test
    void ddlAndDdlFileAreMutuallyExclusive() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", tempDir().toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddl", "create table user (id bigint primary key)");
        set(mojo, "ddlFile", "schema.sql");
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        String message = e.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("互斥"), message);
    }

    @Test
    void ddlFileWithRangeSlicesLines() throws Exception {
        Path ddl = writeDdl();
        // 第一行是 create，第二行是空——范围 1-1 只取 create 语句
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", tempDir().toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddlFile", ddl.getFileName() + ":1-1");
        assertDoesNotThrow(mojo::execute);
        assertTrue(Files.isRegularFile(tempDir().resolve("com/demo/entity/User.java")));
    }

    @Test
    void defaultConfigResolvesToProjectRoot() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", tempDir().toFile());
        set(mojo, "ddl", "create table user (id bigint primary key)");
        // configFile 为空 → 默认 projectRoot/ddl-codegen.properties，不存在 → 报错
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        String message = e.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("配置文件不存在"), message);
        assertTrue(message.contains("ddl-codegen.properties"), message);
    }

    @Test
    void executeGeneratesFromInlineDdl() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", tempDir().toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddl", "create table user (id bigint primary key, name varchar(50) comment '用户名')");
        assertDoesNotThrow(mojo::execute);
        assertTrue(Files.isRegularFile(tempDir().resolve("com/demo/entity/User.java")));
    }

    @Test
    void missingConfigFailsClearly() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", tempDir().toFile());
        set(mojo, "ddl", "create table user (id bigint primary key)");
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        String message = e.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("配置文件不存在"), message);
    }

    @Test
    void missingDdlFileFails() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", tempDir().toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddlFile", "no-such-file.sql");
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        String message = e.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("DDL 文件不存在"), message);
    }

    private GenerateMojo mojo() {
        return new GenerateMojo();
    }

    /** 反射设置私有 @Parameter 字段。 */
    private void set(GenerateMojo mojo, String field, Object value) throws Exception {
        Field f = GenerateMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(mojo, value);
    }

    @Test
    void skipShortCircuits() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "skip", true);
        // 什么都不配，skip 应直接返回
        assertDoesNotThrow(mojo::execute);
    }

    /** @TempDir 注入目录：JUnit 保证注入，但语法层不保证非 null（标注 @Nullable），使用点经此显式校验。 */
    private Path tempDir() {
        Path dir = temp;
        if (dir == null) {
            throw new AssertionError("JUnit 未注入 @TempDir");
        }
        return dir;
    }

    private Path writeConfig() throws Exception {
        Path config = tempDir().resolve("ddl-codegen.properties");
        Files.write(config, ("entity.generator=pojo\n"
                + "entity.package=com.demo.entity\n"
                + "entity.suffix=\n").getBytes(StandardCharsets.UTF_8));
        return config;
    }

    private Path writeDdl() throws Exception {
        Path ddl = tempDir().resolve("schema.sql");
        Files.write(ddl, "create table user (id bigint primary key, name varchar(50) comment '用户名')\n"
                .getBytes(StandardCharsets.UTF_8));
        return ddl;
    }

}
