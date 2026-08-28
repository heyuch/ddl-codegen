package hyc.codegen.core.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 生成文件写入器：写前字节比对，内容不变不写盘（幂等保证）。
 */
public final class FileWriter {

    private FileWriter() {
        throw new AssertionError("no instances");
    }

    /**
     * 写文件（UTF-8）：内容与现有文件字节一致则跳过写盘，否则创建/覆盖。
     *
     * @param file    目标文件
     * @param content 新内容
     * 
     * @return 写盘状态
     */
    public static ChangeStatus writeIfChanged(Path file, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        boolean existed = Files.isRegularFile(file);
        if (existed) {
            byte[] existing = Files.readAllBytes(file);
            if (Arrays.equals(existing, bytes)) {
                return ChangeStatus.UNCHANGED;
            }
        }

        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, bytes);
        return existed ? ChangeStatus.UPDATED : ChangeStatus.CREATED;
    }

    /** 无条件删除文件；不存在时静默返回。 */
    public static boolean deleteIfExists(Path file) throws IOException {
        return Files.deleteIfExists(file);
    }

}
