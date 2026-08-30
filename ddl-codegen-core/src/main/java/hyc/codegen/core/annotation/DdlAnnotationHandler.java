package hyc.codegen.core.annotation;

import java.util.Set;

import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Meta;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * DDL 注释注解处理器 SPI：把 {@code @name:value} 形式的注解解析为模型元数据。
 * <p>
 * 内置实现：{@code type}（列类型覆盖）、{@code as}（生成类命名）、{@code ignore}（跳过字段/索引方法）。
 * 用户可注册自定义注解处理器（config {@code annotations.custom}），解析结果写入节点 {@link Meta}，
 * 供生成器/拦截器消费。后续类型映射阶段（M1b）会为本接口追加 {@code resolveType} 默认钩子。
 */
public interface DdlAnnotationHandler {

    /** 注解名（config/DDL 中 {@code @name} 的名字，不含 {@code @} 与冒号）。 */
    String name();

    /** 允许出现的位置；目标不符时处理器不会被调用（记 warning 忽略）。 */
    Set<MetaTarget> targets();

    /**
     * 解析注解值并写入节点元数据。
     *
     * @param meta  目标节点（表/列/索引）的开放元数据
     * @param value 注解值；无值的注解（如 {@code @ignore}）为 {@code null}
     */
    void parse(Meta meta, @Nullable String value);

    /**
     * 类型解析钩子（默认透传）：自定义注解可在类型映射阶段改写列的默认 Java 类型。
     * <p>
     * 调用时机在 {@code @type} 注解与 enum 映射之后（DESIGN §8 解析顺序第 3 步之后），
     * 因此内置语义始终优先；本钩子只供自定义注解参与类型决策。
     *
     * @param column      目标列
     * @param defaultType 默认解析出的类型（全限定名或简单名）
     * 
     * @return 最终类型
     */
    default String resolveType(Column column, String defaultType) {
        return defaultType;
    }

}
