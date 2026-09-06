package hyc.codegen.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 列 comment 枚举列表解析器（供 {@code @enum} 标注列 / SQL-enum 列补全 desc/name 使用）。
 * <p>
 * Grammar：comment 按空白切 token；匹配 {@code {code}={desc}} 或 {@code {code}={desc}({name})}——
 * code/desc 不含空白与 ASCII 括号；{@code (name)} 必须整 token 后缀且 name 不含空白/括号。
 * 不含 {@code =} 的 token（如正文前缀）自然跳过；含 {@code =} 但结构不符 → warning 跳过该项
 * （正文可能恰含 {@code =}，容忍原则与注解解析一致）。
 */
public final class EnumCommentParser {

    /** 枚举项 token 形态：{@code code=desc} 或 {@code code=desc(name)}（name 组可选）。 */
    private static final Pattern ITEM = Pattern.compile("^([^=()\\s]+)=([^()\\s]*)(?:\\(([^()\\s]+)\\))?$");

    private static final System.Logger LOG = System.getLogger(EnumCommentParser.class.getName());

    private EnumCommentParser() {
        throw new AssertionError("no instances");
    }

    /**
     * 解析 comment 中的枚举列表。
     *
     * @param comment 列注释原文（可能为 {@code null}）
     * 
     * @return 按出现顺序的枚举项列表（结构不符的 {@code =} token 已跳过并记 warning）
     */
    public static List<EnumItem> parse(@Nullable String comment) {
        if (comment == null || comment.isEmpty()) {
            return Collections.emptyList();
        }

        List<EnumItem> items = new ArrayList<>();
        for (String token : comment.split("\\s+", -1)) {
            if (!token.contains("=")) {
                continue;
            }
            Matcher matcher = ITEM.matcher(token);
            if (!matcher.matches()) {
                LOG.log(System.Logger.Level.WARNING,
                        "comment 中存在无法解析的枚举项文本，已跳过: {0}", token);
                continue;
            }
            // 正则保证 code/desc 组必匹配（matches 整体成功）；防御空值防 checker 可空告警
            String code = matcher.group(1);
            String desc = matcher.group(2);
            if (code == null || desc == null) {
                continue;
            }
            items.add(new EnumItem(code, desc, matcher.group(3)));
        }
        return Collections.unmodifiableList(items);
    }

}
