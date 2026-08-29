package hyc.codegen.core.gen;

import javax.lang.model.element.Modifier;

import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Variable;

/**
 * 纯字段类生成器（注册名 {@code pojo}）：entity/po/dto 等字段类产物共用。
 * <p>
 * enum 列默认 String；产物 use 含 {@code enums} 时（{@link TableContext#typeOf} 按配置决定）
 * 返回枚举类全限定名。{@code @ignore} 列不生成字段。
 */
public final class FieldArtifactGenerator extends AbstractJavaArtifactGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "pojo";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        for (Column column : ctx.columns()) {
            if (MetaSupport.isIgnored(column)) {
                continue;
            }
            builder.field(Variable.builder()
                    .modifiers(Modifier.PRIVATE)
                    .type(JavaTypes.typeTree(ctx.typeOf(column)))
                    .name(ctx.fieldName(column))
                    .build());
        }
    }

}
