package io.github.heyuch.codegen.core.gen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

import io.github.heyuch.codegen.core.config.ArtifactConfig;
import io.github.heyuch.codegen.core.model.Column;
import io.github.heyuch.codegen.core.model.Index;
import io.github.heyuch.codegen.tree.Annotation;
import io.github.heyuch.codegen.tree.Class;
import io.github.heyuch.codegen.tree.DocComment;
import io.github.heyuch.codegen.tree.Import;
import io.github.heyuch.codegen.tree.Method;
import io.github.heyuch.codegen.tree.PrimitiveType;
import io.github.heyuch.codegen.tree.SourceExpr;
import io.github.heyuch.codegen.tree.TypeReference;
import io.github.heyuch.codegen.tree.Types;
import io.github.heyuch.codegen.tree.Variable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MyBatis RepositoryImpl 生成器（注册名 {@code mybatisRepositoryImpl}）：桥接 Mapper → Converter → 目标产物。
 * <p>
 * 转换规则（配置驱动，无路径查找，见 design.md）：
 * mapper.target == 自己的 target → 直连；否则经 converter（toX/toXList）转换，
 * 并校验 converter.source == mapper.target、converter.target == 自己的 target。
 * 持有 {@code @Resource Mapper} 与 {@code @Resource Converter}（di=field；config
 * {@code di=constructor} 可选）。
 * <p>
 * 20260906-13：repository 层补写方法 insert/update/deleteById（与 Mapper 语义对齐）；
 * `springCache=true`（repository 产物配置）且表有索引时生成 cache-aside：findBy* 加 @Cacheable、
 * evictCaches(Entity)（接口声明 + 本类实现：@CacheEvict 组与日志行同 spec 循环产出）、写方法 fetch-first
 * 后经 {@code @Lazy} 自引用代理调 evictCaches、类去 final。日志双形态：repositoryImpl 产物
 * {@code lombok=true} → 类级 @Slf4j；否则手写 Logger 常量置类顶。converter 桥接模式 po 类型
 * import 简单名（简单名冲突时回退 FQN）。本生成器实例无表态（跨表复用）。
 */
// 编排方法组合了字段序/方法序/开关/循环，fanout 高；豁免优于改动静态检查配置
@SuppressWarnings("ClassFanOutComplexity")
public final class MybatisRepositoryImplGenerator extends AbstractJavaGenerator {

    /**
     * 生成器注册名。
     */
    public static final String NAME = "mybatisRepositoryImpl";

    private static final String RESOURCE = "javax.annotation.Resource";
    private static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
    private static final String LAZY = "org.springframework.context.annotation.Lazy";
    private static final String CACHEABLE = "org.springframework.cache.annotation.Cacheable";
    private static final String CACHE_EVICT = "org.springframework.cache.annotation.CacheEvict";
    private static final String CACHING = "org.springframework.cache.annotation.Caching";
    private static final String SLF4J = "lombok.extern.slf4j.Slf4j";
    private static final String OVERRIDE = "java.lang.Override";

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

    private static String getter(String field) {
        return "get" + capitalize(field) + "()";
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    private static Column specColumn(TableContext ctx, String columnName) {
        Column column = ctx.getTable().getColumn(columnName);
        if (column == null) {
            throw new IllegalStateException("索引列 '" + columnName + "' 在表 '" + ctx.getTable().getName()
                    + "' 中不存在（DDL 索引引用了未定义的列）");
        }
        return column;
    }

    /** 字段与构造器：Logger 常量置顶（非 lombok 形态）→ mapper → converter → self（cache）。 */
    private void addFields(Class.Builder builder, TableContext ctx, Plan plan,
            String mapperFqn, String repositoryFqn) {
        Bridge bridge = plan.bridge;
        String mapperField = bridge.mapperField;
        if (plan.cache && !plan.implLombok) {
            builder.field(logField(ctx));
        }
        if ("constructor".equals(ctx.getArtifactConfig().getOption("di"))) {
            builder.field(fieldFinal(mapperFqn, mapperField, "数据访问 Mapper"));
            Method.Builder ctor = Method.builder()
                    .modifiers(Modifier.PUBLIC)
                    .name(ctx.className())
                    .parameter(Variable.builder().type(new TypeReference(mapperFqn)).name(mapperField).build());
            String body = "this." + mapperField + " = " + mapperField + ";";
            if (bridge.convert) {
                String converterField = bridge.converterField();
                builder.field(fieldFinal(bridge.converterFqn(), converterField, "实体转换器"));
                ctor.parameter(Variable.builder()
                        .type(new TypeReference(bridge.converterFqn()))
                        .name(converterField)
                        .build());
                body += "\nthis." + converterField + " = " + converterField + ";";
            }
            builder.method(ctor.body(body).build());
        } else {
            builder.field(field(mapperFqn, mapperField));
            if (bridge.convert) {
                builder.field(field(bridge.converterFqn(), bridge.converterField()));
            }
        }
        if (plan.cache) {
            builder.field(Variable.builder()
                    .modifiers(Modifier.PRIVATE)
                    .annotation(Annotation.of(AUTOWIRED))
                    .annotation(Annotation.of(LAZY))
                    .type(new TypeReference(repositoryFqn))
                    .name("self")
                    .javadoc(DocComment.builder().summary("自引用代理（内部调用经此触发缓存清理）").build())
                    .build());
        }
    }

    /** find 循环：springCache 时给每个方法加 @Cacheable。 */
    private void addFindMethods(Class.Builder builder, TableContext ctx, Plan plan,
            String nullable, GenerationContext gctx) {
        for (Index index : ctx.indexes()) {
            for (QueryMethods.Spec spec : QueryMethods.of(index, gctx.getNaming())) {
                Annotation cacheable = null;
                if (plan.cache) {
                    cacheable = cacheableAnnotation(ctx, spec, plan);
                }
                builder.method(bridgeMethod(ctx, spec, nullable, plan, cacheable));
            }
        }
    }

    /** 写方法：insert/update/deleteById/evictCaches（cache 形态见各 body 组装）。 */
    private void addWriteMethods(Class.Builder builder, TableContext ctx, Plan plan) {
        builder.method(crudMethod("insert", "插入记录", plan));
        builder.method(crudMethod("update", "更新记录", plan));
        if (plan.pk != null) {
            builder.method(deleteByIdMethod(ctx, plan, plan.pk));
        }
        if (plan.cache) {
            builder.method(evictCachesMethod(ctx, plan));
        }
    }

    private Method bridgeMethod(TableContext ctx, QueryMethods.Spec spec, String nullable,
            Plan plan, @Nullable Annotation cacheable) {
        List<String> args = new ArrayList<>();
        Method.Builder builder = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .name(spec.getMethodName());
        if (cacheable != null) {
            builder.annotation(cacheable);
        }
        builder.annotation(Annotation.of(OVERRIDE));
        builder.javadoc(DocComment.builder()
                .summary(CommentDocs.findBySummary(ctx, spec.getColumns()))
                .build());

        if (spec.isUniqueFull()) {
            builder.annotation(Annotation.of(nullable));
            builder.returnType(new TypeReference(plan.targetFqn));
        } else {
            builder.returnType(Types.listOf(new TypeReference(plan.targetFqn)));
        }

        for (String columnName : spec.getColumns()) {
            Column column = specColumn(ctx, columnName);
            String fieldName = ctx.fieldName(column);
            builder.parameter(Variable.builder()
                    .type(JavaTypes.typeTree(ctx.typeOf(column)))
                    .name(fieldName)
                    .build());
            args.add(fieldName);
        }

        String call = plan.bridge.mapperField + "." + spec.getMethodName() + "("
                + String.join(", ", args) + ")";
        String body;
        if (plan.bridge.convert) {
            String converterCall = plan.bridge.converterField() + "." + plan.bridge.convertMethod();
            if (spec.isUniqueFull()) {
                String tmp = "po";
                body = plan.poRef + " " + tmp + " = " + call + ";\n"
                        + "return " + tmp + " == null ? null : " + converterCall + "(" + tmp + ");";
            } else {
                body = "return " + converterCall + "List(" + call + ");";
            }
        } else {
            body = "return " + call + ";";
        }
        return builder.body(body).build();
    }

    @Override
    protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
        String tableName = ctx.getTable().getName();
        String ownName = ctx.getArtifactName();
        CommentDocs.classDoc(builder, ctx.tableComment());

        ArtifactConfig mapper = gctx.resolveReference(ownName, "mapper", MapperGenerator.NAME);
        ArtifactConfig target = gctx.resolveReference(ownName, "target", PojoGenerator.NAME);
        ArtifactConfig repository = gctx.resolveReference(ownName, "repository", RepositoryGenerator.NAME);

        String targetFqn = gctx.refFqn(tableName, target);
        String mapperFqn = gctx.refFqn(tableName, mapper);
        String repositoryFqn = gctx.refFqn(tableName, repository);
        Bridge bridge = resolveBridge(ctx, gctx, targetFqn);
        Plan plan = resolvePlan(ctx, gctx, repository, targetFqn, mapperFqn, repositoryFqn, bridge);

        builder.implement(new TypeReference(repositoryFqn));
        if (plan.cache && plan.implLombok) {
            builder.annotation(Annotation.of(SLF4J));
        }
        addFields(builder, ctx, plan, mapperFqn, repositoryFqn);
        addWriteMethods(builder, ctx, plan);
        addFindMethods(builder, ctx, plan, ctx.getNullableAnnotation(), gctx);
    }

    /** Java 表达式复算 key（evictCaches 日志用）："method:" + user.getX()[, "," + user.getY()]。 */
    private String cacheKeyJava(TableContext ctx, QueryMethods.Spec spec, Plan plan) {
        List<String> parts = new ArrayList<>();
        for (String columnName : spec.getColumns()) {
            parts.add(plan.entityParam + "." + getter(ctx.fieldName(specColumn(ctx, columnName))));
        }
        return "\"" + spec.getMethodName() + ":\"" + " + "
                + String.join(" + \",\" + ", parts);
    }

    /** SpEL key：'{methodName}:{arg0},{arg1},…'（cacheNames 已隔离表，无表名前缀）。 */
    private String cacheKeySpel(TableContext ctx, QueryMethods.Spec spec, Plan plan, boolean entityScope) {
        List<String> parts = new ArrayList<>();
        for (String columnName : spec.getColumns()) {
            String field = ctx.fieldName(specColumn(ctx, columnName));
            parts.add(entityScope ? "#" + plan.entityParam + "." + field : "#" + field);
        }
        return "'" + spec.getMethodName() + ":' + " + String.join(" + ',' + ", parts);
    }

    private Annotation cacheableAnnotation(TableContext ctx, QueryMethods.Spec spec, Plan plan) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("cacheNames", plan.cacheName);
        args.put("key", cacheKeySpel(ctx, spec, plan, false));
        return Annotation.of(CACHEABLE, args);
    }

    /**
     * evictCaches 上单个 @Caching(evict={...})：@CacheEvict 在 Spring 5.3 不可重复，须容器注解包裹。
     * 用户定稿格式：每个 @CacheEvict 独立一行（12 空格缩进、`cacheNames = "...", key = "..."` 带空格）。
     */
    private Annotation cachingEvictAnnotation(TableContext ctx, Plan plan) {
        StringBuilder text = new StringBuilder("evict = {");
        for (int i = 0; i < plan.specs.size(); i++) {
            QueryMethods.Spec spec = plan.specs.get(i);
            text.append("\n        @CacheEvict(cacheNames = \"")
                    .append(plan.cacheName)
                    .append("\", key = \"")
                    .append(cacheKeySpel(ctx, spec, plan, true))
                    .append("\")");
            if (i < plan.specs.size() - 1) {
                text.append(",");
            }
        }
        text.append("\n}");
        return Annotation.of(new TypeReference(CACHING),
                java.util.Collections.singletonList(text.toString()));
    }

    private Method crudMethod(String name, String doc, Plan plan) {
        return Method.builder()
                .modifiers(Modifier.PUBLIC)
                .annotation(Annotation.of(OVERRIDE))
                .returnType(new TypeReference("int"))
                .name(name)
                .javadoc(DocComment.builder().summary(doc).build())
                .parameter(Variable.builder()
                        .type(new TypeReference(plan.targetFqn))
                        .name(plan.entityParam)
                        .build())
                .body(writeBody(name, plan))
                .build();
    }

    /** deleteById 方法：cache 且单列主键时先查老行，删成功后用老行清理。 */
    private Method deleteByIdMethod(TableContext ctx, Plan plan, Column pk) {
        List<String> body = new ArrayList<>();
        if (plan.cache && plan.pkFindName != null) {
            body.add(plan.poVarType() + " old = "
                    + mapperCall(plan, plan.pkFindName, plan.pkFieldName()) + ";");
            body.add("int rows = " + mapperCall(plan, "deleteById", plan.pkFieldName()) + ";");
            body.add("if (rows > 0 && old != null) {");
            body.add("    self.evictCaches(" + toEntityCall(plan, "old") + ");");
            body.add("}");
            body.add("return rows;");
        } else {
            body.add("return " + mapperCall(plan, "deleteById", plan.pkFieldName()) + ";");
        }
        return Method.builder()
                .modifiers(Modifier.PUBLIC)
                .annotation(Annotation.of(OVERRIDE))
                .returnType(new TypeReference("int"))
                .name("deleteById")
                .javadoc(DocComment.builder().summary("按主键删除记录").build())
                .parameter(Variable.builder()
                        .type(JavaTypes.typeTree(ctx.typeOf(pk)))
                        .name(plan.pkFieldName())
                        .build())
                .body(String.join("\n", body))
                .build();
    }

    /** evictCaches 实现：@CacheEvict 组与 log 行均按 spec 循环产出（与 find/@Cacheable 同源）。 */
    private Method evictCachesMethod(TableContext ctx, Plan plan) {
        Method.Builder builder = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new PrimitiveType(TypeKind.VOID))
                .name("evictCaches")
                .javadoc(DocComment.builder().summary("清理该实体相关的缓存键").build())
                .parameter(Variable.builder()
                        .type(new TypeReference(plan.targetFqn))
                        .name(plan.entityParam)
                        .build());
        builder.annotation(cachingEvictAnnotation(ctx, plan));
        builder.annotation(Annotation.of(OVERRIDE));
        List<String> body = new ArrayList<>();
        for (QueryMethods.Spec spec : plan.specs) {
            body.add("log.info(\"evictCaches: {}:{}\", \"" + plan.cacheName + "\", "
                    + cacheKeyJava(ctx, spec, plan) + ");");
        }
        return builder.body(String.join("\n", body)).build();
    }

    @Override
    protected List<Import> extraImports(TableContext ctx, GenerationContext gctx) {
        List<Import> imports = new ArrayList<>();
        imports.add(new Import("java.util.List"));
        // 手写 Logger 常量（非 lombok 形态）：Logger 类型 import 由字段类型自动收集，LoggerFactory 引用在 init 文本需显式登记
        boolean cache = Boolean.parseBoolean(gctx.resolveReference(ctx.getArtifactName(), "repository",
                RepositoryGenerator.NAME).getOption("springCache")) && !ctx.indexes().isEmpty();
        boolean implLombok = Boolean.parseBoolean(ctx.getArtifactConfig().getOption("lombok"));
        if (cache && !implLombok) {
            imports.add(new Import("org.slf4j.LoggerFactory"));
        }
        if (cache) {
            // @Caching(evict={@CacheEvict(...)}) 内注解以简单名写在原始文本里，import 需显式登记
            imports.add(new Import(CACHE_EVICT));
        }
        String poFqn = poFqnIfImported(ctx, gctx);
        if (poFqn != null) {
            imports.add(new Import(poFqn));
        }
        return imports;
    }

    private Variable field(String typeFqn, String name) {
        String doc = name.endsWith("Mapper") ? "数据访问 Mapper" : "实体转换器";
        return Variable.builder()
                .modifiers(Modifier.PRIVATE)
                .annotation(Annotation.of(RESOURCE))
                .type(new TypeReference(typeFqn))
                .name(name)
                .javadoc(DocComment.builder().summary(doc).build())
                .build();
    }

    /** 构造注入字段：final 无注解。 */
    private Variable fieldFinal(String typeFqn, String name, String doc) {
        return Variable.builder()
                .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                .type(new TypeReference(typeFqn))
                .name(name)
                .javadoc(DocComment.builder().summary(doc).build())
                .build();
    }

    private String insertBody(Plan plan) {
        List<String> body = new ArrayList<>();
        if (plan.bridge.convert) {
            String poVar = "userPo";
            body.add(plan.poRef + " " + poVar + " = " + toPoCall(plan, plan.entityParam) + ";");
            body.add("int rows = " + mapperCall(plan, "insert", poVar) + ";");
            body.add("if (rows > 0) {");
            body.add("    self.evictCaches(" + toEntityCall(plan, poVar) + ");");
        } else {
            body.add("int rows = " + mapperCall(plan, "insert", plan.entityParam) + ";");
            body.add("if (rows > 0) {");
            body.add("    self.evictCaches(" + plan.entityParam + ");");
        }
        body.add("}");
        body.add("return rows;");
        return String.join("\n", body);
    }

    @Override
    public String kind() {
        return NAME;
    }

    private Variable logField(TableContext ctx) {
        return Variable.builder()
                .modifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .type(new TypeReference("org.slf4j.Logger"))
                .name("log")
                .init(new SourceExpr("LoggerFactory.getLogger(" + ctx.className() + ".class)"))
                .javadoc(DocComment.builder().summary("日志").build())
                .build();
    }

    private String mapperCall(Plan plan, String methodName, String arg) {
        return plan.bridge.mapperField + "." + methodName + "(" + arg + ")";
    }

    /** converter 桥接且 po 简单名不冲突 → po FQN（extraImports 独立重算：生成器实例跨表复用）。 */
    private @Nullable String poFqnIfImported(TableContext ctx, GenerationContext gctx) {
        String ownName = ctx.getArtifactName();
        ArtifactConfig mapper = gctx.resolveReference(ownName, "mapper", MapperGenerator.NAME);
        ArtifactConfig target = gctx.resolveReference(ownName, "target", PojoGenerator.NAME);
        ArtifactConfig repository = gctx.resolveReference(ownName, "repository", RepositoryGenerator.NAME);
        ArtifactConfig mapperTarget = gctx.resolveReference(mapper.getName(), "target", PojoGenerator.NAME);
        if (mapperTarget.getName().equals(target.getName())) {
            return null;
        }
        ArtifactConfig converter = gctx.resolveReference(ownName, "converter", ConverterGenerator.NAME);
        String mapperReturnFqn = gctx.refFqn(ctx.getTable().getName(), mapperTarget);
        String poSimple = simpleName(mapperReturnFqn);
        String tableName = ctx.getTable().getName();
        boolean collide = poSimple.equals(simpleName(gctx.refFqn(tableName, target)))
                || poSimple.equals(simpleName(gctx.refFqn(tableName, mapper)))
                || poSimple.equals(simpleName(gctx.refFqn(tableName, converter)))
                || poSimple.equals(simpleName(gctx.refFqn(tableName, repository)));
        return collide ? null : mapperReturnFqn;
    }

    /** po 引用：convert 且简单名与已引用类型不冲突 → 简单名（import）；否则 FQN/直连 null。 */
    private @Nullable String poRef(Bridge bridge, String targetFqn, String mapperFqn, String repositoryFqn) {
        if (!bridge.convert) {
            return null;
        }
        String poSimple = simpleName(bridge.mapperReturnFqn);
        boolean collide = poSimple.equals(simpleName(targetFqn))
                || poSimple.equals(simpleName(mapperFqn))
                || poSimple.equals(simpleName(bridge.converterFqn()))
                || poSimple.equals(simpleName(repositoryFqn));
        return collide ? bridge.mapperReturnFqn : poSimple;
    }

    private Bridge resolveBridge(TableContext ctx, GenerationContext gctx, String targetFqn) {
        String ownName = ctx.getArtifactName();
        ArtifactConfig mapper = gctx.resolveReference(ownName, "mapper", MapperGenerator.NAME);
        ArtifactConfig target = gctx.resolveReference(ownName, "target", PojoGenerator.NAME);

        String mapperField = decapitalize(simpleName(gctx.refFqn(ctx.getTable().getName(), mapper)));

        ArtifactConfig mapperTarget = gctx.resolveReference(mapper.getName(), "target", PojoGenerator.NAME);
        String mapperReturnFqn = gctx.refFqn(ctx.getTable().getName(), mapperTarget);
        if (mapperTarget.getName().equals(target.getName())) {
            return new Bridge(mapperField, false, null, null, null, mapperReturnFqn);
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
        return new Bridge(mapperField, true, converterFqn,
                "to" + capitalize(simpleName(targetFqn)),
                "to" + capitalize(simpleName(mapperReturnFqn)), mapperReturnFqn);
    }

    /** 生成计划派生：开关读取、po 引用、主键、规格清单（与 find 循环同源）。 */
    private Plan resolvePlan(TableContext ctx, GenerationContext gctx, ArtifactConfig repository,
            String targetFqn, String mapperFqn, String repositoryFqn, Bridge bridge) {
        String tableName = ctx.getTable().getName();
        String cacheName = repository.getOption("cacheName");
        if (cacheName == null) {
            cacheName = ctx.getNaming().tableClassName(tableName);
        }
        String entityParam = decapitalize(simpleName(targetFqn));
        String poRef = poRef(bridge, targetFqn, mapperFqn, repositoryFqn);
        Column pk = MapperGenerator.primaryKey(ctx);
        String pkFindName = null;
        List<QueryMethods.Spec> specs = new ArrayList<>();
        for (Index index : ctx.indexes()) {
            List<QueryMethods.Spec> indexSpecs = QueryMethods.of(index, gctx.getNaming());
            specs.addAll(indexSpecs);
            if (pkFindName == null && index.isUnique() && "PRIMARY".equalsIgnoreCase(index.getName())
                    && index.getColumns().size() == 1) {
                pkFindName = indexSpecs.get(indexSpecs.size() - 1).getMethodName();
            }
        }
        boolean implLombok = Boolean.parseBoolean(ctx.getArtifactConfig().getOption("lombok"));
        boolean cache = Boolean.parseBoolean(repository.getOption("springCache")) && !ctx.indexes().isEmpty();
        return new Plan(cache, implLombok, cacheName, entityParam, targetFqn, poRef, bridge, pk,
                pk == null ? null : ctx.fieldName(pk), pkFindName, specs);
    }

    private String toEntityCall(Plan plan, String poExpr) {
        return plan.bridge.converterField() + "." + plan.bridge.convertMethod() + "(" + poExpr + ")";
    }

    private String toPoCall(Plan plan, String entityExpr) {
        return plan.bridge.converterField() + "." + plan.bridge.toSourceMethod() + "(" + entityExpr + ")";
    }

    private String updateBody(Plan plan) {
        List<String> body = new ArrayList<>();
        String updateArg = plan.entityParam;
        if (plan.pkFindName != null) {
            String pkArg = plan.entityParam + "." + getter(plan.pkFieldName());
            body.add(plan.poVarType() + " old = " + mapperCall(plan, plan.pkFindName, pkArg) + ";");
        }
        if (plan.bridge.convert) {
            body.add(plan.poRef + " userPo = " + toPoCall(plan, plan.entityParam) + ";");
            updateArg = "userPo";
        }
        body.add("int rows = " + mapperCall(plan, "update", updateArg) + ";");
        body.add("if (rows > 0) {");
        if (plan.pkFindName != null) {
            body.add("    if (old != null) {");
            body.add("        self.evictCaches(" + toEntityCall(plan, "old") + ");");
            body.add("    }");
        }
        body.add("    self.evictCaches(" + plan.entityParam + ");");
        body.add("}");
        body.add("return rows;");
        return String.join("\n", body);
    }

    /** insert/update 方法体：cache 时写后清理（update 单列主键时 fetch-first 老行）；否则纯透传。 */
    private String writeBody(String name, Plan plan) {
        if (!plan.cache) {
            String arg = plan.bridge.convert ? toPoCall(plan, plan.entityParam) : plan.entityParam;
            return "return " + mapperCall(plan, name, arg) + ";";
        }
        if ("update".equals(name)) {
            return updateBody(plan);
        }
        return insertBody(plan);
    }

    /**
     * 桥接解析结果：mapper.target 与自己的 target 不一致时经 converter 转换（含一致性校验）。
     */
    private static final class Bridge {

        final String mapperField;

        final boolean convert;

        final @Nullable String converterFqn;

        final @Nullable String convertMethod;

        /** 反向转换方法名（target → mapper.target，如 toUserPo）；convert=false 时为 null。 */
        final @Nullable String toSourceMethod;

        /** mapper 返回类型（其 target 产物）FQN。 */
        final String mapperReturnFqn;

        Bridge(String mapperField, boolean convert, @Nullable String converterFqn,
                @Nullable String convertMethod, @Nullable String toSourceMethod,
                String mapperReturnFqn) {
            this.mapperField = mapperField;
            this.convert = convert;
            this.converterFqn = converterFqn;
            this.convertMethod = convertMethod;
            this.toSourceMethod = toSourceMethod;
            this.mapperReturnFqn = mapperReturnFqn;
        }

        /**
         * converter 转换方法名（convert=true 时必有值，由 resolveBridge 保证）。
         */
        String convertMethod() {
            if (convertMethod == null) {
                throw new IllegalStateException("内部不一致：convert=true 但 convertMethod 为空");
            }
            return convertMethod;
        }

        /**
         * converter 字段名（decap 简单名）。
         */
        String converterField() {
            return decapitalize(simpleName(converterFqn()));
        }

        /**
         * converter 全限定名（convert=true 时必有值，由 resolveBridge 保证）。
         */
        String converterFqn() {
            if (converterFqn == null) {
                throw new IllegalStateException("内部不一致：convert=true 但 converterFqn 为空");
            }
            return converterFqn;
        }

        /**
         * 反向转换方法名（convert=true 时必有值）。
         */
        String toSourceMethod() {
            if (toSourceMethod == null) {
                throw new IllegalStateException("内部不一致：convert=true 但 toSourceMethod 为空");
            }
            return toSourceMethod;
        }

    }

    /** 单表 × 本生成器的生成计划：开关、缓存名、po 引用、主键、规格与桥接。 */
    private static final class Plan {

        final boolean cache;

        final boolean implLombok;

        final String cacheName;

        final String entityParam;

        final String targetFqn;

        final @Nullable String poRef;

        final Bridge bridge;

        final @Nullable Column pk;

        final @Nullable String pkFieldName;

        final @Nullable String pkFindName;

        final List<QueryMethods.Spec> specs;

        Plan(boolean cache, boolean implLombok, String cacheName, String entityParam, String targetFqn,
                @Nullable String poRef, Bridge bridge, @Nullable Column pk, @Nullable String pkFieldName,
                @Nullable String pkFindName, List<QueryMethods.Spec> specs) {
            this.cache = cache;
            this.implLombok = implLombok;
            this.cacheName = cacheName;
            this.entityParam = entityParam;
            this.targetFqn = targetFqn;
            this.poRef = poRef;
            this.bridge = bridge;
            this.pk = pk;
            this.pkFieldName = pkFieldName;
            this.pkFindName = pkFindName;
            this.specs = specs;
        }

        String pkFieldName() {
            if (pkFieldName == null) {
                throw new IllegalStateException("内部不一致：pk 为 null 却取 pkFieldName");
            }
            return pkFieldName;
        }

        /** 桥接临时变量的类型（convert → poRef 简单名/FQN；直连 → target 简单名）。 */
        String poVarType() {
            return poRef == null ? simpleName(targetFqn) : poRef;
        }

    }

}
