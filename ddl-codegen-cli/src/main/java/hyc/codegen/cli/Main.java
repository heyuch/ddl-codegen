package hyc.codegen.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import hyc.codegen.core.Codegen;
import hyc.codegen.core.io.ChangeReport;

/**
 * 命令行入口：DDL → Java 代码（薄壳，管线见 {@link Codegen}）。
 * <p>
 * 用法：{@code java -jar ddl-codegen-cli.jar --config 项目根/ddl-codegen.properties --ddl schema.sql [--dry-run]}
 * {@code --config} 缺省为 {@code cwd/ddl-codegen.properties}。
 * <p>
 * 扇出抑制依据（元素驱动，见 docs/static-rules-review.md §6）：薄壳类，只做参数解析与报告输出。
 */
@SuppressWarnings("ClassFanOutComplexity")
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

        if (configFile == null) {
            configFile = Path.of("ddl-codegen.properties");
        }
        if (ddlPath == null) {
            System.err.println("缺少必需参数: --ddl");
            help();
            return 2;
        }

        try {
            return execute(configFile, ddlPath, dryRun);
        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            return 1;
        }
    }

    private static int execute(Path configFile, Path ddlPath, boolean dryRun) throws Exception {
        String ddl = readDdl(ddlPath);
        Path projectRoot = configFile.toAbsolutePath().getParent();
        ChangeReport report = Codegen.run(configFile, projectRoot, ddl, dryRun);

        System.out.println((dryRun ? "[dry-run] " : "") + "变更摘要: " + report.summary());
        for (ChangeReport.Entry entry : report.getEntries()) {
            System.out.printf("%-10s %s %s%n", entry.getStatus(), entry.getPath(), entry.getDetail());
        }
        for (String warning : report.getWarnings()) {
            System.out.println("警告: " + warning);
        }
        return 0;
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

    private static void help() {
        System.out.println(
                "用法: java -jar ddl-codegen-cli.jar --ddl <schema.sql|目录>"
                        + " [--config <ddl-codegen.properties>] [--dry-run]");
        System.out.println("  --config   配置文件（缺省: cwd/ddl-codegen.properties；项目根 = 该文件所在目录）");
        System.out.println("  --ddl      DDL 文件或目录（目录会按文件名排序拼接全部 .sql）");
        System.out.println("  --dry-run  只报告变更，不写盘");
    }

}
