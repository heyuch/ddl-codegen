package hyc.codegen.tree.gen;

import java.util.StringJoiner;

/**
 * 语句块片段助手：纯字符串组合，便于生成器拼接方法体。
 * <p>
 * 生成的文本交给 {@code SourceBlock} 承载，缩进由打印器按当前层级自动对齐；
 * {@link #ifStmt} 内部语句按 4 空格一层缩进（与打印器默认缩进宽度一致）。
 */
public final class Block {

    private static final String SEP = System.lineSeparator();

    private static final String INDENT = "    ";

    private Block() {
        throw new AssertionError("no instances");
    }

    /** if (cond) { ... }：body 各语句自动缩进一层。 */
    public static String ifStmt(String cond, String... body) {
        StringJoiner joined = new StringJoiner(SEP);
        for (String stmt : body) {
            joined.add(INDENT + stmt);
        }
        return "if (" + cond + ") {" + SEP + joined + SEP + "}";
    }

    /** 多语句以行分隔符连接，构成方法体文本。 */
    public static String statements(String... stmts) {
        return String.join(SEP, stmts);
    }

}
