package hyc.codegen.core.annotation;

import hyc.codegen.core.model.Meta;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 注解分发器：注释文本 → 按注册表分发给处理器。
 * <p>
 * 容忍原则（边界契约）：未知注解名、目标位置不符 → 记 warning 日志并忽略，不中断生成。
 */
public final class AnnotationProcessor {

    private static final System.Logger LOG = System.getLogger(AnnotationProcessor.class.getName());

    private final AnnotationRegistry registry;
    private final AnnotationParser parser = new AnnotationParser();

    /**
     * 创建注解分发器（内置或自定义注册表）。
     *
     * @param registry 处理器注册表（通常为 {@link AnnotationRegistry#builtin()}
     *                 并追加自定义处理器）
     */
    public AnnotationProcessor(AnnotationRegistry registry) {
        this.registry = registry;
    }

    /**
     * 解析注释并应用注解到节点元数据。
     *
     * @param comment 注释原文；为空时直接返回
     * @param target  注解出现位置（表/列/索引）
     * @param meta    目标节点元数据，注解结果写入这里
     */
    public void process(@Nullable String comment, MetaTarget target, Meta meta) {
        if (comment == null || comment.isEmpty()) {
            return;
        }

        for (AnnotationParser.Occurrence occurrence : parser.parse(comment)) {
            DdlAnnotationHandler handler = registry.get(occurrence.name());
            if (handler == null) {
                LOG.log(System.Logger.Level.WARNING,
                        "未知 DDL 注解 {0}（位置 {1}），已忽略", occurrence, target);
                continue;
            }
            if (!handler.targets().contains(target)) {
                LOG.log(System.Logger.Level.WARNING,
                        "注解 {0} 不适用于位置 {1}，已忽略", occurrence, target);
                continue;
            }
            handler.parse(meta, occurrence.value());
        }
    }

}
