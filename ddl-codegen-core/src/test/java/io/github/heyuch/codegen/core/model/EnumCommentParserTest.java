package io.github.heyuch.codegen.core.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 枚举列表 grammar 解析：code=desc / code=desc(name)、正文 token 跳过、结构不符容忍。
 */
class EnumCommentParserTest {

    private static void assertItem(EnumItem item, String rawCode, String desc,
            @org.checkerframework.checker.nullness.qual.Nullable String name) {
        assertEquals(rawCode, item.getRawCode());
        assertEquals(desc, item.getDesc());
        assertEquals(name, item.getName());
    }

    @Test
    void annotationAndPlainTokensAreSkipped() {
        // @enum:Status 与正文前缀不含 '='，自然跳过
        List<EnumItem> items = EnumCommentParser.parse("状态 1=初始(INIT) @enum:Status");
        assertEquals(1, items.size());
        assertItem(items.get(0), "1", "初始", "INIT");
    }

    @Test
    void descContainingEqualSignKeptInDesc() {
        List<EnumItem> items = EnumCommentParser.parse("a=b=c");
        assertEquals(1, items.size());
        assertEquals("a", items.get(0).getRawCode());
        assertEquals("b=c", items.get(0).getDesc());
        assertNull(items.get(0).getName());
    }

    @Test
    void descMayContainNonAsciiParenthesis() {
        // 中文全角括号不是 name 定界符，属 desc 内容
        List<EnumItem> items = EnumCommentParser.parse("1=初始（默认）");
        assertEquals(1, items.size());
        assertItem(items.get(0), "1", "初始（默认）", null);
    }

    @Test
    void leadingZerosKeepRawText() {
        List<EnumItem> items = EnumCommentParser.parse("01=初始(INIT)");
        assertEquals(1, items.size());
        assertEquals("01", items.get(0).getRawCode());
        assertEquals("INIT", items.get(0).getName());
    }

    @Test
    void malformedTokensWithEqualsAreSkipped() {
        // 结构不符（括号后缀外还有内容）→ 跳过该 token
        List<EnumItem> items = EnumCommentParser.parse("1=初始(INIT)extra 2=活跃(ACTIVE)");
        assertEquals(1, items.size());
        assertItem(items.get(0), "2", "活跃", "ACTIVE");
    }

    @Test
    void nullAndEmptyCommentYieldNoItems() {
        assertTrue(EnumCommentParser.parse(null).isEmpty());
        assertTrue(EnumCommentParser.parse("").isEmpty());
        assertTrue(EnumCommentParser.parse("   ").isEmpty());
    }

    @Test
    void parsesItemsWithAndWithoutName() {
        List<EnumItem> items = EnumCommentParser.parse("状态 1=初始(INIT) 2=活跃(ACTIVE) NORMAL=普通");
        assertEquals(3, items.size());
        assertItem(items.get(0), "1", "初始", "INIT");
        assertItem(items.get(1), "2", "活跃", "ACTIVE");
        assertItem(items.get(2), "NORMAL", "普通", null);
    }

}
