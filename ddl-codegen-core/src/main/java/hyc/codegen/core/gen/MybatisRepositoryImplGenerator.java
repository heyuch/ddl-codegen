package hyc.codegen.core.gen;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.Import;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Types;
import hyc.codegen.tree.Variable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MyBatis RepositoryImpl 生成器（注册名 {@code mybatisRepositoryImpl}）：桥接 Mapper → Converter → 目标产物。
 * <p>
 * 转换规则（配置驱动，无路径查找，见 design.md）：
 * mapper.target == 自己的 target → 直连；否则经 converter（toX/toXList）转换，
 * 并校验 converter.source == mapper.target、converter.target == 自己的 target。
 * 持有 {@code @Resource Mapper} 与 {@code @Resource Converter}（di=field；config
 * {@code di=constructor} 可选）。
 */
public final class MybatisRepositoryImplGenerator extends AbstractJavaGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "mybatisRepositoryImpl";

    private static final String RESOURCE = "javax.annotation.Resource";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        String tableName = ctx.getTable().getName();
        String ownName = ctx.getArtifactName();

        ArtifactConfig target = gctx.resolveReference(ownName, "target", PojoGenerator.NAME);
        ArtifactConfig mapper = gctx.resolveReference(ownName, "mapper", MapperGenerator.NAME);
        ArtifactConfig repository = gctx.resolveReference(ownName, "repository", RepositoryGenerator.NAME);

        String targetFqn = gctx.refFqn(tableName, target);
        String mapperFqn = gctx.refFqn(tableName, mapper);
        String repositoryFqn = gctx.refFqn(tableName, repository);
        String nullable = ctx.getNullableAnnotation();

        // 转换规则：mapper.target 与自己的 target 是否一致（配置驱动，无路径查找；
        // converter 仅在需要转换时解析，直连场景不要求配置 converter）
        Bridge bridge = resolveBridge(ctx, gctx, targetFqn);

        builder.implement(new TypeReference(repositoryFqn));

        String mapperField = decapitalize(simpleName(mapperFqn));
        String converterField = bridge.convert ? decapitalize(simpleName(bridge.converterFqn())) : null;

        if ("constructor".equals(ctx.getArtifactConfig().getOption("di"))) {
            Method.Builder ctor = Method.builder()
                    .modifiers(Modifier.PUBLIC)
                    .name(ctx.className())
                    .parameter(Variable.builder().type(new TypeReference(mapperFqn)).name(mapperField).build());
            String body = "this." + mapperField + " = " + mapperField + ";";
            if (bridge.convert) {
                if (converterField == null) {
                    throw new IllegalStateException("内部不一致：convert=true 但 converterField 为空");
                }
                ctor.parameter(
                        Variable.builder().type(new TypeReference(bridge.converterFqn())).name(converterField).build());
                body += "\nthis." + converterField + " = " + converterField + ";";
            }
            builder.method(ctor.body(body).build());
        } else {
            builder.field(field(mapperFqn, mapperField));
            if (bridge.convert) {
                if (converterField == null) {
                    throw new IllegalStateException("内部不一致：convert=true 但 converterField 为空");
                }
                builder.field(field(bridge.converterFqn(), converterField));
            }
        }

        for (Index index : ctx.indexes()) {

            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                builder.method(bridgeMethod(spec, ctx, targetFqn, nullable, bridge));
            }
        }
    }

    @Override
    protected List<Import> extraImports(TableContext ctx, GenerationContext gctx) {
        List<Import> imports = new ArrayList<>();
        imports.add(new Import("java.util.List"));
        return imports;
    }

    private Variable field(String typeFqn, String name) {
        return Variable.builder()
                .modifiers(Modifier.PRIVATE)
                .annotation(Annotation.of(RESOURCE))
                .type(new TypeReference(typeFqn))
                .name(name)
                .build();
    }

    private Method bridgeMethod(QueryMethods.Spec spec, TableContext ctx, String targetFqn, String nullable,
            Bridge bridge) {
        List<String> args = new ArrayList<>();
        Method.Builder builder = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .annotation(Annotation.of("java.lang.Override"))
                .name(spec.getMethodName());

        if (spec.isUniqueFull()) {
            builder.annotation(Annotation.of(nullable));
            builder.returnType(new TypeReference(targetFqn));
        } else {
            builder.returnType(Types.listOf(new TypeReference(targetFqn)));
        }

        for (String columnName : spec.getColumns()) {
            Column column = ctx.getTable().getColumn(columnName);
            if (column == null) {
                throw new IllegalStateException("索引列 '" + columnName + "' 在表 '" + ctx.getTable().getName()
                        + "' 中不存在（DDL 索引引用了未定义的列）");
            }
            String fieldName = ctx.fieldName(column);
            builder.parameter(Variable.builder()
                    .type(JavaTypes.typeTree(ctx.typeOf(column)))
                    .name(fieldName)
                    .build());
            // 参数类型走本生成器 fieldType（SQL 映射，enum 列 String），与 mapper 参数同型，直传
            args.add(fieldName);
        }

        String call = bridge.mapperField + "." + spec.getMethodName() + "(" + String.join(", ", args) + ")";
        String body;
        if (bridge.convert) {
            String listSuffix = spec.isUniqueFull() ? "" : "List";
            body = "return " + decapitalize(simpleName(bridge.converterFqn())) + "."
                    + bridge.convertMethod() + listSuffix + "(" + call + ");";
        } else {
            body = "return " + call + ";";
        }
        return builder.body(body).build();
    }

    private Bridge resolveBridge(TableContext ctx, GenerationContext gctx, String targetFqn) {
        String ownName = ctx.getArtifactName();
        ArtifactConfig mapper = gctx.resolveReference(ownName, "mapper", MapperGenerator.NAME);
        ArtifactConfig target = gctx.resolveReference(ownName, "target", PojoGenerator.NAME);

        String mapperField = decapitalize(simpleName(gctx.refFqn(ctx.getTable().getName(), mapper)));

        ArtifactConfig mapperTarget = gctx.resolveReference(mapper.getName(), "target", PojoGenerator.NAME);
        if (mapperTarget.getName().equals(target.getName())) {
            return new Bridge(mapperField, false, null, null);
        }
        ArtifactConfig converter = gctx.resolveReference(ownName, "converter", ConverterGenerator.NAME);
        String converterFqn = gctx.refFqn(ctx.getTable().getName(), converter);
        ArtifactConfig converterSource =
                gctx.resolveReference(converter.getName(), "source", PojoGenerator.NAME);
        ArtifactConfig converterTarget =
                gctx.resolveReference(converter.getName(), "target", PojoGenerator.NAME);
        if (!converterSource.getName().equals(mapperTarget.getName())) {
            throw new IllegalStateException("产物 '" + converter.getName() + "' 的 source("
                    + converterSource.getName() + ") 应与 mapper(" + mapper.getName()
                    + ") 的 target(" + mapperTarget.getName() + ") 一致");
        }
        if (!converterTarget.getName().equals(target.getName())) {
            throw new IllegalStateException("产物 '" + converter.getName() + "' 的 target("
                    + converterTarget.getName() + ") 应与 repositoryImpl(" + ownName
                    + ") 的 target(" + target.getName() + ") 一致");
        }
        return new Bridge(mapperField, true, converterFqn, "to" + capitalize(simpleName(targetFqn)));
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

    /** 桥接解析结果：mapper.target 与自己的 target 不一致时经 converter 转换（含一致性校验）。 */
    private static final class Bridge {

        final String mapperField;

        final boolean convert;

        final @Nullable String converterFqn;

        final @Nullable String convertMethod;

        Bridge(String mapperField, boolean convert, @Nullable String converterFqn, @Nullable String convertMethod) {
            this.mapperField = mapperField;
            this.convert = convert;
            this.converterFqn = converterFqn;
            this.convertMethod = convertMethod;
        }

        /** converter 全限定名（convert=true 时必有值，由 resolveBridge 保证）。 */
        String converterFqn() {
            if (converterFqn == null) {
                throw new IllegalStateException("内部不一致：convert=true 但 converterFqn 为空");
            }
            return converterFqn;
        }

        /** converter 转换方法名（convert=true 时必有值，由 resolveBridge 保证）。 */
        String convertMethod() {
            if (convertMethod == null) {
                throw new IllegalStateException("内部不一致：convert=true 但 convertMethod 为空");
            }
            return convertMethod;
        }

    }

}
