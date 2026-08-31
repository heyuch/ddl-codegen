package hyc.codegen.tree.utils;

import java.io.IOException;
import java.io.Writer;

/**
 * 面向代码生成器的缩进感知输出器。
 * <p>
 * 核心规则：行首缩进只在 {@link #write} 一处处理，其余方法（{@link #line}、
 * {@link #rawLine}、{@link #newline}）均基于行首状态派生；文本内嵌换行时逐行对齐。
 * 空行不写缩进，避免尾随空格。
 */
public final class CodePrinter {

    private static final String SEP = System.lineSeparator();

    private final Writer out;

    private final int indentWidth;

    private int level;

    private boolean atLineStart = true;

    public CodePrinter(Writer out) {
        this(out, 4);
    }

    public CodePrinter(Writer out, int indentWidth) {
        this.out = out;
        this.indentWidth = indentWidth;
    }

    /** 缩进层级 +1。 */
    public void indent() {
        level++;
    }

    /** 当前缩进空格数 = 层级 × 宽度。 */
    public int indentSpaces() {
        return indentWidth * level;
    }

    /** 写入一行内容并换行；内容为空时仅换行。 */
    public void line(Object... parts) {
        write(parts);
        newline();
    }

    /** 换行并标记行首。 */
    public void newline() {
        writeRaw(SEP);
        atLineStart = true;
    }

    /**
     * 原样写入内容（不做行首缩进、不处理内嵌换行），用于已手工对齐的文本。
     *
     * @param parts 内容片段，null 忽略
     */
    public void raw(Object... parts) {
        for (Object part : parts) {
            if (part == null) {
                continue;
            }
            writeRaw(part.toString());
        }
    }

    /** 原样写入一行并换行（不补缩进），用于已手工对齐的原始文本。 */
    public void rawLine(Object... parts) {
        raw(parts);
        newline();
    }

    /** 缩进层级 -1。 */
    public void undent() {
        level--;
    }

    /**
     * 写入内容：若处于行首先补缩进；文本内嵌换行时逐行对齐。
     *
     * @param parts 内容片段，null 忽略
     */
    public void write(Object... parts) {
        for (Object part : parts) {
            if (part == null) {
                continue;
            }
            writeText(part.toString());
        }
    }

    private void writeIndent() {
        writeRaw(" ".repeat(indentWidth * level));
    }

    private void writeRaw(String s) {
        try {
            out.write(s);
        } catch (IOException e) {
            throw new RuntimeException("codegen 输出失败", e);
        }
    }

    /** 写一段不含换行的文本：行首补缩进；空段不写缩进（避免尾随空格）。 */
    private void writeSegment(String segment) {
        if (segment.isEmpty()) {
            return;
        }
        if (atLineStart) {
            writeIndent();
            atLineStart = false;
        }
        writeRaw(segment);
    }

    /**
     * 写入可能含内嵌换行的文本：按行切分，每行行首自动补缩进。
     */
    private void writeText(String text) {
        int start = 0;
        while (true) {
            int nl = text.indexOf('\n', start);
            if (nl < 0) {
                writeSegment(text.substring(start));
                return;
            }
            writeSegment(text.substring(start, nl));
            newline();
            start = nl + 1;
        }
    }

}
