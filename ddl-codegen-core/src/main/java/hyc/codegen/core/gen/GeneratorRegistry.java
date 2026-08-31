package hyc.codegen.core.gen;

import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 两阶段的 artifact 描述注册表：生成器可发布/查询跨 artifact 的元数据。
 * <p>
 * 内置生成器不依赖它（类名/包名均由命名推导），它只作用户扩展互相传信息的兜底
 * （DESIGN §10：95% 由命名推导覆盖，registry 不做依赖图）。
 */
public final class GeneratorRegistry {

    /**
     * kind#table → 描述对象。
     */
    private final Map<String, Object> entries = new LinkedHashMap<>();

    private static String key(String kind, String table) {
        return kind + "#" + table;
    }

    /**
     * 按类型查询描述。
     */
    public @Nullable <T> T lookup(String kind, String table, Class<T> type) {
        Object info = entries.get(key(kind, table));
        return type.isInstance(info) ? type.cast(info) : null;
    }

    /**
     * 发布一条描述。
     *
     * @param kind  artifact 类型名
     * @param table 表名
     * @param info  描述对象（自定义类型，发布方与消费方约定）
     */
    public void publish(String kind, String table, Object info) {
        entries.put(key(kind, table), info);
    }

}
