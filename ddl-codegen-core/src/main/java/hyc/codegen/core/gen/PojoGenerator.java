package hyc.codegen.core.gen;

import javax.lang.model.element.Modifier;

import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Literal;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Variable;

/**
 * 纯字段类生成器（注册名 {@code pojo}，类名与 config {@code generator=pojo} 一致）：
 * entity/po/dto 等字段类产物共用，靠特性开关区分（读产物配置选项，如 {@code entity.lombok=true}）：
 * <ul>
 * <li>{@code lombok=true} → 类级 {@code @Data/@Builder/@NoArgsConstructor/@AllArgsConstructor}</li>
 * <li>{@code serializable=true} → {@code implements Serializable} + serialVersionUID</li>
 * <li>{@code jsr303=true} → NOT NULL/长度/精度 → {@code @NotNull/@Size/@Digits}</li>
 * <li>{@code jsr305=true} → nullable 列 → {@code @Nullable}</li>
 * <li>{@code enums/type} → 字段类型视图（在 {@link ArtifactGenerator#fieldType} 基类实现）</li>
 * </ul>
 */
public final class PojoGenerator extends AbstractJavaArtifactGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "pojo";

    private static final String JSR303 = "javax.validation.constraints";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        applyClassFeatures(builder, ctx);
        for (Column column : ctx.columns()) {
            builder.field(fieldFor(column, ctx));
        }
    }

    /** 成员类型视图（pojo 内部特性）：{@code type=true} 时 @type 优先；{@code enums=true} 时 enum 列 → 枚举类；否则 SQL 映射。 */
    @Override
    public String fieldType(Column column, TableContext ctx) {
        if (option(ctx, "type")) {
            Object type = column.getMeta().get("type");
            if (type != null) {
                return type.toString();
            }
        }
        if (option(ctx, "enums") && !column.getEnumValues().isEmpty()) {
            return ctx.getEnumPackage() + "." + ctx.enumClassName(column);
        }
        return super.fieldType(column, ctx);
    }

    private static boolean option(TableContext ctx, String key) {
        return Boolean.parseBoolean(ctx.getArtifactConfig().getOption(key));
    }

    /** 类级特性：lombok 注解集、Serializable。 */
    private void applyClassFeatures(Class.Builder builder, TableContext ctx) {
        if (option(ctx, "lombok")) {
            builder.annotation(Annotation.of("lombok.Data"));
            builder.annotation(Annotation.of("lombok.Builder"));
            builder.annotation(Annotation.of("lombok.NoArgsConstructor"));
            builder.annotation(Annotation.of("lombok.AllArgsConstructor"));
        }
        if (option(ctx, "serializable")) {
            builder.implement(new TypeReference("java.io.Serializable"));
            builder.field(Variable.builder()
                    .modifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .type(new TypeReference("long"))
                    .name("serialVersionUID")
                    .init(Literal.of(1L))
                    .build());
        }
    }

    /** 单字段：类型走查询契约（type/enums 视图在基类 fieldType），字段级特性加注解。 */
    private Variable fieldFor(Column column, TableContext ctx) {
        Variable.Builder builder = Variable.builder()
                .modifiers(Modifier.PRIVATE)
                .type(JavaTypes.typeTree(ctx.typeOf(column)))
                .name(ctx.fieldName(column));
        applyFieldAnnotations(builder, column, ctx);
        return builder.build();
    }

    /** 字段级特性：jsr303 约束、jsr305 空值注解。 */
    private void applyFieldAnnotations(Variable.Builder builder, Column column, TableContext ctx) {
        if (option(ctx, "jsr303")) {
            if (!column.isNullable()) {
                builder.annotation(Annotation.of(JSR303 + ".NotNull"));
            }
            String sqlType = column.getSqlType();
            if (("varchar".equals(sqlType) || "char".equals(sqlType)) && column.getLength() > 0) {
                builder.annotation(Annotation.of(JSR303 + ".Size", "max = " + column.getLength()));
            }
            if ("decimal".equals(sqlType) && column.getPrecision() > 0) {
                int integer = column.getPrecision() - column.getScale();
                builder.annotation(Annotation.of(JSR303 + ".Digits",
                        "integer = " + integer + ", fraction = " + column.getScale()));
            }
        }
        if (option(ctx, "jsr305") && column.isNullable()) {
            builder.annotation(Annotation.of(ctx.getNullableAnnotation()));
        }
    }

}
