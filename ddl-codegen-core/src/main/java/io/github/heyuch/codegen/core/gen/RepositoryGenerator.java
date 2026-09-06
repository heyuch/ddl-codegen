package io.github.heyuch.codegen.core.gen;

import javax.lang.model.type.TypeKind;

import com.sun.source.tree.Tree.Kind;
import io.github.heyuch.codegen.core.config.ArtifactConfig;
import io.github.heyuch.codegen.core.model.Column;
import io.github.heyuch.codegen.core.model.Index;
import io.github.heyuch.codegen.tree.Class;
import io.github.heyuch.codegen.tree.DocComment;
import io.github.heyuch.codegen.tree.Method;
import io.github.heyuch.codegen.tree.PrimitiveType;
import io.github.heyuch.codegen.tree.TypeReference;
import io.github.heyuch.codegen.tree.Variable;

/**
 * Repository 接口生成器（注册名 {@code repository}）：索引派生 findBy*（Entity 视图，use 含 enums 时 enum 参数用枚举类）。
 * <p>
 * 20260906-13：补数据更新类方法 insert/update/deleteById（无条件，deleteById 判据同 mapper 单列主键）；
 * `springCache=true` 且表有索引（存在可缓存查询）时追加 evictCaches 声明（无注解，缓存注解全在 impl）。
 */
public final class RepositoryGenerator extends AbstractJavaGenerator {

    /**
     * 生成器注册名。
     */
    public static final String NAME = "repository";

    private static String decapitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    /** springCache 开关（repository 产物配置）；缓存产物仅在表存在索引（有可缓存查询）时生效。 */
    static boolean springCacheEnabled(TableContext ctx) {
        return Boolean.parseBoolean(ctx.getArtifactConfig().getOption("springCache"))
                && !ctx.indexes().isEmpty();
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        builder.kind(Kind.INTERFACE);
        CommentDocs.classDoc(builder, ctx.tableComment());

        ArtifactConfig target = gctx.resolveReference(ctx.getArtifactName(), "target", PojoGenerator.NAME);
        String targetFqn = gctx.refFqn(ctx.getTable().getName(), target);
        String entityParam = decapitalize(simpleName(targetFqn));

        builder.method(crudMethod("insert", "插入记录", targetFqn, entityParam));
        builder.method(crudMethod("update", "更新记录", targetFqn, entityParam));
        Column pk = MapperGenerator.primaryKey(ctx);
        if (pk != null) {
            builder.method(deleteByIdMethod(pk, ctx));
        }
        if (springCacheEnabled(ctx)) {
            builder.method(evictCachesMethod(targetFqn, entityParam));
        }

        String nullable = ctx.getNullableAnnotation();
        for (Index index : ctx.indexes()) {
            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                builder.method(QueryMethodFactory.findBy(spec, ctx, targetFqn, nullable, false));
            }
        }
    }

    /** insert/update 声明：target 视图入参、int 返回（接口方法无 public，见既有 findBy 约定）。 */
    private Method crudMethod(String name, String doc, String targetFqn, String entityParam) {
        return Method.builder()
                .returnType(new TypeReference("int"))
                .name(name)
                .javadoc(DocComment.builder().summary(doc).build())
                .parameter(Variable.builder()
                        .type(new TypeReference(targetFqn))
                        .name(entityParam)
                        .build())
                .build();
    }

    /** deleteById 声明：与 MapperGenerator.primaryKey 判据一致（PRIMARY 索引首列，单列主键为支持面）。 */
    private Method deleteByIdMethod(Column pk, TableContext ctx) {
        String fieldName = ctx.fieldName(pk);
        return Method.builder()
                .returnType(new TypeReference("int"))
                .name("deleteById")
                .javadoc(DocComment.builder().summary("按主键删除记录").build())
                .parameter(Variable.builder()
                        .type(JavaTypes.typeTree(ctx.typeOf(pk)))
                        .name(fieldName)
                        .build())
                .build();
    }

    /** evictCaches 声明：手动清理入口（impl 上挂 @CacheEvict 组并实现日志）。 */
    private Method evictCachesMethod(String targetFqn, String entityParam) {
        return Method.builder()
                .returnType(new PrimitiveType(TypeKind.VOID))
                .name("evictCaches")
                .javadoc(DocComment.builder().summary("清理该实体相关的缓存键").build())
                .parameter(Variable.builder()
                        .type(new TypeReference(targetFqn))
                        .name(entityParam)
                        .build())
                .build();
    }

    @Override
    public String kind() {
        return NAME;
    }

}
