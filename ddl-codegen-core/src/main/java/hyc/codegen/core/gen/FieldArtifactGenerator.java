package hyc.codegen.core.gen;

import javax.lang.model.element.Modifier;

import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Variable;

/**
 * 纯字段类生成器：entity 与 pojo 共用（差别仅在 artifact kind，进而 TypeMapper 给不同类型视图）。
 * <p>
 * entity：enum 列 → 枚举类；pojo：enum 列 → String（固定基础类型，不进 config）。
 * {@code @ignore} 列不生成字段。
 */
public final class FieldArtifactGenerator extends AbstractJavaArtifactGenerator {

    private final String kind;

    public FieldArtifactGenerator(String kind) {
        this.kind = kind;
    }

    @Override
    public String kind() {
        return kind;
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
