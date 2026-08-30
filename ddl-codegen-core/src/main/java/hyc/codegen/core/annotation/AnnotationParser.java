package hyc.codegen.core.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * 严格语法 {@code @name[:value]} 的注解提取器。
 * <p>
 * 规则：{@code @} 后跟标识符为注解名，冒号后可跟非空值（值不含空白与 {@code @}）；
 * 无隐式简写（{@code @boolean} 不是 {@code @type:boolean}）。一个注释可含多个注解。
 * 注意：注释里的邮箱（{@code foo@bar.com}）会被当作未知注解提取，由调用方记 warning 忽略。
 */
public final class AnnotationParser {

    private static final Pattern ANNOTATION = Pattern.compile("@([A-Za-z_][A-Za-z0-9_]*)(?::([^@\\s]*))?");

    /**
     * 从注释文本中提取注解。
     *
     * @param comment 注释原文（可能为 {@code null}）
     * 
     * @return 按出现顺序的注解列表
     */
    public List<Occurrence> parse(@Nullable String comment) {
        List<Occurrence> occurrences = new ArrayList<>();
        if (comment == null || comment.isEmpty()) {
            return occurrences;
        }

        Matcher matcher = ANNOTATION.matcher(comment);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name == null) {
                // 正则保证必有（@name），防御：缺名字的匹配跳过
                continue;
            }
            String value = matcher.group(2);
            occurrences.add(new Occurrence(name, value));
        }
        return occurrences;
    }

    /** 一次注解出现：名字 + 值（无值时为 {@code null}）。 */
    public static final class Occurrence {

        private final String name;
        @Nullable
        private final String value;

        Occurrence(String name, @Nullable String value) {
            this.name = name;
            this.value = value;
        }

        public String name() {
            return name;
        }

        @Nullable
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value == null ? "@" + name : "@" + name + ":" + value;
        }

    }

}
