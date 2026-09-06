package io.github.heyuch.codegen.core.gen;

import java.util.StringJoiner;
import java.util.regex.Pattern;

import io.github.heyuch.codegen.core.model.Column;
import io.github.heyuch.codegen.tree.Class;
import io.github.heyuch.codegen.tree.DocComment;
import io.github.heyuch.codegen.tree.Method;
import io.github.heyuch.codegen.tree.Variable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * DDL 注释 → javadoc 文本清洗：剥除 {@code @name[:value]} 注解 token 与 {@code {code}={desc}({name})}
 * 枚举项 token，保留说明文本（如「状态 1=初始(INIT) @enum:Status」→「状态」）；清洗后为空返回 null。
 */
public final class CommentDocs {

    /** 枚举项 token（与 EnumCommentParser 语法一致）：{@code code=desc} 或 {@code code=desc(name)}。 */
    private static final Pattern ENUM_ITEM = Pattern.compile("^[^=@()\\s]+=[^()\\s]*(?:\\([^()\\s]+\\))?$");

    private CommentDocs() {
        throw new AssertionError("no instances");
    }

    /** 清洗后非空则为类加 javadoc 摘要。 */
    public static void classDoc(Class.Builder builder, @Nullable String comment) {
        String text = summary(comment);
        if (text != null) {
            builder.javadoc(DocComment.builder().summary(text).build());
        }
    }

    /** 清洗后非空则为字段加 javadoc 摘要。 */
    public static void fieldDoc(Variable.Builder builder, @Nullable String comment) {
        String text = summary(comment);
        if (text != null) {
            builder.javadoc(DocComment.builder().summary(text).build());
        }
    }

    /** 索引列查询方法摘要：按各列注释（空则字段名）「按 X 查询」。 */
    public static String findBySummary(TableContext ctx, java.util.List<String> columns) {
        StringJoiner names = new StringJoiner("、");
        for (String columnName : columns) {
            Column column = ctx.getTable().getColumn(columnName);
            if (column == null) {
                continue;
            }
            String comment = summary(column.getComment());
            names.add(comment != null ? comment : ctx.fieldName(column));
        }
        String text = names.toString();
        return text.isEmpty() ? "按条件查询" : "按 " + text + " 查询";
    }

    /** 清洗后非空则为方法加 javadoc 摘要。 */
    public static void methodDoc(Method.Builder builder, @Nullable String comment) {
        String text = summary(comment);
        if (text != null) {
            builder.javadoc(DocComment.builder().summary(text).build());
        }
    }

    /**
     * 清洗列/表注释为 javadoc 摘要。
     *
     * @param comment DDL 注释原文（可为 null/空）
     * 
     * @return 清洗后文本；空则 null（调用方不加 javadoc）
     */
    public static @Nullable String summary(@Nullable String comment) {
        if (comment == null || comment.isEmpty()) {
            return null;
        }
        StringJoiner kept = new StringJoiner(" ");
        for (String token : comment.split("\\s+", -1)) {
            if (token.isEmpty() || token.charAt(0) == '@' || ENUM_ITEM.matcher(token).matches()) {
                continue;
            }
            kept.add(token);
        }
        String text = kept.toString().trim();
        return text.isEmpty() ? null : text;
    }

}
