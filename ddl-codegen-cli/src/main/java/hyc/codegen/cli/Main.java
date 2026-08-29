package hyc.codegen.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
import hyc.codegen.core.gen.ArtifactGenerator;
import hyc.codegen.core.gen.CodeGenerator;
import hyc.codegen.core.gen.ConverterGenerator;
import hyc.codegen.core.gen.EnumGenerator;
import hyc.codegen.core.gen.FieldArtifactGenerator;
import hyc.codegen.core.gen.MapperGenerator;
import hyc.codegen.core.gen.MapperXmlGenerator;
import hyc.codegen.core.gen.RepositoryGenerator;
import hyc.codegen.core.gen.RepositoryImplGenerator;
import hyc.codegen.core.interceptor.ArtifactInterceptor;
import hyc.codegen.core.interceptor.Jsr303Interceptor;
import hyc.codegen.core.interceptor.LombokInterceptor;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.io.FileWriter;
import hyc.codegen.core.model.Schema;

/**
 * 命令行入口：DDL → Java 代码。
 * <p>
 * 用法：{@code java -jar ddl-codegen-cli.jar --config 项目根/ddlgen.properties --ddl schema.sql [--dry-run]}
 * <p>
 * 扇出抑制依据（元素驱动，见 docs/static-rules-review.md §6）：组合根类，职责是把全部生成器/拦截器
 * 组装进 {@link CodeGenerator}，引用类型数 ≈ 组件数，无逻辑混杂可拆分。
 */
@SuppressWarnings({"ClassFanOutComplexity", "ClassDataAbstractionCoupling"})
public final class Main {

    private Main() {
        throw new AssertionError("no instances");
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        Path configFile = null;
        Path ddlPath = null;
        boolean dryRun = false;
        boolean sync = false;

        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            switch (arg) {
                case "--config":
                    configFile = Path.of(args[i++]);
                    break;
                case "--ddl":
                    ddlPath = Path.of(args[i++]);
                    break;
                case "--dry-run":
                    dryRun = true;
                    break;
                case "--sync":
                    sync = true;
                    break;
                case "--help":
                case "-h":
                    help();
                    return 0;
                default:
                    System.err.println("未知参数: " + arg);
                    help();
                    return 2;
            }
        }

        if (configFile == null || ddlPath == null) {
            System.err.println("缺少必需参数: --config 与 --ddl");
            help();
            return 2;
        }

        if (sync) {
            System.err.println("--sync 尚未实现（需要文件归属标记，见 docs/progress.md 已知限制）");
            return 1;
        }

        try {
            return execute(configFile, ddlPath, dryRun);
        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            return 1;
        }
    }

    private static int execute(Path configFile, Path ddlPath, boolean dryRun) throws Exception {
        ConfigLoader loader = new PropertiesConfigLoader();
        DdlConfig config = loader.load(configFile);
        List<DdlAnnotationHandler> handlers = instantiateHandlers(config);

        String ddl = readDdl(ddlPath);
        AnnotationRegistry registry = AnnotationRegistry.builtin();
        for (DdlAnnotationHandler handler : handlers) {
            registry.register(handler);
        }
        DdlParser parser = new DruidDdlParser(new AnnotationProcessor(registry));
        Schema schema = new Schema();
        ApplyResult result = new StatementApplier().apply(schema, parser.parse(ddl));

        CodeGenerator generator = new CodeGenerator(defaultGenerators(), defaultInterceptors());
        FileWriter.setDryRun(dryRun);
        ChangeReport report = generator.generate(config, schema, result, handlers);

        System.out.println((dryRun ? "[dry-run] " : "") + "变更摘要: " + report.summary());
        for (ChangeReport.Entry entry : report.getEntries()) {
            System.out.printf("%-10s %s %s%n", entry.getStatus(), entry.getPath(), entry.getDetail());
        }
        for (String warning : report.getWarnings()) {
            System.out.println("警告: " + warning);
        }
        return 0;
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

    /** 读单个 .sql 文件或目录下全部 .sql（按文件名排序拼接）。 */
    private static String readDdl(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        if (Files.isDirectory(path)) {
            List<Path> files = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(path)) {
                walk.filter(p -> p.toString().endsWith(".sql"))
                        .sorted()
                        .forEach(files::add);
            }
            StringBuilder sb = new StringBuilder();
            for (Path file : files) {
                sb.append(Files.readString(file, StandardCharsets.UTF_8)).append("\n");
            }
            return sb.toString();
        }
        throw new IllegalArgumentException("DDL 路径不存在: " + path);
    }

    private static List<ArtifactGenerator> defaultGenerators() {
        List<ArtifactGenerator> generators = new ArrayList<>();
        generators.add(new FieldArtifactGenerator("entity"));
        generators.add(new EnumGenerator());
        generators.add(new FieldArtifactGenerator("pojo"));
        generators.add(new MapperGenerator());
        generators.add(new MapperXmlGenerator());
        generators.add(new RepositoryGenerator());
        generators.add(new RepositoryImplGenerator());
        generators.add(new ConverterGenerator());
        return generators;
    }

    private static List<ArtifactInterceptor> defaultInterceptors() {
        List<ArtifactInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LombokInterceptor());
        interceptors.add(new Jsr303Interceptor());
        return interceptors;
    }

    private static void help() {
        System.out.println(
                "用法: java -jar ddl-codegen-cli.jar --config <ddlgen.properties> --ddl <schema.sql|目录> [--dry-run]");
        System.out.println("  --config   项目根目录下的 ddlgen.properties（项目根 = 该文件所在目录）");
        System.out.println("  --ddl      DDL 文件或目录（目录会按文件名排序拼接全部 .sql）");
        System.out.println("  --dry-run  只报告变更，不写盘");
    }

}
