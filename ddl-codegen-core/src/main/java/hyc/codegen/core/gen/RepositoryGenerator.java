package hyc.codegen.core.gen;

import com.sun.source.tree.Tree.Kind;
import hyc.codegen.core.model.Index;
import hyc.codegen.tree.Class;

/**
 * Repository 接口生成器（kind {@code repository}）：索引派生的 findBy*（Entity 视图，enum 参数用枚举类）。
 * <p>
 * 返回 {@code @Nullable Entity}（唯一键全列）或 {@code List<Entity>}；方法名与 Mapper 一致。
 */
public final class RepositoryGenerator extends AbstractJavaArtifactGenerator {

    @Override
    public String kind() {
        return "repository";
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        builder.kind(Kind.INTERFACE);

        String entityType = gctx.entityType(ctx.getTable().getName());
        String nullable = gctx.getConfig().getNullableAnnotation();

        for (Index index : ctx.indexes()) {
            if (MetaSupport.isIgnored(index)) {
                continue;
            }
            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                builder.method(QueryMethodFactory.findBy(spec, ctx, entityType, nullable, false));
            }
        }
    }

}
