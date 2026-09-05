package hyc.codegen.core.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.Tree.Kind;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.EnumCommentParser;
import hyc.codegen.core.model.EnumItem;
import hyc.codegen.core.types.TypeMapper;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.DocComment;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.SourceExpr;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Variable;
import hyc.codegen.tree.VariableKind;

/**
 * 枚举类生成器（注册名 {@code enum}）：表内每个枚举列生成一个枚举类（一表多文件）。
 * <p>
 * 枚举列 = SQL {@code enum(...)} 类型列或 {@code @enum} 标注列（{@link Column#isEnumColumn()}）。
 * 模板（code/desc 双字段 + 反查方法对，breaking）：常量 = 枚举项；字段 {@code code}（数据库存储值，
 * 类型 = 列映射）与 {@code desc}（描述）；产物选项 {@code lombok=true} → 类级
 * {@code @Getter + @RequiredArgsConstructor}，缺省 → 手写私有构造器与 {@code getCode()/getDesc()}。
 * 反查：{@code fromCodeNullable}（宽松，{@code @Nullable} 入参/返回，null/未命中均返 null）
 * 与 {@code fromCode}（严格，null 入参或未命中抛异常）。
 * <p>
 * 枚举项来源：SQL-enum 列 = {@code enum(...)} 字面量为主，comment 列表按 code 匹配补 desc/name
 * （不匹配项 warning 跳过）；{@code @enum} 列 = comment 列表（格式 {@code {code}={desc}({name})}，
 * name 可选），无列表 → 空枚举类骨架（留用户自补）。类名：列 {@code @as} &gt; {@code @enum} 值 &gt;
 * 命名策略（{@link TableContext#enumClassName}）。枚举项数据（常量 init）参与 reconcile 签名，
 * ALTER 列表后增/删/同名 code/desc 值改均按 {@code @Generated} 成员增量同步。
 */
public final class EnumGenerator extends AbstractJavaGenerator {

    /**
     * 生成器注册名。
     */
    public static final String NAME = "enum";

    /** Java 关键字（{@code @enum} 类名不得为关键字）。 */
    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while"));

    private static final String JAVA_LANG_STRING = "java.lang.String";

    private static final System.Logger LOG = System.getLogger(EnumGenerator.class.getName());

    /**
     * 列 code 字段的 Java 类型：SQL-enum 列 = String（存储字面量）；{@code @enum} 列 = 自然映射
     * Integer/Long/String，其余类型（decimal/BigDecimal、tinyint(1)/Boolean、date 等）不支持 → 报错。
     */
    private static String codeTypeOf(TableContext ctx, Column column) {
        if (column.isEnum()) {
            return JAVA_LANG_STRING;
        }
        String type = TypeMapper.sqlToJava(column);
        if ("java.lang.Integer".equals(type) || "java.lang.Long".equals(type) || JAVA_LANG_STRING.equals(type)) {
            return type;
        }
        throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 列 " + column.getName()
                + " 的 @enum 不受支持：类型映射为 " + type
                + "（仅支持 Integer/Long/String；tinyint(1) 等 Boolean 映射列请改用 tinyint）");
    }

    /**
     * 枚举常量名：非标识符字符转下划线、大写；空串 → EMPTY；数字开头加前缀。
     */
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

    /** {@code @enum} 显式类名合法性：简单 Java 标识符且非关键字。 */
    private static boolean isValidClassName(String name) {
        if (name == null || name.isEmpty() || JAVA_KEYWORDS.contains(name)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String lookupBody(String enumName) {
        return "for (" + enumName + " e : values()) {\n"
                + "    if (java.util.Objects.equals(e.code, code)) {\n"
                + "        return e;\n"
                + "    }\n"
                + "}";
    }

    private static boolean option(TableContext ctx, String key) {
        return Boolean.parseBoolean(ctx.getArtifactConfig().getOption(key));
    }

    private static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    /** lombok=false：手写私有构造器 + getCode()/getDesc()。 */
    private void addHandwrittenAccessors(Class.Builder builder, String enumName, String codeType) {
        builder.method(Method.builder()
                .modifiers(Modifier.PRIVATE)
                .name(enumName)
                .parameter(Variable.builder().type(new TypeReference(codeType)).name("code").build())
                .parameter(Variable.builder()
                        .type(new TypeReference(JAVA_LANG_STRING))
                        .name("desc")
                        .build())
                .body("this.code = code;\nthis.desc = desc;")
                .javadoc(DocComment.builder().summary("私有构造：code + desc").build())
                .build());
        builder.method(Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new TypeReference(codeType))
                .name("getCode")
                .body("return code;")
                .javadoc(DocComment.builder().summary("获取数据库存储值 code").build())
                .build());
        builder.method(Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new TypeReference(JAVA_LANG_STRING))
                .name("getDesc")
                .body("return desc;")
                .javadoc(DocComment.builder().summary("获取枚举值描述 desc").build())
                .build());
    }

    /** 反查方法对（恒生成，与 lombok 形态无关）。 */
    private void addLookupMethods(Class.Builder builder, TableContext ctx, String enumName, String codeType) {
        String nullable = ctx.getNullableAnnotation();
        builder.method(Method.builder()
                .modifiers(Modifier.PUBLIC, Modifier.STATIC)
                .annotation(Annotation.of(nullable))
                .returnType(new TypeReference(enumName))
                .name("fromCodeNullable")
                .javadoc(DocComment.builder().summary("按 code 宽松反查：无匹配或入参为 null 时返回 null").build())
                .parameter(Variable.builder()
                        .kind(VariableKind.PARAMETER)
                        .annotation(Annotation.of(nullable))
                        .type(new TypeReference(codeType))
                        .name("code")
                        .build())
                .body("if (code == null) {\n"
                        + "    return null;\n"
                        + "}\n"
                        + lookupBody(enumName)
                        + "\nreturn null;")
                .build());
        builder.method(Method.builder()
                .modifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returnType(new TypeReference(enumName))
                .name("fromCode")
                .javadoc(DocComment.builder().summary("按 code 严格反查：入参为 null 或无匹配时抛异常").build())
                .parameter(Variable.builder()
                        .kind(VariableKind.PARAMETER)
                        .type(new TypeReference(codeType))
                        .name("code")
                        .build())
                .body("if (code == null) {\n"
                        + "    throw new IllegalArgumentException(\"code 不能为 null\");\n"
                        + "}\n"
                        + lookupBody(enumName)
                        + "\nthrow new IllegalArgumentException(\"未知枚举值: \" + code);")
                .build());
    }

    private void addResolved(TableContext ctx, Column column, List<ResolvedItem> resolved, Set<Object> codes,
            Set<String> names, EnumItem item, String codeType) {
        Object code;
        String codeLiteral;
        String rawCode = item.getRawCode();
        if ("java.lang.Integer".equals(codeType)) {
            try {
                code = Integer.valueOf(rawCode);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 列 " + column.getName()
                        + " 的枚举项 code '" + rawCode + "' 不是整数（列类型 Integer）", e);
            }
            codeLiteral = rawCode;
        } else if ("java.lang.Long".equals(codeType)) {
            try {
                code = Long.valueOf(rawCode);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 列 " + column.getName()
                        + " 的枚举项 code '" + rawCode + "' 不是整数（列类型 Long）", e);
            }
            codeLiteral = rawCode + "L";
        } else {
            code = rawCode;
            codeLiteral = quote(rawCode);
        }
        if (!codes.add(code)) {
            throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 列 " + column.getName()
                    + " 的枚举项 code 重复: " + rawCode + "（数值按解析后值判重，前导零视作同值）");
        }

        // 常量名候选：显式 (name) > 字符列/SQL-enum 用 code 原文 > 数字列用 {列名}_{rawCode}；统一大写清洗
        String name = item.getName();
        String candidate = name != null
                ? name
                : JAVA_LANG_STRING.equals(codeType)
                        ? rawCode
                        : column.getName() + "_" + rawCode;
        String constantName = constantName(candidate);
        if (!names.add(constantName)) {
            throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 列 " + column.getName()
                    + " 的枚举项常量名重复（清洗后）: " + constantName + "（可用 (name) 显式区分）");
        }

        resolved.add(new ResolvedItem(constantName, "(" + codeLiteral + ", " + quote(item.getDesc()) + ")",
                item.getDesc()));
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        // 单类路径不使用（generate() 已按列拆分）；保留空实现以满足抽象方法
    }

    private void buildEnum(Class.Builder builder, TableContext ctx, Column column, String enumName,
            List<ResolvedItem> items, boolean lombok) {
        builder.kind(Kind.ENUM);
        if (lombok) {
            builder.annotation(Annotation.of("lombok.Getter"));
            builder.annotation(Annotation.of("lombok.RequiredArgsConstructor"));
        }
        // 类 javadoc：表注释优先，空则退列注释（CommentDocs 内部清洗）
        CommentDocs.classDoc(builder, CommentDocs.summary(ctx.tableComment()) != null
                ? ctx.tableComment()
                : column.getComment());

        for (ResolvedItem item : items) {
            Variable.Builder constant = Variable.builder()
                    .name(item.constantName)
                    .type(new TypeReference(enumName))
                    .init(new SourceExpr(item.initText));
            if (!item.desc.isEmpty()) {
                constant.javadoc(DocComment.builder().summary(item.desc).build());
            }
            builder.enumConstant(constant.build());
        }

        String codeType = codeTypeOf(ctx, column);
        builder.field(Variable.builder()
                .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                .type(new TypeReference(codeType))
                .name("code")
                .javadoc(DocComment.builder().summary("code - 数据库存储值").build())
                .build());
        builder.field(Variable.builder()
                .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                .type(new TypeReference(JAVA_LANG_STRING))
                .name("desc")
                .javadoc(DocComment.builder().summary("desc - 枚举值描述").build())
                .build());

        if (!lombok) {
            addHandwrittenAccessors(builder, enumName, codeType);
        }
        addLookupMethods(builder, ctx, enumName, codeType);
    }

    /**
     * 枚举列类名（{@code @as} > {@code @enum} 值 > 命名策略），并校验 {@code @enum} 显式值合法性。
     */
    private String enumClassNameOf(Column column, TableContext ctx) {
        Object annotated = column.getMeta().get("enum");
        if (annotated instanceof String) {
            String name = annotated.toString();
            if (!isValidClassName(name)) {
                throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 列 " + column.getName()
                        + " 的 @enum 值 '" + name + "' 不是合法简单类名（Java 标识符且非关键字）");
            }
        }
        return ctx.enumClassName(column);
    }

    @Override
    public void generate(TableContext ctx, GenerationContext gctx) {
        if (!shouldGenerate(ctx)) {
            deleteIfExists(ctx, gctx);
            return;
        }

        boolean lombok = option(ctx, "lombok");
        // 同表类名撞名 fail-fast（含两列名与类名）
        Map<String, Column> classNameColumns = new HashMap<>();
        for (Column column : ctx.columns()) {
            if (!column.isEnumColumn()) {
                continue;
            }
            if (!column.isEnum() && column.getMeta().contains("type")) {
                throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 列 " + column.getName()
                        + " 同时带 @enum 与列级 @type（@type 优先于 enums 视图，二者叠加语义矛盾）");
            }
            String enumName = enumClassNameOf(column, ctx);
            Column previous = classNameColumns.putIfAbsent(enumName, column);
            if (previous != null) {
                throw new IllegalStateException("表 '" + ctx.getTable().getName() + "' 下列 " + previous.getName()
                        + " 与 " + column.getName() + " 解析出相同枚举类名 '" + enumName + "'（可用 @as/@enum 指定类名）");
            }
            List<ResolvedItem> items = resolveItems(ctx, column);
            generateClass(ctx, gctx, enumName,
                    builder -> buildEnum(builder, ctx, column, enumName, items, lombok));
        }
    }

    @Override
    public String kind() {
        return NAME;
    }

    /** 解析列枚举项并校验（code/常量名重复、数字解析失败等 → 报错含表.列）。 */
    private List<ResolvedItem> resolveItems(TableContext ctx, Column column) {
        List<EnumItem> parsed = EnumCommentParser.parse(column.getComment());
        String codeType = codeTypeOf(ctx, column);

        List<ResolvedItem> resolved = new ArrayList<>();
        Set<Object> codes = new HashSet<>();
        Set<String> names = new HashSet<>();

        if (column.isEnum()) {
            // SQL-enum 列：字面量为主；comment 列表按 code（字面量）匹配补 desc/name，不匹配项 warning 跳过
            for (String literal : column.getEnumValues()) {
                EnumItem matched = null;
                for (EnumItem item : parsed) {
                    if (literal.equals(item.getRawCode())) {
                        matched = item;
                        break;
                    }
                }
                addResolved(ctx, column, resolved, codes, names,
                        matched != null ? matched : new EnumItem(literal, "", null), codeType);
            }
            for (EnumItem item : parsed) {
                if (!column.getEnumValues().contains(item.getRawCode())) {
                    LOG.log(System.Logger.Level.WARNING,
                            "表 {0} 列 {1} 的 comment 枚举项 code 不在 enum(...) 字面量中，已忽略: {2}",
                            ctx.getTable().getName(), column.getName(), item.getRawCode());
                }
            }
            return resolved;
        }

        for (EnumItem item : parsed) {
            addResolved(ctx, column, resolved, codes, names, item, codeType);
        }
        return resolved;
    }

    @Override
    protected boolean shouldGenerate(TableContext ctx) {
        for (Column column : ctx.columns()) {
            if (column.isEnumColumn()) {
                return true;
            }
        }
        return false;
    }

    /** 解析后的枚举项：最终常量名 + 完整常量 init 文本 + desc（常量 javadoc 用）。 */
    private static final class ResolvedItem {

        final String constantName;

        final String initText;

        final String desc;

        ResolvedItem(String constantName, String initText, String desc) {
            this.constantName = constantName;
            this.initText = initText;
            this.desc = desc;
        }

    }

}
