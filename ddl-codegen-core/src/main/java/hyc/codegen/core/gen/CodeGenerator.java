package hyc.codegen.core.gen;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.annotation.DdlAnnotationHandler;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.ddl.ApplyResult;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.io.ChangeStatus;
import hyc.codegen.core.io.FileWriter;
import hyc.codegen.core.io.PathResolver;
import hyc.codegen.core.model.Schema;
import hyc.codegen.core.model.Table;
import hyc.codegen.core.naming.NamingService;
import hyc.codegen.core.types.TypeMapper;

/**
 * 代码生成编排器：按 ApplyResult 路由到各启用的 artifact 生成器，产出变更报告。
 * <p>
 * 流程：DROP → 删除文件；RENAME → 删旧文件 + 生成新文件；CREATE/MODIFY → 逐 artifact 生成。
 * 生成顺序 = config {@code artifacts.*} 出现顺序；启用了但未注册生成器的 kind 记 warning。
 */
public final class CodeGenerator {

    private final Map<String, Generator> generators = new LinkedHashMap<>();

    public CodeGenerator(List<Generator> generators) {
        for (Generator generator : generators) {
            this.generators.put(generator.kind(), generator);
        }
    }

    /**
     * 执行生成。
     *
     * @param config         项目配置
     * @param schema         应用 DDL 后的最终模型
     * @param result         语句应用结果（受影响表/改名/删除）
     * @param customHandlers 自定义注解处理器（config {@code annotations.custom} 解析出的实例）
     * 
     * @return 变更报告
     */
    public ChangeReport generate(DdlConfig config, Schema schema, ApplyResult result,
            List<DdlAnnotationHandler> customHandlers) {
        GenerationContext gctx = buildContext(config, customHandlers);
        ChangeReport report = gctx.getReport();
        Path root = config.getRoot();

        // DROP：删除该表所有启用的 artifact 文件
        handleDrops(root, result, gctx);

        // RENAME：旧表名产物文件保留（含用户手写代码——工具永不触碰用户代码，见设计契约），
        // 新表名走正常生成；仅当类名变化（非 shard 后缀类改名）时提示手动迁移。
        handleRenames(result, gctx.getNaming(), config, gctx);

        // CREATE/MODIFY/RENAME 目标：逐 artifact 生成（跳过已删除的表）
        generateTables(schema, result, gctx);

        for (String warning : gctx.getWarnings()) {
            report.addWarning(warning);
        }
        return report;
    }

    /**
     * 构建全局上下文：共享服务（命名/类型映射）、注解注册表、生成器、变更报告。
     */
    private GenerationContext buildContext(DdlConfig config, List<DdlAnnotationHandler> customHandlers) {
        NamingService naming = new NamingService(config);
        TypeMapper typeMapper = new TypeMapper(customHandlers);
        ChangeReport report = new ChangeReport();

        AnnotationRegistry annotationRegistry = AnnotationRegistry.builtin();
        for (DdlAnnotationHandler handler : customHandlers) {
            annotationRegistry.register(handler);
        }

        GenerationContext.Builder gb = GenerationContext.builder()
                .config(config)
                .naming(naming)
                .typeMapper(typeMapper)
                .annotationRegistry(annotationRegistry)
                .report(report);
        for (Generator generator : generators.values()) {
            gb.generator(generator);
        }

        return gb.build();
    }

    /**
     * DROP：删除被删表在所有启用 artifact 路径下的产物文件。
     */
    private void handleDrops(Path root, ApplyResult result, GenerationContext gctx) {
        for (String tableName : result.getDroppedTables()) {
            deleteArtifacts(root, tableName, gctx);
        }
    }

    /**
     * 为 result 指定的受影响表逐表 × 逐 artifact 生成（受影响表含已删除表，跳过；校验表在模型中）。
     */
    private void generateTables(Schema schema, ApplyResult result, GenerationContext gctx) {
        List<String> dropped = result.getDroppedTables();
        for (String tableName : result.getAffectedTables()) {
            if (dropped.contains(tableName)) {
                continue;
            }
            Table table = schema.getTable(tableName);
            if (table == null) {
                gctx.warning("受影响表不存在于模型中: " + tableName);
                continue;
            }
            generateTable(table, gctx);
        }
    }

    /**
     * 任一 artifact 的类名因表改名而变化（true → 旧文件与新旧文件不同名，需保留提示迁移）。
     */
    private boolean classNameChanged(ApplyResult.TableRename rename, NamingService naming, DdlConfig config) {
        for (String artifactName : config.artifactNames()) {
            if (!naming.artifactClassName(rename.getFrom(), artifactName)
                    .equals(naming.artifactClassName(rename.getTo(), artifactName))) {
                return true;
            }
        }
        return false;
    }

    /**
     * RENAME 处理：保留旧表名产物文件（用户手写代码），新表名走正常生成；类名变化时提示迁移。
     */
    private void handleRenames(ApplyResult result, NamingService naming, DdlConfig config, GenerationContext gctx) {
        for (ApplyResult.TableRename rename : result.getTableRenames()) {
            if (classNameChanged(rename, naming, config)) {
                gctx.warning("表 '" + rename.getFrom() + "' 已改名为 '" + rename.getTo()
                        + "'：旧表名产物文件已保留（含手写代码），请迁移手写内容后手动删除旧文件");
            }
        }
    }

    /**
     * 对一张表执行全部启用的产物生成器（产物名 → config.generator → 注册生成器）。
     */
    private void generateTable(Table table, GenerationContext gctx) {
        for (String name : gctx.getConfig().artifactNames()) {
            ArtifactConfig artifactConfig = gctx.getConfig().artifact(name);
            if (artifactConfig == null) {
                throw new IllegalStateException("未配置产物: " + name);
            }
            String generatorName = artifactConfig.getGenerator();
            if (generatorName == null) {
                gctx.warning("产物 '" + name + "' 未配置 generator，跳过");
                continue;
            }
            Generator generator = generators.get(generatorName);
            if (generator == null) {
                gctx.warning("产物 '" + name + "' 引用了未注册的生成器: " + generatorName);
                continue;
            }
            TableContext ctx = gctx.tableContext(table, name);
            generator.generate(ctx, gctx);
        }
    }

    /**
     * 删除一张表在所有启用 artifact 路径下的文件（Java 类走 package 路径，XML 走资源路径）。
     */
    private void deleteArtifacts(Path root, String tableName, GenerationContext gctx) {
        for (String name : gctx.getConfig().artifactNames()) {
            ArtifactConfig artifactConfig = gctx.getConfig().artifact(name);
            if (artifactConfig == null) {
                throw new IllegalStateException("未配置产物: " + name);
            }
            String generator = artifactConfig.getGenerator();
            if (generator == null || !generators.containsKey(generator)) {
                continue;
            }
            TableContext ctx = gctx.tableContext(syntheticTable(tableName), name);
            Path file;
            String path = ctx.getArtifactConfig().getPath();
            if (path != null) {
                // XML 产物：文件名为其 mapper 引用的产物类名
                hyc.codegen.core.config.ArtifactConfig mapper = gctx.resolveReference(
                        name, "mapper", MapperGenerator.NAME);
                file = PathResolver.xmlFile(root, ctx.getArtifactConfig().getModule(), path,
                        gctx.getNaming().artifactClassName(tableName, mapper.getName()) + ".xml");
            } else {
                file = PathResolver.javaFile(root, ctx.getArtifactConfig().getModule(),
                        ctx.packageName(), ctx.className());
            }
            try {
                if (FileWriter.deleteIfExists(file)) {
                    gctx.getReport()
                            .add(file, ChangeStatus.DELETED,
                                    name + " " + ctx.className() + "（表 " + tableName + " 已删除/改名）");
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException("删除文件失败: " + file, e);
            }
        }
    }

    /**
     * 仅用于路径推导的表壳（删除/改名场景不需要真实列）。
     */
    private Table syntheticTable(String tableName) {
        return new Table(tableName, null);
    }

}
