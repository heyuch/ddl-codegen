package hyc.codegen.core.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileIoTest {

    // JUnit @TempDir 注入，语法层不保证非 null：标 @Nullable，使用点经 tempDir() 显式校验。
    @TempDir
    @Nullable
    Path temp;

    /**
     * 注入目录（@TempDir 注入，标注 @Nullable，使用点经此显式校验）。
     */
    private Path tempDir() {
        Path dir = temp;
        if (dir == null) {
            throw new AssertionError("JUnit 未注入 @TempDir");
        }
        return dir;
    }

    @Test
    void javaFileWithModule() {
        Path file = PathResolver.javaFile(tempDir(), "core", "com.myapp.core.entity", "User");
        assertEquals(tempDir().resolve("core/com/myapp/core/entity/User.java"), file);
    }

    @Test
    void javaFileWithoutModule() {
        Path file = PathResolver.javaFile(tempDir(), null, "com.myapp", "User");
        assertEquals(tempDir().resolve("com/myapp/User.java"), file);
    }

    @Test
    void xmlFileWithResourcePath() {
        Path file = PathResolver.xmlFile(tempDir(), "service", "src/main/resources/mapper", "UserMapper.xml");
        assertEquals(tempDir().resolve("service/src/main/resources/mapper/UserMapper.xml"), file);
    }

    @Test
    void writeCreatesNewFile() throws IOException {
        Path file = PathResolver.javaFile(tempDir(), "core", "com.a", "User");
        assertEquals(ChangeStatus.CREATED, FileWriter.writeIfChanged(file, "class User {}"));
        assertTrue(Files.isRegularFile(file));
        assertEquals("class User {}", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void writeSameContentDoesNotWrite() throws IOException {
        Path file = tempDir().resolve("a.txt");
        FileWriter.writeIfChanged(file, "hello");
        assertEquals(ChangeStatus.UNCHANGED, FileWriter.writeIfChanged(file, "hello"));
        // 第二次写不改变内容与时间无关，只需确认状态
        assertEquals("hello", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void writeChangedContentUpdates() throws IOException {
        Path file = tempDir().resolve("a.txt");
        FileWriter.writeIfChanged(file, "v1");
        assertEquals(ChangeStatus.UPDATED, FileWriter.writeIfChanged(file, "v2"));
        assertEquals("v2", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void reportTracksStatusAndSummary() throws IOException {
        ChangeReport report = new ChangeReport();
        report.add(tempDir().resolve("new.java"), ChangeStatus.CREATED);
        report.add(tempDir().resolve("upd.java"), ChangeStatus.UPDATED);
        report.add(tempDir().resolve("del.java"), ChangeStatus.DELETED);
        report.add(tempDir().resolve("same.java"), ChangeStatus.UNCHANGED);

        assertTrue(report.hasChanges());
        assertEquals("+1 ~1 -1 =1", report.summary());
        assertEquals(4, report.getEntries().size());
    }

}
