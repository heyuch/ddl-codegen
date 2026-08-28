package hyc.codegen.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;

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

    private static final String ARTIFACTS_PREFIX = "artifacts.";

    @Override
    public DdlConfig load(Path configFile) {
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取配置文件失败: " + configFile, e);
        }

        DdlConfig config = new DdlConfig();
        config.setRoot(configFile.toAbsolutePath().getParent());

        loadArtifacts(props, config, configFile);
        loadNaming(props, config);
        loadCustomHandlers(props, config);
        return config;
    }

    private static void loadArtifacts(Properties props, DdlConfig config, Path configFile) {
        // kind 顺序 = 配置文件出现顺序（Properties 底层是 Hashtable，迭代无序，需按文件行序收集）
        Map<String, ArtifactConfig> byKind = new LinkedHashMap<>();
        try (BufferedReader r = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String key = keyOfLine(line);
                if (key != null && key.startsWith(ARTIFACTS_PREFIX)) {
                    int dot = key.indexOf('.', ARTIFACTS_PREFIX.length());
                    if (dot > 0) {
                        byKind.computeIfAbsent(key.substring(ARTIFACTS_PREFIX.length(), dot), ArtifactConfig::new);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("读取配置文件失败: " + configFile, e);
        }

        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(ARTIFACTS_PREFIX)) {
                continue;
            }
            int dot = key.indexOf('.', ARTIFACTS_PREFIX.length());
            if (dot < 0) {
                continue;
            }
            String kind = key.substring(ARTIFACTS_PREFIX.length(), dot);
            String prop = key.substring(dot + 1);
            ArtifactConfig artifact = byKind.computeIfAbsent(kind, ArtifactConfig::new);
            applyArtifactProperty(artifact, prop, props.getProperty(key));
        }

        for (ArtifactConfig artifact : byKind.values()) {
            if (artifact.getPkg() == null && artifact.getPath() == null) {
                throw new IllegalArgumentException("artifact '" + artifact.getKind()
                        + "' 缺少 package（Java 类 artifact 必须配置 package，XML artifact 请配置 path）: " + configFile);
            }
            config.addArtifact(artifact);
        }
    }

    /** 提取属性行中的键（支持 {@code key=value} 与 {@code key: value}；注释/空行返回 null）。 */
    @Nullable
    private static String keyOfLine(String line) {
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

    private static void applyArtifactProperty(ArtifactConfig artifact, String prop, String value) {
        switch (prop) {
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
            case "use":
                artifact.setUse(splitList(value));
                break;
            default:
                artifact.putOption(prop, value);
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

    private static boolean boolValue(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
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

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
