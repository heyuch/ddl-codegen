package hyc.codegen.core.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 项目配置模型，对应项目根目录下的 properties 配置文件。
 * <p>
 * {@code artifacts.*} 段即生成器启停开关：配置了哪些 artifact 就启用哪些；module 为项目根下的一级子目录。
 * 命名策略各键语义见 {@link hyc.codegen.core.naming.NamingService}。
 */
@Getter
@Setter
public final class DdlConfig {

    /** 项目根 = 配置文件所在目录（加载前为 null；经 getRoot 的 requireNonNull 强制非空）。 */
    private @Nullable Path root;

    /** artifact 按配置出现顺序保存。 */
    private final Map<String, ArtifactConfig> artifacts = new LinkedHashMap<>();

    // ---- 命名策略（naming.*）----
    private final List<String> tableStripPrefixes = new ArrayList<>();
    private boolean tableStripShardSuffix;
    private String tableShardPattern = "_\\d+$";
    private boolean columnCamelCase = true;
    private String columnKeywordSuffix = "_";
    private String methodPrefix = "find";
    private String enumStyle = "column";

    // ---- 自定义注解处理器（annotations.custom）----
    private final List<String> customAnnotationHandlers = new ArrayList<>();

    /** {@code @Nullable} 注解全限定名（默认 checkerframework 的 Nullable，config {@code annotations.nullable} 可配）。 */
    private String nullableAnnotation = "org.checkerframework.checker.nullness.qual.Nullable";

    public void addArtifact(ArtifactConfig artifact) {
        artifacts.put(artifact.getName(), artifact);
    }

    public void addCustomAnnotationHandler(String className) {
        customAnnotationHandlers.add(className);
    }

    public void addTableStripPrefix(String prefix) {
        tableStripPrefixes.add(prefix);
    }

    /** 按产物名取配置；未配置时返回 {@code null}。 */
    @Nullable
    public ArtifactConfig artifact(String name) {
        return artifacts.get(name);
    }

    /** 启用的生成器集合（配置出现顺序）。 */
    public List<String> artifactNames() {
        return new ArrayList<>(artifacts.keySet());
    }

    /** 全部 artifact 配置（有序）。 */
    public Map<String, ArtifactConfig> getArtifacts() {
        return new LinkedHashMap<>(artifacts);
    }

    public List<String> getCustomAnnotationHandlers() {
        return new ArrayList<>(customAnnotationHandlers);
    }

    public Path getRoot() {
        if (root == null) {
            throw new IllegalStateException("项目根未设置（需先经 PropertiesConfigLoader 加载或 setRoot）");
        }
        return root;
    }

    public List<String> getTableStripPrefixes() {
        return new ArrayList<>(tableStripPrefixes);
    }

}
