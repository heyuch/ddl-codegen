package hyc.codegen.mavenplugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * DDL 文件行范围：{@code create-user.sql:66-120} → 文件 + 起始/结束行（1 起）。
 * <p>
 * 无范围后缀（或后缀不是 {@code :start-end} 数字格式）= 整个文件；
 * 路径含 {@code :} 但不匹配范围格式时按整文件处理（找不到文件时错误信息会指出完整路径）。
 */
public final class DdlFileRange {

    private static final Pattern RANGE = Pattern.compile("^(.*):(\\d+)-(\\d+)$");

    private final String path;

    private final int startLine;

    private final int endLine;

    private DdlFileRange(String path, int startLine, int endLine) {
        this.path = path;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    /** 解析范围；无范围返回 null（整文件）。格式错误（start≥1 且 start≤end）抛异常。 */
    public static @Nullable DdlFileRange parse(String value) {
        Matcher matcher = RANGE.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        String path = matcher.group(1);
        String startStr = matcher.group(2);
        String endStr = matcher.group(3);
        if (path == null || startStr == null || endStr == null) {
            throw new IllegalArgumentException("行范围格式非法: " + value);
        }
        int start = Integer.parseInt(startStr);
        int end = Integer.parseInt(endStr);
        if (start < 1) {
            throw new IllegalArgumentException("行范围起始必须 ≥1: " + value);
        }
        if (start > end) {
            throw new IllegalArgumentException("行范围起始大于结束: " + value);
        }
        return new DdlFileRange(path, start, end);
    }

    public int getEndLine() {
        return endLine;
    }

    public String getPath() {
        return path;
    }

    public int getStartLine() {
        return startLine;
    }

}
