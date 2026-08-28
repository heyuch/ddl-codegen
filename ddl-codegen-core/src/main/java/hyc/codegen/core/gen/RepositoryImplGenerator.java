package hyc.codegen.core.gen;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.Types;
import hyc.codegen.tree.Variable;
import hyc.codegen.tree.gen.Expr;

/**
 * RepositoryImpl 生成器（kind {@code repositoryImpl}）：桥接 Mapper → Converter → Entity。
 * <p>
 * 持有 {@code @Resource Mapper} 与 {@code @Resource Converter}（di=field，config
 * {@code repositoryImpl.di=constructor} 可选）；每个 findBy* 方法调用 mapper 并把 PO 转成 Entity；
 * enum 参数从枚举类转回 DDL 值（{@code gender.value()}）再传给 Mapper。
 */
public final class RepositoryImplGenerator extends AbstractJavaArtifactGenerator {

    private static final String RESOURCE = "javax.annotation.Resource";

    @Override
    public String kind() {
        return "repositoryImpl";
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        String tableName = ctx.getTable().getName();
        String entityType = gctx.entityType(tableName);
        String poType = gctx.poType(tableName);
        String mapperType = gctx.artifactFqn(tableName, "mybatisMapper");
        String converterType = gctx.artifactFqn(tableName, "converter");
        String repositoryType = gctx.artifactFqn(tableName, "repository");
        String nullable = gctx.getConfig().getNullableAnnotation();

        builder.implement(new hyc.codegen.tree.TypeReference(repositoryType));

        String mapperField = decapitalize(simpleName(mapperType));
        String converterField = decapitalize(simpleName(converterType));

        if ("constructor".equals(ctx.getArtifactConfig().getOption("di"))) {
            builder.method(Method.builder()
                    .modifiers(Modifier.PUBLIC)
                    .name(ctx.className())
                    .parameter(Variable.builder()
                            .type(new hyc.codegen.tree.TypeReference(mapperType))
                            .name(mapperField)
                            .build())
                    .parameter(Variable.builder()
                            .type(new hyc.codegen.tree.TypeReference(converterType))
                            .name(converterField)
                            .build())
                    .body("this." + mapperField + " = " + mapperField + ";\n"
                            + "this." + converterField + " = " + converterField + ";")
                    .build());
        } else {
            builder.field(field(mapperType, mapperField));
            builder.field(field(converterType, converterField));
        }

        for (Index index : ctx.indexes()) {
            if (MetaSupport.isIgnored(index)) {
                continue;
            }
            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                builder.method(bridgeMethod(spec, ctx, entityType, poType, nullable, mapperField, converterField));
            }
        }
    }

    @Override
    protected List<hyc.codegen.tree.Import> extraImports(TableContext ctx, GenerationContext gctx) {
        List<hyc.codegen.tree.Import> imports = new ArrayList<>();
        imports.add(new hyc.codegen.tree.Import("java.util.List"));
        return imports;
    }

    private Variable field(String typeFqn, String name) {
        return Variable.builder()
                .modifiers(Modifier.PRIVATE)
                .annotation(Annotation.of(RESOURCE))
                .type(new hyc.codegen.tree.TypeReference(typeFqn))
                .name(name)
                .build();
    }

    private Method bridgeMethod(QueryMethods.Spec spec, TableContext ctx, String entityType, String poType,
            String nullable, String mapperField, String converterField) {
        String mapperMethod = spec.getMethodName();
        List<String> args = new ArrayList<>();
        Method.Builder builder = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .annotation(Annotation.of("java.lang.Override"))
                .name(spec.getMethodName());

        if (spec.isUniqueFull()) {
            builder.annotation(Annotation.of(nullable));
            builder.returnType(new hyc.codegen.tree.TypeReference(entityType));
        } else {
            builder.returnType(Types.listOf(new hyc.codegen.tree.TypeReference(entityType)));
        }

        for (String columnName : spec.getColumns()) {
            Column column = ctx.getTable().getColumn(columnName);
            String fieldName = ctx.fieldName(column);
            builder.parameter(Variable.builder()
                    .type(JavaTypes.typeTree(ctx.typeOf(column)))
                    .name(fieldName)
                    .build());
            if (column.getEnumValues().isEmpty()) {
                args.add(fieldName);
            } else {
                args.add(Expr.nullSafe(fieldName, fieldName + ".value()"));
            }
        }

        String call = mapperField + "." + mapperMethod + "(" + String.join(", ", args) + ")";
        String body;
        if (spec.isUniqueFull()) {
            body = "return " + converterField + ".toEntity(" + call + ");";
        } else {
            body = "return " + converterField + ".toEntityList(" + call + ");";
        }
        return builder.body(body).build();
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
