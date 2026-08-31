package hyc.codegen.core.gen;

import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Types;
import hyc.codegen.tree.Variable;

/**
 * 索引派生查询方法构建（Mapper 与 Repository 共用；差别：@Param、视图类型由 artifact kind 决定）。
 */
final class QueryMethodFactory {

    private QueryMethodFactory() {
        throw new AssertionError("no instances");
    }

    /**
     * 构建 findBy* 方法。
     *
     * @param spec      前缀规格
     * @param ctx       表上下文（参数类型按 artifact 视图解析：mapper→POJO、repository→entity）
     * @param returnFqn 返回类型全限定名（PO 或 Entity）
     * @param nullable  {@code @Nullable} 注解全限定名
     * @param withParam 是否给参数加 {@code @Param}（MyBatis 需要）
     */
    static Method findBy(QueryMethods.Spec spec, TableContext ctx, String returnFqn,
                         String nullable, boolean withParam) {
        Method.Builder builder = Method.builder().name(spec.getMethodName());

        if (spec.isUniqueFull()) {
            builder.annotation(Annotation.of(nullable));
            builder.returnType(new TypeReference(returnFqn));
        } else {
            builder.returnType(Types.listOf(new TypeReference(returnFqn)));
        }

        for (String columnName : spec.getColumns()) {
            Column column = ctx.getTable().getColumn(columnName);
            if (column == null) {
                throw new IllegalStateException("索引列 '" + columnName + "' 在表 '" + ctx.getTable().getName()
                        + "' 中不存在（DDL 索引引用了未定义的列）");
            }
            String fieldName = ctx.fieldName(column);
            Variable.Builder param = Variable.builder()
                    .type(JavaTypes.typeTree(ctx.typeOf(column)))
                    .name(fieldName);
            if (withParam) {
                param.annotation(Annotation.of("org.apache.ibatis.annotations.Param", "\"" + fieldName + "\""));
            }
            builder.parameter(param.build());
        }

        return builder.build();
    }

}
