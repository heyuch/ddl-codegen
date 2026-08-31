package hyc.codegen.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLI 入口行为测试：参数解析、正常生成、dry-run、目录 DDL、错误路径。
 */
class MainTest {

    // JUnit @TempDir 注入，语法层不保证非 null：标 @Nullable，使用点经 tempDir() 显式校验。
    @TempDir
    @Nullable
    Path temp;

    @Test
    void directoryDdlConcatenatesSqlFiles() throws Exception {
        Path config = writeConfig();
        Path dir = tempDir().resolve("ddl-dir");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("a.sql"), "create table user_a (id bigint primary key)\n");
        Files.writeString(dir.resolve("b.sql"), "create table user_b (id bigint primary key)\n");

        int code = Main.run(new String[] {"--config", config.toString(), "--ddl", dir.toString()});

        assertEquals(0, code);
        assertTrue(Files.isRegularFile(tempDir().resolve("com/demo/entity/UserA.java")), "应生成 UserA.java");
        assertTrue(Files.isRegularFile(tempDir().resolve("com/demo/entity/UserB.java")), "应生成 UserB.java");
    }

    @Test
    void dryRunDoesNotWrite() throws Exception {
        Path config = writeConfig();
        Path ddl = writeDdl();

        int code = Main.run(new String[] {"--config", config.toString(), "--ddl", ddl.toString(), "--dry-run"});

        assertEquals(0, code);
        assertFalse(Files.exists(tempDir().resolve("com/demo/entity/User.java")), "dry-run 不应写盘");
    }

    @Test
    void generateWritesFiles() throws Exception {
        Path config = writeConfig();
        Path ddl = writeDdl();

        int code = Main.run(new String[] {"--config", config.toString(), "--ddl", ddl.toString()});

        assertEquals(0, code);
        Path generated = tempDir().resolve("com/demo/entity/User.java");
        assertTrue(Files.isRegularFile(generated), "应生成实体类 User.java");
    }

    @Test
    void helpReturns0() {
        assertEquals(0, Main.run(new String[] {"--help"}));
    }

    @Test
    void missingDdlArgReturns2() {
        assertEquals(2, Main.run(new String[] {"--config", "ddl-codegen.properties"}));
    }

    @Test
    void missingDdlPathReturns1() throws Exception {
        Path config = writeConfig();

        int code = Main.run(new String[] {"--config", config.toString(), "--ddl", "不存在.sql"});

        assertEquals(1, code);
    }

    private Path tempDir() {
        Path dir = temp;
        if (dir == null) {
            throw new AssertionError("JUnit 未注入 @TempDir");
        }
        return dir;
    }

    @Test
    void unknownArgReturns2() {
        assertEquals(2, Main.run(new String[] {"--foo"}));
    }

    private Path writeConfig() throws Exception {
        Path config = tempDir().resolve("ddl-codegen.properties");
        Files.writeString(config, "entity.generator=pojo\nentity.package=com.demo.entity\n", StandardCharsets.UTF_8);
        return config;
    }

    private Path writeDdl() throws Exception {
        Path ddl = tempDir().resolve("schema.sql");
        Files.writeString(ddl, "create table user (\n    id bigint primary key,\n    name varchar(50)\n)\n",
                StandardCharsets.UTF_8);
        return ddl;
    }

}
