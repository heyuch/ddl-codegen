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
public final class ConverterGenerator extends AbstractJavaArtifactGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "converter";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        String tableName = ctx.getTable().getName();
        String ownName = ctx.getArtifactName();

        ArtifactConfig source = gctx.resolveReference(ownName, "source", FieldArtifactGenerator.NAME);
        ArtifactConfig target = gctx.resolveReference(ownName, "target", FieldArtifactGenerator.NAME);

        String sourceFqn = gctx.refFqn(tableName, source);
        String targetFqn = gctx.refFqn(tableName, target);
        String sourceSimple = simpleName(sourceFqn);
        String targetSimple = simpleName(targetFqn);
        boolean sourceEnums = gctx.usesEnums(source.getName());
        boolean targetEnums = gctx.usesEnums(target.getName());

        builder.method(toMethod(ctx, new Mapping(
                "to" + capitalize(targetSimple), sourceSimple, targetSimple, "source", targetEnums, sourceEnums)));
        builder.method(toMethod(ctx, new Mapping(
                "to" + capitalize(sourceSimple), targetSimple, sourceSimple, "target", sourceEnums, targetEnums)));
        builder.method(listMethod("to" + capitalize(targetSimple) + "List", targetFqn, sourceSimple, targetSimple,
                "to" + capitalize(targetSimple), "sourceList", "source"));
        builder.method(listMethod("to" + capitalize(sourceSimple) + "List", sourceFqn, targetSimple, sourceSimple,
                "to" + capitalize(sourceSimple), "targetList", "target"));
    }

    @Override
    protected List<Import> extraImports(TableContext ctx, GenerationContext gctx) {
        List<Import> imports = new ArrayList<>();
        imports.add(new Import(gctx.refFqn(ctx.getTable().getName(),
                gctx.resolveReference(ctx.getArtifactName(), "source", FieldArtifactGenerator.NAME))));
        imports.add(new Import(gctx.refFqn(ctx.getTable().getName(),
                gctx.resolveReference(ctx.getArtifactName(), "target", FieldArtifactGenerator.NAME))));
        String enumPackage = gctx.enumPackage();
        if (enumPackage != null) {
            for (Column column : ctx.columns()) {
                if (!column.getEnumValues().isEmpty() && !MetaSupport.isIgnored(column)) {
                    imports.add(new Import(enumPackage + "." + ctx.enumClassName(column)));
                }
            }
        }
        imports.add(new Import("java.util.List"));
        imports.add(new Import("java.util.ArrayList"));
        return imports;
    }

    /** 构建单对象映射方法：{@code fromType} → {@code toType}。 */
    private Method toMethod(TableContext ctx, Mapping m) {
        String toVar = decapitalize(m.toType);
        List<String> stmts = new ArrayList<>();
        stmts.add(m.toType + " " + toVar + " = new " + m.toType + "();");
        for (Column column : ctx.columns()) {
            if (MetaSupport.isIgnored(column)) {
                continue;
            }
            String field = ctx.fieldName(column);
            String getter = m.fromParam + ".get" + capitalize(field) + "()";
            String expr = getter;
            if (!column.getEnumValues().isEmpty()) {
                if (m.toEnums && !m.fromEnums) {
                    expr = Expr.nullSafe(getter, ctx.enumClassName(column) + ".fromValue(" + getter + ")");
                } else if (m.fromEnums && !m.toEnums) {
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

    private static String decapitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** 单对象映射方向信息（toEnums/fromEnums：目标/源端是否枚举类视图）。 */
    private static final class Mapping {

        final String methodName;

        final String fromType;

        final String toType;

        final String fromParam;

        final boolean toEnums;

        final boolean fromEnums;

        Mapping(String methodName, String fromType, String toType, String fromParam,
                boolean toEnums, boolean fromEnums) {
            this.methodName = methodName;
            this.fromType = fromType;
            this.toType = toType;
            this.fromParam = fromParam;
            this.toEnums = toEnums;
            this.fromEnums = fromEnums;
        }

    }

}
