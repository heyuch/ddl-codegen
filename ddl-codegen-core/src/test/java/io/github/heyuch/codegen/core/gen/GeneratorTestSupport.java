package io.github.heyuch.codegen.core.gen;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.heyuch.codegen.core.config.ArtifactConfig;
import io.github.heyuch.codegen.core.config.DdlConfig;
import io.github.heyuch.codegen.core.ddl.ApplyResult;
import io.github.heyuch.codegen.core.ddl.DruidDdlParser;
import io.github.heyuch.codegen.core.ddl.StatementApplier;
import io.github.heyuch.codegen.core.io.ChangeReport;
import io.github.heyuch.codegen.core.model.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 生成器 golden 契约测试 harness（测试 only）：mini config + 指定 kinds 的 CodeGenerator + DDL 应用，
 * 把实际生成产物与 {@code src/test/resources/fixtures/gen/<case>/expected/**} 整文件比对。
 * <p>
 * fixture 固化流程：产物模板变更 = 契约变更 → 显式以 {@code -Dgolden.update=true} 重写 fixture 后
 * 审阅 diff 提交（测试不提供失败自动更新，防掩盖）。
 */
public final class GeneratorTestSupport {

    private static final String FIXTURES_BASE = "fixtures/gen/";

    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    private final Path root;

    private final List<Generator> generators;

    public GeneratorTestSupport(@org.checkerframework.checker.nullness.qual.Nullable Path root,
            Generator... generators) {
        if (root == null) {
            throw new AssertionError("@TempDir 未注入");
        }
        this.root = root;
        this.generators = Arrays.asList(generators);
    }

    private static String line(List<String> lines, int index) {
        if (index >= lines.size()) {
            return "<EOF>";
        }
        String text = lines.get(index);
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }

    private static List<Path> listFiles(Path base) throws IOException {
        try (Stream<Path> stream = Files.walk(base)) {
            return stream.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }
    }

    private static List<String> relativize(Path base, List<Path> files) {
        List<String> rel = new ArrayList<>();
        for (Path file : files) {
            rel.add(base.relativize(file).toString().replace('\\', '/'));
        }
        Collections.sort(rel);
        return rel;
    }

    /** 新增产物配置（pkg 或 suffix 可空，与 Java/XML 产物语义一致）。 */
    public ArtifactConfig addArtifact(DdlConfig config, String name, String generator, @Nullable String pkg,
            @Nullable String suffix) {
        ArtifactConfig artifact = new ArtifactConfig(name);
        artifact.setGenerator(generator);
        artifact.setModule("");
        artifact.setPkg(pkg);
        artifact.setSuffix(suffix);
        config.addArtifact(artifact);
        return artifact;
    }

    /** 按产物名取配置（测试约定产物已配置）。 */
    public ArtifactConfig artifact(DdlConfig config, String name) {
        ArtifactConfig artifactConfig = config.artifact(name);
        if (artifactConfig == null) {
            throw new AssertionError("产物未配置: " + name);
        }
        return artifactConfig;
    }

    private void assertEach(String caseName, Path expectedBase, List<String> rels) throws IOException {
        for (String rel : rels) {
            String expectedText = new String(Files.readAllBytes(expectedBase.resolve(rel)), StandardCharsets.UTF_8);
            String actualText = new String(Files.readAllBytes(root.resolve(rel)), StandardCharsets.UTF_8);
            if (expectedText.equals(actualText)) {
                continue;
            }
            List<String> expLines = Arrays.asList(expectedText.split("\n", -1));
            List<String> actLines = Arrays.asList(actualText.split("\n", -1));
            int first = 0;
            while (first < expLines.size() && first < actLines.size()
                    && expLines.get(first).equals(actLines.get(first))) {
                first++;
            }
            fail("golden 不一致 case=" + caseName + " file=" + rel + "\n  首个差异行 " + (first + 1)
                    + "\n  expected: " + line(expLines, first) + "\n  actual  : " + line(actLines, first)
                    + "\n  （fixture 属产物契约，变更需 review；必要时 -Dgolden.update=true 重写后审阅 diff）");
        }
    }

    /**
     * golden 断言（严格）：期望目录 = {@code fixtures/gen/<case>/expected}，实际基目录 = 生成 root。
     * 文件集对称（无多无缺）+ 逐文件整文本一致（不等时报首处差异行）。authoring 时先写后返回。
     */
    public void assertGolden(String caseName) throws Exception {
        Path expectedBase = fixtureDir(caseName);
        if (UPDATE) {
            writeAll(expectedBase, relativize(root, listFiles(root)));
            return;
        }
        List<String> expectedRel = relativize(expectedBase, listFiles(expectedBase));
        List<String> actualRel = relativize(root, listFiles(root));
        assertEquals(expectedRel, actualRel, "生成文件集与 fixture 不对称（case=" + caseName + "）");
        assertEach(caseName, expectedBase, expectedRel);
    }

    /**
     * golden 断言（子集）：只比对期望文件（每个须存在且整文本一致），忽略兄弟产物。
     * authoring 时按清单写期望文件。
     */
    public void assertGoldenSubset(String caseName, String... rels) throws Exception {
        Path expectedBase = fixtureDir(caseName);
        if (UPDATE) {
            writeRels(expectedBase, Arrays.asList(rels));
            return;
        }
        assertEach(caseName, expectedBase, relativize(expectedBase, listFiles(expectedBase)));
    }

    private Path fixtureDir(String caseName) {
        Path source = Paths.get("src/test/resources", FIXTURES_BASE + caseName + "/expected");
        if (Files.isDirectory(source) || UPDATE) {
            return source;
        }
        ClassLoader loader = getClass().getClassLoader();
        if (loader != null) {
            java.net.URL url = loader.getResource(FIXTURES_BASE + caseName + "/expected");
            if (url != null) {
                try {
                    URI uri = url.toURI();
                    return Paths.get(uri);
                } catch (URISyntaxException e) {
                    return source;
                }
            }
        }
        return source;
    }

    /** 复用 Schema 生成（create/alter 同流）。 */
    public ChangeReport generate(DdlConfig config, Schema schema, String ddl) {
        ApplyResult result = new StatementApplier().apply(schema, new DruidDdlParser().parse(ddl));
        CodeGenerator generator = new CodeGenerator(generators);
        return generator.generate(config, schema, result, Collections.emptyList());
    }

    /** 一次性生成（内部新建 Schema）。 */
    public ChangeReport generate(DdlConfig config, String ddl) {
        return generate(config, new Schema(), ddl);
    }

    /**
     * 运行 case 的 DDL input（{@code fixtures/gen/<case>/input/*.sql}，按文件名序、共享 Schema）后做
     * 严格 golden 比对。无 input 时等价 {@link #assertGolden}。
     */
    public void generateAndAssert(DdlConfig config, String caseName) throws Exception {
        runInputs(config, caseName);
        assertGolden(caseName);
    }

    /** 同 {@link #generateAndAssert}，但用子集断言（仅比对清单内期望文件）。 */
    public void generateAndAssertSubset(DdlConfig config, String caseName, String... rels) throws Exception {
        runInputs(config, caseName);
        assertGoldenSubset(caseName, rels);
    }

    private Path inputDir(String caseName) {
        Path source = Paths.get("src/test/resources", FIXTURES_BASE + caseName + "/input");
        if (Files.isDirectory(source) || UPDATE) {
            return source;
        }
        ClassLoader loader = getClass().getClassLoader();
        if (loader != null) {
            java.net.URL url = loader.getResource(FIXTURES_BASE + caseName + "/input");
            if (url != null) {
                try {
                    return Paths.get(url.toURI());
                } catch (URISyntaxException e) {
                    return source;
                }
            }
        }
        return source;
    }

    /** 新配置（root 已设，module 为空，产物按需 add）。 */
    public DdlConfig newConfig() {
        DdlConfig config = new DdlConfig();
        config.setRoot(root);
        return config;
    }

    /** 读取生成产物文本（供测试按需附加断言，主断言走 assertGolden*）。 */
    public String readGenerated(String relative) throws IOException {
        Path path = root.resolve(relative);
        assertTrue(Files.isRegularFile(path), "文件不存在: " + relative);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /** 按文件名序执行 case 的 DDL input 文件（create/alter 同 Schema）。 */
    private void runInputs(DdlConfig config, String caseName) throws Exception {
        Path inputBase = inputDir(caseName);
        if (!Files.isDirectory(inputBase)) {
            return;
        }
        Schema schema = new Schema();
        for (Path file : listFiles(inputBase)) {
            String ddl = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            generate(config, schema, ddl);
        }
    }

    /** 带常见表命名策略的配置（t_ 前缀剥除 + 分表后缀剥除）。 */
    public DdlConfig tableConfig() {
        DdlConfig config = newConfig();
        config.addTableStripPrefix("t_");
        config.setTableStripShardSuffix(true);
        return config;
    }

    /** authoring（-Dgolden.update=true）：按实际文件集写入 fixture 并清理过期期望。 */
    private void writeAll(Path expectedBase, List<String> rels) throws IOException {
        writeRels(expectedBase, rels);
        for (Path file : listFiles(expectedBase)) {
            String rel = expectedBase.relativize(file).toString().replace('\\', '/');
            if (!rels.contains(rel)) {
                Files.delete(file);
            }
        }
    }

    /** authoring：把清单内实际产物写入 fixture 期望目录。 */
    private void writeRels(Path expectedBase, List<String> rels) throws IOException {
        Files.createDirectories(expectedBase);
        for (String rel : rels) {
            Path target = expectedBase.resolve(rel);
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(root.resolve(rel), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
