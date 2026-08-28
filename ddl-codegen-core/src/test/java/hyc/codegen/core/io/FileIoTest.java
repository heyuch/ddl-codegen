package hyc.codegen.core.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileIoTest {

    @TempDir
    Path temp;

    @Test
    void javaFileWithModule() {
        Path file = PathResolver.javaFile(temp, "core", "com.myapp.core.entity", "User");
        assertEquals(temp.resolve("core/com/myapp/core/entity/User.java"), file);
    }

    @Test
    void javaFileWithoutModule() {
        Path file = PathResolver.javaFile(temp, null, "com.myapp", "User");
        assertEquals(temp.resolve("com/myapp/User.java"), file);
    }

    @Test
    void xmlFileWithResourcePath() {
        Path file = PathResolver.xmlFile(temp, "service", "src/main/resources/mapper", "UserMapper.xml");
        assertEquals(temp.resolve("service/src/main/resources/mapper/UserMapper.xml"), file);
    }

    @Test
    void writeCreatesNewFile() throws IOException {
        Path file = PathResolver.javaFile(temp, "core", "com.a", "User");
        assertEquals(ChangeStatus.CREATED, FileWriter.writeIfChanged(file, "class User {}"));
        assertTrue(Files.isRegularFile(file));
        assertEquals("class User {}", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void writeSameContentDoesNotWrite() throws IOException {
        Path file = temp.resolve("a.txt");
        FileWriter.writeIfChanged(file, "hello");
        assertEquals(ChangeStatus.UNCHANGED, FileWriter.writeIfChanged(file, "hello"));
        // 第二次写不改变内容与时间无关，只需确认状态
        assertEquals("hello", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void writeChangedContentUpdates() throws IOException {
        Path file = temp.resolve("a.txt");
        FileWriter.writeIfChanged(file, "v1");
        assertEquals(ChangeStatus.UPDATED, FileWriter.writeIfChanged(file, "v2"));
        assertEquals("v2", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void reportTracksStatusAndSummary() throws IOException {
        ChangeReport report = new ChangeReport();
        report.add(temp.resolve("new.java"), ChangeStatus.CREATED);
        report.add(temp.resolve("upd.java"), ChangeStatus.UPDATED);
        report.add(temp.resolve("del.java"), ChangeStatus.DELETED);
        report.add(temp.resolve("same.java"), ChangeStatus.UNCHANGED);

        assertTrue(report.hasChanges());
        assertEquals("+1 ~1 -1 =1", report.summary());
        assertEquals(4, report.getEntries().size());
    }

}
