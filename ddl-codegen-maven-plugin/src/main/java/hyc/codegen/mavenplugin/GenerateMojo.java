package hyc.codegen.mavenplugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import hyc.codegen.core.Codegen;
import hyc.codegen.core.io.ChangeReport;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * DDL 代码生成 Mojo（{@code mvn ddl-codegen:generate}）。
 * <p>
 * 参数见 docs/changes/2026-08-29-feat-maven-plugin/design.md：
 * projectRoot（默认 basedir）/ configFile（默认 projectRoot/ddl-codegen.properties）/
 * ddl（内联，与 ddlFile 互斥）/ ddlFile（支持 {@code path:start-end} 行范围，相对 projectRoot 解析）/
 * dryRun / skip。不绑定生命周期（显式调用）。
 */
@Mojo(name = "generate")
public class GenerateMojo extends AbstractMojo {

    /** 项目根，覆盖 config 推导的根。 */
    @Parameter(defaultValue = "${project.basedir}", property = "ddlCodegen.projectRoot")
    private java.io.File projectRoot;

    /** 配置文件；缺省 = projectRoot/ddl-codegen.properties。 */
    @Parameter(property = "ddlCodegen.configFile")
    private java.io.File configFile;

    /** 内联 DDL 字符串（与 ddlFile 互斥）。 */
    @Parameter(property = "ddlCodegen.ddl")
    private String ddl;

    /** DDL 文件（支持 {@code path:start-end} 行范围；相对 projectRoot 解析）。 */
    @Parameter(property = "ddlCodegen.ddlFile")
    private String ddlFile;

    /** 只报告不写盘。 */
    @Parameter(property = "ddlCodegen.dryRun", defaultValue = "false")
    private boolean dryRun;

    /** 跳过执行。 */
    @Parameter(property = "ddlCodegen.skip", defaultValue = "false")
    private boolean skip;

    /**
     * 执行生成：校验参数 → 加载配置 → 解析 DDL（内联/文件+范围）→ Codegen 门面 → 报告输出。
     *
     * @throws MojoExecutionException 参数/执行错误
     * @throws MojoFailureException   生成失败
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("ddl-codegen: skip=true，跳过");
            return;
        }

        Path root = projectRoot.toPath();
        Path config = configFile != null ? configFile.toPath() : root.resolve("ddl-codegen.properties");
        if (!Files.isRegularFile(config)) {
            throw new MojoExecutionException("配置文件不存在: " + config);
        }

        try {
            String ddlText = resolveDdl();
            ChangeReport report = Codegen.run(config, root, ddlText, dryRun);
            getLog().info((dryRun ? "[dry-run] " : "") + "变更摘要: " + report.summary());
            for (ChangeReport.Entry entry : report.getEntries()) {
                getLog().info(String.format("%-10s %s %s", entry.getStatus(), entry.getPath(), entry.getDetail()));
            }
            for (String warning : report.getWarnings()) {
                getLog().warn(warning);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("DDL 代码生成失败: " + e.getMessage(), e);
        }
    }

    /** 解析 DDL 文本：内联或文件（含行范围）；互斥校验。 */
    private String resolveDdl() throws MojoExecutionException {
        boolean hasDdl = ddl != null && !ddl.isEmpty();
        boolean hasFile = ddlFile != null && !ddlFile.isEmpty();
        if (hasDdl && hasFile) {
            throw new MojoExecutionException("ddl 与 ddlFile 互斥，只能设置其中一个");
        }
        if (hasDdl) {
            return ddl;
        }
        if (hasFile) {
            return readDdlFile();
        }
        throw new MojoExecutionException("必须设置 ddl 或 ddlFile 之一");
    }

    /** 读 DDL 文件：无范围 = 整文件；有范围 = 精确按行切片（越界钳制到文件边界 + warning）。 */
    private String readDdlFile() throws MojoExecutionException {
        DdlFileRange range = DdlFileRange.parse(ddlFile);
        Path file = Path.of(range != null ? range.getPath() : ddlFile);
        if (!file.isAbsolute()) {
            file = projectRoot.toPath().resolve(file);
        }
        if (!Files.isRegularFile(file)) {
            throw new MojoExecutionException("DDL 文件不存在: " + file);
        }
        try {
            if (range == null) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int end = range.getEndLine();
            if (end > lines.size()) {
                getLog().warn("行范围结束行超过文件行数（" + lines.size() + "），钳制到文件末尾");
                end = lines.size();
            }
            return String.join("\n", lines.subList(range.getStartLine() - 1, end));
        } catch (IOException e) {
            throw new MojoExecutionException("读取 DDL 文件失败: " + file, e);
        }
    }

}
