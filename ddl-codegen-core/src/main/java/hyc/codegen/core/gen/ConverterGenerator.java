package hyc.codegen.core.gen;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Import;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Types;
import hyc.codegen.tree.Variable;

/**
 * Converter 生成器（kind {@code converter}，plain 风格）：逐字段在 POJO ↔ Entity 间赋值。
 * <p>
 * enum 列转换：toEntity 用 {@code Gender.fromValue(...)}（String → 枚举），toPo 用 {@code .value()}（枚举 → DDL 值）。
 * 方法体引用的类型（Entity/枚举/List/ArrayList）经 {@link #extraImports} 显式登记 import。
 */
public final class ConverterGenerator extends AbstractJavaArtifactGenerator {

    @Override
    public String kind() {
        return "converter";
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        String tableName = ctx.getTable().getName();
        String entityType = gctx.entityType(tableName);
        String poType = gctx.poType(tableName);
        String entitySimple = simpleName(entityType);
        String poSimple = simpleName(poType);

        builder.method(toEntityMethod(ctx, entitySimple, poSimple));
        builder.method(toPoMethod(ctx, entitySimple, poSimple));
        builder.method(listMethod("toEntityList", poType, poSimple, "toEntity", "poList", "po"));
        builder.method(listMethod("toPoList", entityType, entitySimple, "toPo", "entityList", "entity"));
    }

    @Override
    protected List<Import> extraImports(TableContext ctx, GenerationContext gctx) {
        String tableName = ctx.getTable().getName();
        List<Import> imports = new ArrayList<>();
        imports.add(new Import(gctx.entityType(tableName)));
        imports.add(new Import(gctx.poType(tableName)));
        for (Column column : ctx.columns()) {
            if (MetaSupport.isIgnored(column) || column.getEnumValues().isEmpty()) {
                continue;
            }
            imports.add(new Import(enumFqn(ctx, gctx, column)));
        }
        imports.add(new Import("java.util.List"));
        imports.add(new Import("java.util.ArrayList"));
        return imports;
    }

    private String enumFqn(TableContext ctx, GenerationContext gctx, Column column) {
        String enumPkg = gctx.getConfig()
                .artifact("enum")
                .map(a -> a.getPkg())
                .orElse(ctx.packageName());
        return enumPkg + "." + ctx.enumClassName(column);
    }

    private Method toEntityMethod(TableContext ctx, String entitySimple, String poSimple) {
        List<String> stmts = new ArrayList<>();
        stmts.add(entitySimple + " u = new " + entitySimple + "();");
        for (Column column : ctx.columns()) {
            if (MetaSupport.isIgnored(column)) {
                continue;
            }
            String field = ctx.fieldName(column);
            String getter = "po.get" + capitalize(field) + "()";
            String expr = column.getEnumValues().isEmpty()
                    ? getter
                    : hyc.codegen.tree.gen.Expr.nullSafe(getter,
                            ctx.enumClassName(column) + ".fromValue(" + getter + ")");
            stmts.add("u.set" + capitalize(field) + "(" + expr + ");");
        }
        stmts.add("return u;");

        return Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new TypeReference(entitySimple))
                .name("toEntity")
                .parameter(Variable.builder().type(new TypeReference(poSimple)).name("po").build())
                .body(String.join("\n", stmts))
                .build();
    }

    private Method toPoMethod(TableContext ctx, String entitySimple, String poSimple) {
        List<String> stmts = new ArrayList<>();
        stmts.add(poSimple + " po = new " + poSimple + "();");
        for (Column column : ctx.columns()) {
            if (MetaSupport.isIgnored(column)) {
                continue;
            }
            String field = ctx.fieldName(column);
            String getter = "entity.get" + capitalize(field) + "()";
            String expr = column.getEnumValues().isEmpty()
                    ? getter
                    : hyc.codegen.tree.gen.Expr.nullSafe(getter, getter + ".value()");
            stmts.add("po.set" + capitalize(field) + "(" + expr + ");");
        }
        stmts.add("return po;");

        return Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new TypeReference(poSimple))
                .name("toPo")
                .parameter(Variable.builder().type(new TypeReference(entitySimple)).name("entity").build())
                .body(String.join("\n", stmts))
                .build();
    }

    private Method listMethod(String name, String elementFqn, String itemType,
            String convertMethod, String listParam, String itemParam) {
        String elementSimple = simpleName(elementFqn);
        List<String> stmts = new ArrayList<>();
        stmts.add("java.util.List<" + elementSimple + "> list = new java.util.ArrayList<>();");
        stmts.add("if (" + listParam + " != null) {");
        stmts.add("    for (" + itemType + " " + itemParam + " : " + listParam + ") {");
        stmts.add("        list.add(" + convertMethod + "(" + itemParam + "));");
        stmts.add("    }");
        stmts.add("}");
        stmts.add("return list;");

        return Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(Types.listOf(new TypeReference(elementSimple)))
                .name(name)
                .parameter(Variable.builder()
                        .type(Types.listOf(new TypeReference(simpleName(elementFqn))))
                        .name(listParam)
                        .build())
                .body(String.join("\n", stmts))
                .build();
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

}
