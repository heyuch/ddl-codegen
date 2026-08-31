package hyc.codegen.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 模型节点的开放元数据容器。
 * <p>
 * DDL 注解（{@code @type}/{@code @as}/{@code @ignore} 及自定义注解）的解析结果写入这里，
 * 任何生成器/拦截器都可读写，框架不限制键值语义。
 */
public final class Meta {

    private final Map<String, Object> values = new LinkedHashMap<>();

    /** 返回底层键值副本，便于遍历。 */
    public Map<String, Object> asMap() {
        return new LinkedHashMap<>(values);
    }

    /** 是否包含指定键。 */
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    /** 读取原始值；不存在时返回 {@code null}。 */
    public @Nullable Object get(String key) {
        return values.get(key);
    }

    /** 读取字符串值；不存在或非字符串时返回 {@code null}。 */
    public @Nullable String getString(String key) {
        Object value = values.get(key);
        return value instanceof String ? (String)value : null;
    }

    /** 是否为布尔真（{@code @ignore} 这类开关标记用）。 */
    public boolean isTrue(String key) {
        return Boolean.TRUE.equals(values.get(key));
    }

    /**
     * 写入一个键值。
     *
     * @param key   键
     * @param value 值，{@code null} 表示清除
     */
    public void put(String key, @Nullable Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    /** 将全部键值并入当前容器（复制语义，不改动来源）。 */
    public void putAll(Meta source) {
        values.putAll(source.values);
    }

    @Override
    public String toString() {
        return values.toString();
    }

}
