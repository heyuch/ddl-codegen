package hyc.codegen.core.gen;

import com.sun.source.tree.Tree.Kind;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.ParameterizedType;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Types;
import hyc.codegen.tree.Variable;

/**
 * MyBatis Mapper 接口生成器（kind {@code mybatisMapper}）。
 * <p>
 * 方法集：CRUD（insert/update/deleteById）+ 主键 findById + 索引派生的 findBy*（DESIGN §12）。
 * 视图为 POJO：enum 列参数 → String；返回 PO（pojo 未启用时回退 entity）。
 */
public final class MapperGenerator extends AbstractJavaArtifactGenerator {

    /** {@code @Param} 注解全限定名。 */
    private static final String PARAM = "org.apache.ibatis.annotations.Param";

    @Override
    public String kind() {
        return "mybatisMapper";
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        builder.kind(Kind.INTERFACE);

        String poType = gctx.poType(ctx.getTable().getName());
        String nullable = gctx.getConfig().getNullableAnnotation();
        Column id = primaryKey(ctx);

        builder.method(insertMethod(poType));
        builder.method(updateMethod(poType));
        if (id != null) {
            builder.method(deleteByIdMethod(id, ctx));
        }

        for (Index index : ctx.indexes()) {
            if (MetaSupport.isIgnored(index)) {
                continue;
            }
            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                builder.method(findByMethod(spec, poType, nullable, ctx));
            }
        }
    }

    /** 主键列：PRIMARY KEY 索引首列；无主键返回 null（不生成按 id 删除）。 */
    static Column primaryKey(TableContext ctx) {
        for (Index index : ctx.indexes()) {
            if (index.isUnique() && "PRIMARY".equalsIgnoreCase(index.getName())) {
                return ctx.getTable().getColumn(index.getColumns().get(0));
            }
        }
        return null;
    }

    private Method insertMethod(String poType) {
        return Method.builder()
                .returnType(new TypeReference("int"))
                .name("insert")
                .parameter(Variable.builder()
                        .type(new TypeReference(poType))
                        .name("po")
                        .build())
                .build();
    }

    private Method updateMethod(String poType) {
        return Method.builder()
                .returnType(new TypeReference("int"))
                .name("update")
                .parameter(Variable.builder()
                        .type(new TypeReference(poType))
                        .name("po")
                        .build())
                .build();
    }

    private Method deleteByIdMethod(Column id, TableContext ctx) {
        return Method.builder()
                .returnType(new TypeReference("int"))
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
            builder.returnType(new TypeReference(poType));
        } else {
            builder.returnType(listOf(poType));
        }
        for (String columnName : spec.getColumns()) {
            Column column = ctx.getTable().getColumn(columnName);
            String fieldName = ctx.fieldName(column);
            builder.parameter(Variable.builder()
                    .annotation(Annotation.of(PARAM, "\"" + fieldName + "\""))
                    .type(JavaTypes.typeTree(ctx.typeOf(column)))
                    .name(fieldName)
                    .build());
        }
        return builder.build();
    }

    private static ParameterizedType listOf(String elementFqn) {
        return Types.listOf(new TypeReference(elementFqn));
    }

}
