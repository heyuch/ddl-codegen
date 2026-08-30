package hyc.codegen.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hyc.codegen.core.annotation.AnnotationProcessor;
import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.annotation.DdlAnnotationHandler;
import hyc.codegen.core.config.ConfigLoader;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.config.PropertiesConfigLoader;
import hyc.codegen.core.ddl.ApplyResult;
import hyc.codegen.core.ddl.DdlParser;
import hyc.codegen.core.ddl.DruidDdlParser;
import hyc.codegen.core.ddl.StatementApplier;
import hyc.codegen.core.gen.CodeGenerator;
import hyc.codegen.core.gen.ConverterGenerator;
import hyc.codegen.core.gen.EnumGenerator;
import hyc.codegen.core.gen.Generator;
import hyc.codegen.core.gen.MapperGenerator;
import hyc.codegen.core.gen.MapperXmlGenerator;
import hyc.codegen.core.gen.MybatisRepositoryImplGenerator;
import hyc.codegen.core.gen.PojoGenerator;
import hyc.codegen.core.gen.RepositoryGenerator;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.io.FileWriter;
import hyc.codegen.core.model.Schema;

/**
 * 代码生成统一门面：CLI 与 Maven 插件共用的唯一管线入口（设计见
 * docs/changes/2026-08-29-feat-maven-plugin/design.md）。
 * <p>
 * 流程：加载 config（项目根以参数为准）→ 构建默认生成器/拦截器 → 解析 DDL → 应用 → 生成 → 变更报告。
 * 调用方负责读 DDL 文本（内联/文件/范围切片），本门面只收文本。
 * <p>
 * 扇出/耦合抑制依据（元素驱动，见 docs/static-rules-review.md §6）：组合根类，
 * 职责是把全部组件组装进管线，引用类型数 ≈ 组件数，无逻辑混杂可拆分。
 */
@SuppressWarnings({"ClassFanOutComplexity", "ClassDataAbstractionCoupling"})
public final class Codegen {

    private Codegen() {
        throw new AssertionError("no instances");
    }

    /**
     * 执行一次完整生成。
     *
     * @param configFile  配置文件（必填，不存在报错）
     * @param projectRoot 项目根，覆盖 config 推导的根
     * @param ddlText     DDL 文本（可含多条语句）
     * @param dryRun      只报告不写盘
     * 
     * @return 变更报告
     */
    public static ChangeReport run(Path configFile, Path projectRoot, String ddlText, boolean dryRun)
            throws Exception {
        ConfigLoader loader = new PropertiesConfigLoader();
        DdlConfig config = loader.load(configFile);
        config.setRoot(projectRoot);
        List<DdlAnnotationHandler> handlers = instantiateHandlers(config);

        AnnotationRegistry registry = AnnotationRegistry.builtin();
        for (DdlAnnotationHandler handler : handlers) {
            registry.register(handler);
        }
        DdlParser parser = new DruidDdlParser(new AnnotationProcessor(registry));
        Schema schema = new Schema();
        ApplyResult result = new StatementApplier().apply(schema, parser.parse(ddlText));

        CodeGenerator generator = new CodeGenerator(defaultGenerators());
        FileWriter.setDryRun(dryRun);
        return generator.generate(config, schema, result, handlers);
    }

    /** config {@code annotations.custom} 指定的自定义注解处理器（反射实例化）。 */
    private static List<DdlAnnotationHandler> instantiateHandlers(DdlConfig config) {
        List<DdlAnnotationHandler> handlers = new ArrayList<>();
        for (String className : config.getCustomAnnotationHandlers()) {
            try {
                Class<?> handlerClass = Class.forName(className);
                handlers.add(handlerClass.asSubclass(DdlAnnotationHandler.class)
                        .getDeclaredConstructor()
                        .newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("无法实例化注解处理器: " + className, e);
            }
        }
        return handlers;
    }

    private static List<Generator> defaultGenerators() {
        List<Generator> generators = new ArrayList<>();
        generators.add(new PojoGenerator());
        generators.add(new EnumGenerator());
        generators.add(new MapperGenerator());
        generators.add(new MapperXmlGenerator());
        generators.add(new RepositoryGenerator());
        generators.add(new MybatisRepositoryImplGenerator());
        generators.add(new ConverterGenerator());
        return generators;
    }

}
