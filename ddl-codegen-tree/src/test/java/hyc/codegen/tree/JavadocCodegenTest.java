package hyc.codegen.tree;

import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;

import com.sun.source.doctree.DocTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// 组装型测试：引用类型数 ≈ 被测 DocTree 节点类型数（§6 元素驱动）
@SuppressWarnings("ClassDataAbstractionCoupling")
public class JavadocCodegenTest {

    @Test
    public void classDoc() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * This is a comment");
        j.add(" * <p>");
        j.add(" * 这里可以有多行描述。");
        j.add(" *");
        j.add(" * @author hyc");
        j.add(" * @since 2025-12-12");
        j.add(" * @version 1.0 测试版本");
        j.add(" * @see <a href=\"https://baidu.com\">百度</a>");
        j.add(" * @see DemoStatus#code");
        j.add(" */");
        j.add("");

        DocComment doc = DocComment.builder()
                .summary("This is a comment")
                .body(new DocElemStart("p"), System.lineSeparator()
                        + "这里可以有多行描述。")
                .tag(new DocTagAuthor("hyc"))
                .tag(new DocTagSince("2025-12-12"))
                .tag(new DocTagVersion("1.0 测试版本"))
                .tag(new DocTagSee(Docs.html("a", "百度", Map.of("href", "https://baidu.com"))))
                .tag(new DocTagSee(Docs.ref("DemoStatus#code")))
                .build();

        String s = codegen(doc);
        Assertions.assertEquals(j.toString(), s);
    }

    private String codegen(DocTree node) {
        return JavadocCodegen.generateCode(node);
    }

    @Test
    public void methodDoc() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * 方法功能概述");
        j.add(" * <p>");
        j.add(" * 详细描述");
        j.add(" *");
        j.add(" * @param <T> 类型参数 T 的描述");
        j.add(" * @param p1 第一个参数，描述其含义");
        j.add(" * @param param2 第二个参数，描述其含义");
        j.add(" * @return 返回值描述");
        j.add(" * @throws Exception 异常情况描述");
        j.add(" * @deprecated 请使用 {@link DemoStatus#code} 方法");
        j.add(" */");
        j.add("");

        DocComment doc = DocComment.builder()
                .summary("方法功能概述")
                .body("<p>")
                .body("详细描述")
                .tag(new DocTagParam("<T>", "类型参数 T 的描述", true))
                .tag(new DocTagParam("p1", "第一个参数，描述其含义"))
                .tag(new DocTagParam("param2", "第二个参数，描述其含义"))
                .tag(new DocTagReturn("返回值描述"))
                .tag(new DocTagThrows("Exception", "异常情况描述"))
                .tag(new DocTagDeprecated(Arrays.asList(
                        new DocText("请使用 "),
                        new DocLink("DemoStatus#code"),
                        new DocText(" 方法"))))
                .build();

        String s = codegen(doc);
        Assertions.assertEquals(j.toString(), s);
    }

    @Test
    public void fieldDoc() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * 字段概述");
        j.add(" */");
        j.add("");

        DocComment doc = DocComment.builder()
                .summary("字段概述")
                .build();

        String s = codegen(doc);
        Assertions.assertEquals(j.toString(), s);
    }

    @Test
    public void inlineTags() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * 使用 {@code code} 标签来表示代码片段。");
        j.add(" * 使用 {@link java.lang.String} 来链接到其他类或方法。");
        j.add(" * 也可以使用 {@link #inlineTags()} 链接到本类的方法。");
        j.add(" */");
        j.add("");

        DocComment doc = DocComment.builder()
                .summary("使用 ", new DocCode("code"), " 标签来表示代码片段。")
                .body("使用 ", new DocLink("java.lang.String"), " 来链接到其他类或方法。")
                .body("也可以使用 ", new DocLink("#inlineTags()"), " 链接到本类的方法。")
                .build();

        String s = codegen(doc);
        Assertions.assertEquals(j.toString(), s);
    }

    @Test
    public void htmlTags() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * 这是一个包含HTML标签的Javadoc。");
        j.add(" * <p>这是一个段落。</p>");
        j.add(" * <ul>");
        j.add(" *   <li>列表项1</li>");
        j.add(" *   <li>列表项2</li>");
        j.add(" * </ul>");
        j.add(" */");
        j.add("");

        DocComment doc = DocComment.builder()
                .summary("这是一个包含HTML标签的Javadoc。")
                .body(Docs.html("p", "这是一个段落。"))
                .body(new DocElemStart("ul"))
                .body("  ", Docs.html("li", "列表项1"))
                .body("  ", Docs.html("li", "列表项2"))
                .body(new DocElemEnd("ul"))
                .build();

        String s = codegen(doc);
        Assertions.assertEquals(j.toString(), s);
    }

}
