package hyc.codegen.core.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 项目配置模型，对应项目根目录下的 properties 配置文件。
 * <p>
 * {@code artifacts.*} 段即生成器启停开关：配置了哪些 artifact 就启用哪些；module 为项目根下的一级子目录。
 * 命名策略各键语义见 {@link hyc.codegen.core.naming.NamingService}。
 */
public final class DdlConfig {

    /** 项目根 = 配置文件所在目录。 */
    private Path root;

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

    /** {@code @Nullable} 注解全限定名（默认 javax.annotation.Nullable，config {@code annotations.nullable} 可配）。 */
    private String nullableAnnotation = "javax.annotation.Nullable";

    public String getNullableAnnotation() {
        return nullableAnnotation;
    }

    public void setNullableAnnotation(String nullableAnnotation) {
        this.nullableAnnotation = nullableAnnotation;
    }

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public void addArtifact(ArtifactConfig artifact) {
        artifacts.put(artifact.getName(), artifact);
    }

    /** 按产物名取配置。 */
    public Optional<ArtifactConfig> artifact(String name) {
        return Optional.ofNullable(artifacts.get(name));
    }

    /** 启用的生成器集合（配置出现顺序）。 */
    public List<String> artifactNames() {
        return new ArrayList<>(artifacts.keySet());
    }

    /** 全部 artifact 配置（有序）。 */
    public Map<String, ArtifactConfig> getArtifacts() {
        return new LinkedHashMap<>(artifacts);
    }

    public List<String> getTableStripPrefixes() {
        return new ArrayList<>(tableStripPrefixes);
    }

    public void addTableStripPrefix(String prefix) {
        tableStripPrefixes.add(prefix);
    }

    public boolean isTableStripShardSuffix() {
        return tableStripShardSuffix;
    }

    public void setTableStripShardSuffix(boolean tableStripShardSuffix) {
        this.tableStripShardSuffix = tableStripShardSuffix;
    }

    public String getTableShardPattern() {
        return tableShardPattern;
    }

    public void setTableShardPattern(String tableShardPattern) {
        this.tableShardPattern = tableShardPattern;
    }

    public boolean isColumnCamelCase() {
        return columnCamelCase;
    }

    public void setColumnCamelCase(boolean columnCamelCase) {
        this.columnCamelCase = columnCamelCase;
    }

    public String getColumnKeywordSuffix() {
        return columnKeywordSuffix;
    }

    public void setColumnKeywordSuffix(String columnKeywordSuffix) {
        this.columnKeywordSuffix = columnKeywordSuffix;
    }

    public String getMethodPrefix() {
        return methodPrefix;
    }

    public void setMethodPrefix(String methodPrefix) {
        this.methodPrefix = methodPrefix;
    }

    /** enum 类命名风格：column（Gender）/ tableColumn（UserGender）。 */
    public String getEnumStyle() {
        return enumStyle;
    }

    public void setEnumStyle(String enumStyle) {
        this.enumStyle = enumStyle;
    }

    public List<String> getCustomAnnotationHandlers() {
        return new ArrayList<>(customAnnotationHandlers);
    }

    public void addCustomAnnotationHandler(String className) {
        customAnnotationHandlers.add(className);
    }

}
