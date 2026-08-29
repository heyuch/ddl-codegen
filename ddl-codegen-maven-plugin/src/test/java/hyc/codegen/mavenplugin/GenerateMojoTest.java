package hyc.codegen.mavenplugin;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GenerateMojo 参数与执行逻辑（直接实例化 Mojo + 反射注入参数，不跑真实 mvn）。
 */
class GenerateMojoTest {

    @TempDir
    Path temp;

    private GenerateMojo mojo() {
        return new GenerateMojo();
    }

    /** 反射设置私有 @Parameter 字段。 */
    private void set(GenerateMojo mojo, String field, Object value) throws Exception {
        Field f = GenerateMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(mojo, value);
    }

    private Path writeConfig() throws Exception {
        Path config = temp.resolve("ddl-codegen.properties");
        Files.write(config, ("artifacts.entity.module=\n"
                + "artifacts.entity.package=com.demo.entity\n"
                + "artifacts.entity.suffix=\n").getBytes(StandardCharsets.UTF_8));
        return config;
    }

    private Path writeDdl() throws Exception {
        Path ddl = temp.resolve("schema.sql");
        Files.write(ddl, "create table user (id bigint primary key, name varchar(50) comment '用户名')\n"
                .getBytes(StandardCharsets.UTF_8));
        return ddl;
    }

    @Test
    void defaultConfigResolvesToProjectRoot() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", temp.toFile());
        set(mojo, "ddl", "create table user (id bigint primary key)");
        // configFile 为空 → 默认 projectRoot/ddl-codegen.properties，不存在 → 报错
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("配置文件不存在"), e.getMessage());
        assertTrue(e.getMessage().contains("ddl-codegen.properties"), e.getMessage());
    }

    @Test
    void missingConfigFailsClearly() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", temp.toFile());
        set(mojo, "ddl", "create table user (id bigint primary key)");
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("配置文件不存在"), e.getMessage());
    }

    @Test
    void ddlAndDdlFileAreMutuallyExclusive() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", temp.toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddl", "create table user (id bigint primary key)");
        set(mojo, "ddlFile", "schema.sql");
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("互斥"), e.getMessage());
    }

    @Test
    void skipShortCircuits() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "skip", true);
        // 什么都不配，skip 应直接返回
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void executeGeneratesFromInlineDdl() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", temp.toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddl", "create table user (id bigint primary key, name varchar(50) comment '用户名')");
        assertDoesNotThrow(mojo::execute);
        assertTrue(Files.isRegularFile(temp.resolve("com/demo/entity/User.java")));
    }

    @Test
    void ddlFileWithRangeSlicesLines() throws Exception {
        Path ddl = writeDdl();
        // 第一行是 create，第二行是空——范围 1-1 只取 create 语句
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", temp.toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddlFile", ddl.getFileName() + ":1-1");
        assertDoesNotThrow(mojo::execute);
        assertTrue(Files.isRegularFile(temp.resolve("com/demo/entity/User.java")));
    }

    @Test
    void missingDdlFileFails() throws Exception {
        GenerateMojo mojo = mojo();
        set(mojo, "projectRoot", temp.toFile());
        set(mojo, "configFile", writeConfig().toFile());
        set(mojo, "ddlFile", "no-such-file.sql");
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("DDL 文件不存在"), e.getMessage());
    }

}
