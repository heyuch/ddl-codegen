package hyc.codegen.core.gen;

import com.sun.source.tree.Tree.Kind;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.model.Index;
import hyc.codegen.tree.Class;

/**
 * Repository 接口生成器（注册名 {@code repository}）：索引派生 findBy*（Entity 视图，use 含 enums 时 enum 参数用枚举类）。
 */
public final class RepositoryGenerator extends AbstractJavaArtifactGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "repository";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        builder.kind(Kind.INTERFACE);

        ArtifactConfig target = gctx.resolveReference(ctx.getArtifactName(), "target", PojoGenerator.NAME);
        String returnType = gctx.refFqn(ctx.getTable().getName(), target);
        String nullable = ctx.getNullableAnnotation();

        for (Index index : ctx.indexes()) {
            if (MetaSupport.isIgnored(index)) {
                continue;
            }
            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                builder.method(QueryMethodFactory.findBy(spec, ctx, returnType, nullable, false));
            }
        }
    }

}
