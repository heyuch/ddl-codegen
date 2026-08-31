package hyc.codegen.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 基于 JDK {@link Properties} 的配置加载器（零依赖）。
 * <p>
 * 键格式：
 * <ul>
 * <li>{@code artifacts.<kind>.module|package|suffix|path|use}——use 逗号分隔，其余任意键进 options</li>
 * <li>{@code naming.table.stripPrefixes}（逗号分隔）、{@code naming.table.stripShardSuffix}、
 * {@code naming.table.shardPattern}、{@code naming.column.camelCase}、{@code naming.column.keywordSuffix}、
 * {@code naming.method.prefix}、{@code naming.enum.style}</li>
 * <li>{@code annotations.custom}（逗号分隔的处理器类名）</li>
 * </ul>
 */
public final class PropertiesConfigLoader implements ConfigLoader {

    /** 保留命名空间：naming.* 与 annotations.*；其余顶层键第一段 = 产物名。 */
    private static final List<String> RESERVED = Arrays.asList("naming", "annotations");

    // artifact 属性 switch 分发：分支数 ≈ 属性键数（每键一 case，元素驱动）
    @SuppressWarnings({"CyclomaticComplexity", "NPathComplexity"})
    private static void applyArtifactProperty(ArtifactConfig artifact, String prop, String value) {
        switch (prop) {
            case "generator":
                artifact.setGenerator(emptyToNull(value));
                break;
            case "source":
                artifact.setSource(emptyToNull(value));
                break;
            case "target":
                artifact.setTarget(emptyToNull(value));
                break;
            case "module":
                artifact.setModule(emptyToNull(value));
                break;
            case "package":
                artifact.setPkg(emptyToNull(value));
                break;
            case "suffix":
                artifact.setSuffix(value);
                break;
            case "path":
                artifact.setPath(emptyToNull(value));
                break;
            default:
                artifact.putOption(prop, value);
        }
    }

    /** 顶层键的产物名：非保留命名空间且有属性的键返回第一段，否则 null。 */
    private static @Nullable String artifactNameOf(String key) {
        int dot = key.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        String first = key.substring(0, dot);
        if (RESERVED.contains(first)) {
            return null;
        }
        return first;
    }

    private static boolean boolValue(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    /** 空/空白字符串 → null（属性值可缺省语义）；已为 null 原样返回。 */
    private static @Nullable String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 提取属性行中的键（支持 {@code key=value} 与 {@code key: value}；注释/空行返回 null）。 */
    private static @Nullable String keyOfLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return null;
        }
        int eq = trimmed.indexOf('=');
        if (eq < 0) {
            eq = trimmed.indexOf(':');
        }
        String key = eq < 0 ? trimmed : trimmed.substring(0, eq);
        return key.trim();
    }

    // 属性收集+校验编排：分支 = 属性类型判定与缺省校验（元素驱动）
    @SuppressWarnings({"CyclomaticComplexity", "NPathComplexity"})
    private static void loadArtifacts(Properties props, DdlConfig config, Path configFile) {
        // 产物顺序 = 配置文件出现顺序（Properties 底层是 Hashtable，迭代无序，需按文件行序收集）
        Map<String, ArtifactConfig> byName = new LinkedHashMap<>();
        try (BufferedReader r = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String key = keyOfLine(line);
                if (key != null) {
                    String name = artifactNameOf(key);
                    if (name != null) {
                        byName.computeIfAbsent(name, ArtifactConfig::new);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("读取配置文件失败: " + configFile, e);
        }

        for (String key : props.stringPropertyNames()) {
            String name = artifactNameOf(key);
            if (name == null) {
                continue;
            }
            int dot = key.indexOf('.');
            String prop = key.substring(dot + 1);
            ArtifactConfig artifact = byName.computeIfAbsent(name, ArtifactConfig::new);
            String value = props.getProperty(key);
            if (value != null) {
                applyArtifactProperty(artifact, prop, value);
            }
        }

        for (ArtifactConfig artifact : byName.values()) {
            if (artifact.getPkg() == null && artifact.getPath() == null) {
                throw new IllegalArgumentException("产物 '" + artifact.getName()
                        + "' 缺少 package（Java 类产物必须配置 package，XML 产物请配置 path）: " + configFile);
            }
            config.addArtifact(artifact);
        }
    }

    private static void loadCustomHandlers(Properties props, DdlConfig config) {
        String handlers = props.getProperty("annotations.custom");
        if (handlers != null) {
            for (String handler : splitList(handlers)) {
                config.addCustomAnnotationHandler(handler);
            }
        }
        String nullable = props.getProperty("annotations.nullable");
        if (nullable != null && !nullable.trim().isEmpty()) {
            config.setNullableAnnotation(nullable.trim());
        }
    }

    private static void loadNaming(Properties props, DdlConfig config) {
        String stripPrefixes = props.getProperty("naming.table.stripPrefixes");
        if (stripPrefixes != null) {
            for (String prefix : splitList(stripPrefixes)) {
                config.addTableStripPrefix(prefix);
            }
        }
        config.setTableStripShardSuffix(
                boolValue(props, "naming.table.stripShardSuffix", config.isTableStripShardSuffix()));
        String shardPattern = props.getProperty("naming.table.shardPattern");
        if (shardPattern != null) {
            config.setTableShardPattern(shardPattern);
        }
        config.setColumnCamelCase(boolValue(props, "naming.column.camelCase", config.isColumnCamelCase()));
        String keywordSuffix = props.getProperty("naming.column.keywordSuffix");
        if (keywordSuffix != null) {
            config.setColumnKeywordSuffix(keywordSuffix);
        }
        String methodPrefix = props.getProperty("naming.method.prefix");
        if (methodPrefix != null) {
            config.setMethodPrefix(methodPrefix);
        }
        String enumStyle = props.getProperty("naming.enum.style");
        if (enumStyle != null) {
            config.setEnumStyle(enumStyle);
        }
    }

    /** 逗号分隔 → 去空白去空项的列表。 */
    private static java.util.List<String> splitList(String value) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    @Override
    public DdlConfig load(Path configFile) {
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取配置文件失败: " + configFile, e);
        }

        DdlConfig config = new DdlConfig();
        Path root = configFile.toAbsolutePath().getParent();
        if (root == null) {
            throw new IllegalArgumentException("配置文件必须位于目录中（无法确定项目根）: " + configFile);
        }
        config.setRoot(root);

        loadArtifacts(props, config, configFile);
        loadNaming(props, config);
        loadCustomHandlers(props, config);
        return config;
    }

}
