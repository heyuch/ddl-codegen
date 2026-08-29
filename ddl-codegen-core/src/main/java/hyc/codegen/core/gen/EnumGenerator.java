package hyc.codegen.core.gen;

import java.util.List;
import java.util.Locale;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.Tree.Kind;
import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.SourceExpr;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Variable;

/**
 * 枚举类生成器：表内每个 enum 列生成一个枚举类（一表多文件）。
 * <p>
 * 结构：常量（携带 DDL 值）+ {@code value()}（常量 → DDL 值）+ {@code fromValue(String)}（DDL 值 → 常量，
 * 支持非标识符值如 {@code in-progress}）。类名按命名策略（{@code Gender} / {@code UserGender}），
 * 可被 {@code @as} 覆盖。列失去 enum 类型后文件不自动清理（见 docs/progress.md 已知限制）。
 */
public final class EnumGenerator extends AbstractJavaArtifactGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "enum";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    protected boolean shouldGenerate(TableContext ctx) {
        for (Column column : ctx.columns()) {
            if (!column.getEnumValues().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void generate(TableContext ctx, GenerationContext gctx) {
        if (!shouldGenerate(ctx)) {
            deleteIfExists(ctx, gctx);
            return;
        }
        for (Column column : ctx.columns()) {
            if (column.getEnumValues().isEmpty()) {
                continue;
            }
            String enumName = ctx.enumClassName(column);
            generateClass(ctx, gctx, enumName, builder -> buildEnum(builder, ctx, column, enumName));
        }
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        // 单类路径不使用（generate() 已按列拆分）；保留空实现以满足抽象方法
    }

    private void buildEnum(Class.Builder builder, TableContext ctx, Column column, String enumName) {
        builder.kind(Kind.ENUM);

        for (String value : column.getEnumValues()) {
            builder.enumConstant(Variable.builder()
                    .name(constantName(value))
                    .init(new SourceExpr("\"" + escape(value) + "\""))
                    .build());
        }

        builder.field(Variable.builder()
                .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                .type(new TypeReference("java.lang.String"))
                .name("value")
                .build());

        builder.method(Method.builder()
                .modifiers(Modifier.PRIVATE)
                .name(enumName)
                .parameter(Variable.builder()
                        .type(new TypeReference("java.lang.String"))
                        .name("value")
                        .build())
                .body("this.value = value;")
                .build());

        builder.method(Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new TypeReference("java.lang.String"))
                .name("value")
                .body("return value;")
                .build());

        builder.method(Method.builder()
                .modifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returnType(new TypeReference(enumName))
                .name("fromValue")
                .parameter(Variable.builder()
                        .type(new TypeReference("java.lang.String"))
                        .name("value")
                        .build())
                .body(fromValueBody(column.getEnumValues(), enumName))
                .build());
    }

    /** 枚举常量名：非标识符字符转下划线、大写；空串 → EMPTY；数字开头加前缀。 */
    static String constantName(String value) {
        if (value == null || value.isEmpty()) {
            return "EMPTY";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String name = sb.toString().toUpperCase(Locale.ROOT);
        if (Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        return name;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String fromValueBody(List<String> values, String enumName) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (value == null) {\n");
        sb.append("    return null;\n");
        sb.append("}\n");
        sb.append("switch (value) {\n");
        for (String value : values) {
            sb.append("    case \"").append(escape(value)).append("\":\n");
            sb.append("        return ").append(constantName(value)).append(";\n");
        }
        sb.append("    default:\n");
        sb.append("        throw new IllegalArgumentException(\"未知枚举值: \" + value);\n");
        sb.append("}");
        return sb.toString();
    }

}
