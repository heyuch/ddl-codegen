package hyc.codegen.tree.utils;

import java.util.Locale;

/** 命名相关的字符串工具。 */
public final class Names {

    private Names() {
        throw new AssertionError("no instances");
    }

    /** 首字母大写，其余不变；null/空串原样返回。 */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

}
