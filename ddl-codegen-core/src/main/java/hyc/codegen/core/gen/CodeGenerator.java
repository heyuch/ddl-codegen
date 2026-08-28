package hyc.codegen.core.gen;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.annotation.DdlAnnotationHandler;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.ddl.ApplyResult;
import hyc.codegen.core.interceptor.ArtifactInterceptor;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.io.ChangeStatus;
import hyc.codegen.core.io.FileWriter;
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

    private final Map<String, ArtifactGenerator> generators = new LinkedHashMap<>();

    private final Map<String, ArtifactInterceptor> interceptors = new LinkedHashMap<>();

    public CodeGenerator(List<ArtifactGenerator> generators, List<ArtifactInterceptor> interceptors) {
        for (ArtifactGenerator generator : generators) {
            this.generators.put(generator.kind(), generator);
        }
        for (ArtifactInterceptor interceptor : interceptors) {
            this.interceptors.put(interceptor.name(), interceptor);
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
        NamingService naming = new NamingService(config);
        TypeMapper typeMapper = new TypeMapper(naming, customHandlers);
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
        for (ArtifactInterceptor interceptor : interceptors.values()) {
            gb.interceptor(interceptor);
        }
        GenerationContext gctx = gb.build();

        Path root = config.getRoot();

        // DROP：删除该表所有启用的 artifact 文件
        for (String tableName : result.getDroppedTables()) {
            deleteArtifacts(root, tableName, gctx);
        }

        // RENAME：删旧文件，新名走正常生成
        for (ApplyResult.TableRename rename : result.getTableRenames()) {
            deleteArtifacts(root, rename.getFrom(), gctx);
        }

        // CREATE/MODIFY/RENAME 目标：逐 artifact 生成
        for (String tableName : result.getAffectedTables()) {
            Table table = schema.getTable(tableName);
            if (table == null) {
                gctx.warning("受影响表不存在于模型中: " + tableName);
                continue;
            }
            generateTable(table, gctx);
        }

        return report;
    }

    /** 对一张表执行全部启用的 artifact 生成器。 */
    private void generateTable(Table table, GenerationContext gctx) {
        for (String kind : gctx.getConfig().artifactKinds()) {
            ArtifactGenerator generator = generators.get(kind);
            if (generator == null) {
                gctx.warning("启用了 artifact '" + kind + "' 但没有注册生成器");
                continue;
            }
            TableContext ctx = gctx.tableContext(table, kind);
            if (generator instanceof AbstractJavaArtifactGenerator) {
                ((AbstractJavaArtifactGenerator)generator).generate(ctx, gctx);
            } else {
                gctx.warning("生成器未继承 AbstractJavaArtifactGenerator，暂不支持: " + kind);
            }
        }
    }

    /** 删除一张表在所有启用 artifact 路径下的文件。 */
    private void deleteArtifacts(Path root, String tableName, GenerationContext gctx) {
        for (String kind : gctx.getConfig().artifactKinds()) {
            ArtifactGenerator generator = generators.get(kind);
            if (generator == null) {
                continue;
            }
            TableContext ctx = gctx.tableContext(syntheticTable(tableName), kind);
            try {
                if (FileWriter.deleteIfExists(ctx.javaFile(root))) {
                    gctx.getReport()
                            .add(ctx.javaFile(root), ChangeStatus.DELETED,
                                    kind + " " + ctx.className() + "（表 " + tableName + " 已删除/改名）");
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException("删除文件失败: " + ctx.javaFile(root), e);
            }
        }
    }

    /** 仅用于路径推导的表壳（删除/改名场景不需要真实列）。 */
    private Table syntheticTable(String tableName) {
        return new Table(tableName, null);
    }

}
