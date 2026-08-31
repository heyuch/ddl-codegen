package hyc.codegen.core.gen;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.model.Column;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Import;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Types;
import hyc.codegen.tree.Variable;
import hyc.codegen.tree.gen.Expr;

/**
 * Converter 生成器（注册名 {@code converter}）：source → target 逐字段映射。
 * <p>
 * source/target 为产物引用（可缺省为唯一 pojo 实例）；enum 转换方向由两端产物的
 * {@code use: enums} 决定（一端枚举类一端 String 时转换：fromValue / .value()）。
 */
public final class ConverterGenerator extends AbstractJavaGenerator {

    /**
     * 生成器注册名。
     */
    public static final String NAME = "converter";

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

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

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        String tableName = ctx.getTable().getName();
        String ownName = ctx.getArtifactName();

        ArtifactConfig source = gctx.resolveReference(ownName, "source", PojoGenerator.NAME);
        ArtifactConfig target = gctx.resolveReference(ownName, "target", PojoGenerator.NAME);

        String sourceFqn = gctx.refFqn(tableName, source);
        String targetFqn = gctx.refFqn(tableName, target);
        String sourceSimple = simpleName(sourceFqn);
        String targetSimple = simpleName(targetFqn);

        builder.method(toMethod(ctx, new Mapping(
                "to" + capitalize(targetSimple), sourceSimple, targetSimple, "source", source.getName(),
                target.getName()), gctx));
        builder.method(toMethod(ctx, new Mapping(
                "to" + capitalize(sourceSimple), targetSimple, sourceSimple, "target", target.getName(),
                source.getName()), gctx));
        builder.method(listMethod("to" + capitalize(targetSimple) + "List", targetFqn, sourceSimple, targetSimple,
                "to" + capitalize(targetSimple), "sourceList", "source"));
        builder.method(listMethod("to" + capitalize(sourceSimple) + "List", sourceFqn, targetSimple, sourceSimple,
                "to" + capitalize(sourceSimple), "targetList", "target"));
    }

    @Override
    protected List<Import> extraImports(TableContext ctx, GenerationContext gctx) {
        List<Import> imports = new ArrayList<>();
        imports.add(new Import(gctx.refFqn(ctx.getTable().getName(),
                gctx.resolveReference(ctx.getArtifactName(), "source", PojoGenerator.NAME))));
        imports.add(new Import(gctx.refFqn(ctx.getTable().getName(),
                gctx.resolveReference(ctx.getArtifactName(), "target", PojoGenerator.NAME))));
        String enumPackage = gctx.enumPackage();
        if (enumPackage != null) {
            for (Column column : ctx.columns()) {
                if (!column.getEnumValues().isEmpty()) {
                    imports.add(new Import(enumPackage + "." + ctx.enumClassName(column)));
                }
            }
        }
        imports.add(new Import("java.util.List"));
        imports.add(new Import("java.util.ArrayList"));
        return imports;
    }

    @Override
    public String kind() {
        return NAME;
    }

    private Method listMethod(String methodName, String elementFqn, String fromType, String toType,
            String convertMethod, String listParam, String itemParam) {
        String elementSimple = simpleName(elementFqn);
        List<String> stmts = new ArrayList<>();
        stmts.add("java.util.List<" + elementSimple + "> list = new java.util.ArrayList<>();");
        stmts.add("if (" + listParam + " != null) {");
        stmts.add("    for (" + fromType + " " + itemParam + " : " + listParam + ") {");
        stmts.add("        list.add(" + convertMethod + "(" + itemParam + "));");
        stmts.add("    }");
        stmts.add("}");
        stmts.add("return list;");

        return Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(Types.listOf(new TypeReference(elementSimple)))
                .name(methodName)
                .parameter(Variable.builder()
                        .type(Types.listOf(new TypeReference(fromType)))
                        .name(listParam)
                        .build())
                .body(String.join("\n", stmts))
                .build();
    }

    /**
     * 构建单对象映射方法：{@code fromType} → {@code toType}。
     */
    private Method toMethod(TableContext ctx, Mapping m, GenerationContext gctx) {
        String toVar = decapitalize(m.toType);
        List<String> stmts = new ArrayList<>();
        stmts.add(m.toType + " " + toVar + " = new " + m.toType + "();");
        TableContext fromCtx = gctx.tableContext(ctx.getTable(), m.fromName);
        TableContext toCtx = gctx.tableContext(ctx.getTable(), m.toName);
        for (Column column : ctx.columns()) {
            String field = ctx.fieldName(column);
            String getter = m.fromParam + ".get" + capitalize(field) + "()";
            String expr = getter;
            if (!column.getEnumValues().isEmpty()) {
                // 转换方向 = from/to 产物各自 fieldType 的视图差异（查询契约）
                String fromType = gctx.generatorFor(m.fromName).fieldType(column, fromCtx);
                String toType = gctx.generatorFor(m.toName).fieldType(column, toCtx);
                boolean fromEnumView = !"java.lang.String".equals(fromType);
                boolean toEnumView = !"java.lang.String".equals(toType);
                if (toEnumView && !fromEnumView) {
                    expr = Expr.nullSafe(getter, ctx.enumClassName(column) + ".fromValue(" + getter + ")");
                } else if (fromEnumView && !toEnumView) {
                    expr = Expr.nullSafe(getter, getter + ".value()");
                }
            }
            stmts.add(toVar + ".set" + capitalize(field) + "(" + expr + ");");
        }
        stmts.add("return " + toVar + ";");

        return Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new TypeReference(m.toType))
                .name(m.methodName)
                .parameter(Variable.builder().type(new TypeReference(m.fromType)).name(m.fromParam).build())
                .body(String.join("\n", stmts))
                .build();
    }

    /**
     * 单对象映射方向信息（toEnums/fromEnums：目标/源端是否枚举类视图）。
     */
    private static final class Mapping {

        final String methodName;

        final String fromType;

        final String toType;

        final String fromParam;

        final String fromName;

        final String toName;

        Mapping(String methodName, String fromType, String toType, String fromParam,
                String fromName, String toName) {
            this.methodName = methodName;
            this.fromType = fromType;
            this.toType = toType;
            this.fromParam = fromParam;
            this.fromName = fromName;
            this.toName = toName;
        }

    }

}
