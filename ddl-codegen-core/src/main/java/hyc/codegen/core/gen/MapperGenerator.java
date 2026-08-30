package hyc.codegen.core.gen;

import com.sun.source.tree.Tree.Kind;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.Types;
import hyc.codegen.tree.Variable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MyBatis Mapper 接口生成器（注册名 {@code mybatisMapper}）。
 * <p>
 * 方法集：CRUD（insert/update/deleteById）+ 主键 findById + 索引派生 findBy*（DESIGN §12）。
 * 返回类型产物 = {@code target}（缺省 = 唯一 pojo 实例；无 po 时配 {@code target=entity}）。
 * 参数视图由产物 use 决定（默认无 enums → enum 列 String）。
 */
public final class MapperGenerator extends AbstractJavaArtifactGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "mybatisMapper";

    /** {@code @Param} 注解全限定名。 */
    private static final String PARAM = "org.apache.ibatis.annotations.Param";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        builder.kind(Kind.INTERFACE);

        ArtifactConfig target = gctx.resolveReference(ctx.getArtifactName(), "target", PojoGenerator.NAME);
        String poType = gctx.refFqn(ctx.getTable().getName(), target);
        String nullable = ctx.getNullableAnnotation();
        Column id = primaryKey(ctx);

        builder.method(insertMethod(poType));
        builder.method(updateMethod(poType));
        if (id != null) {
            builder.method(deleteByIdMethod(id, ctx));
        }

        for (Index index : ctx.indexes()) {

            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                builder.method(findByMethod(spec, poType, nullable, ctx));
            }
        }
    }

    /** 主键列：PRIMARY KEY 索引首列；无主键返回 null（不生成按 id 删除）。 */
    static @Nullable Column primaryKey(TableContext ctx) {
        for (Index index : ctx.indexes()) {
            if (index.isUnique() && "PRIMARY".equalsIgnoreCase(index.getName())) {
                return ctx.getTable().getColumn(index.getColumns().get(0));
            }
        }
        return null;
    }

    private Method insertMethod(String poType) {
        return Method.builder()
                .returnType(new hyc.codegen.tree.TypeReference("int"))
                .name("insert")
                .parameter(Variable.builder()
                        .type(new hyc.codegen.tree.TypeReference(poType))
                        .name(decapitalize(simpleName(poType)))
                        .build())
                .build();
    }

    private Method updateMethod(String poType) {
        return Method.builder()
                .returnType(new hyc.codegen.tree.TypeReference("int"))
                .name("update")
                .parameter(Variable.builder()
                        .type(new hyc.codegen.tree.TypeReference(poType))
                        .name(decapitalize(simpleName(poType)))
                        .build())
                .build();
    }

    private Method deleteByIdMethod(Column id, TableContext ctx) {
        return Method.builder()
                .returnType(new hyc.codegen.tree.TypeReference("int"))
                .name("deleteById")
                .parameter(Variable.builder()
                        .annotation(Annotation.of(PARAM, "\"id\""))
                        .type(JavaTypes.typeTree(ctx.typeOf(id)))
                        .name(ctx.fieldName(id))
                        .build())
                .build();
    }

    private Method findByMethod(QueryMethods.Spec spec, String poType, String nullable, TableContext ctx) {
        Method.Builder builder = Method.builder().name(spec.getMethodName());
        if (spec.isUniqueFull()) {
            builder.annotation(Annotation.of(nullable));
            builder.returnType(new hyc.codegen.tree.TypeReference(poType));
        } else {
            builder.returnType(Types.listOf(new hyc.codegen.tree.TypeReference(poType)));
        }
        for (String columnName : spec.getColumns()) {
            Column column = ctx.getTable().getColumn(columnName);
            if (column == null) {
                throw new IllegalStateException("索引列 '" + columnName + "' 在表 '" + ctx.getTable().getName()
                        + "' 中不存在（DDL 索引引用了未定义的列）");
            }
            String fieldName = ctx.fieldName(column);
            builder.parameter(Variable.builder()
                    .annotation(Annotation.of(PARAM, "\"" + fieldName + "\""))
                    .type(JavaTypes.typeTree(ctx.typeOf(column)))
                    .name(fieldName)
                    .build());
        }
        return builder.build();
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    private static String decapitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

}
