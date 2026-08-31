package hyc.codegen.tree;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.Tree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// 组装型测试：引用类型数 ≈ 被测 AST 节点类型数（§6 元素驱动）；StringJoiner 先声明后逐行填充的组装模式使声明距使用远（VDUD 固有）
@SuppressWarnings({"ClassDataAbstractionCoupling", "VariableDeclarationUsageDistance"})
public class JavaCodegenTest {

    @Test
    public void field() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * 编码列表");
        j.add(" */");
        j.add("@NotEmpty(message = \"编码列表不能为空\")");
        j.add("private List<String> codes = new ArrayList<>()");

        Variable v = Variable.builder()
                .javadoc(DocComment.builder()
                        .summary("编码列表")
                        .build())
                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotEmpty"),
                        "message = \"编码列表不能为空\""))
                .modifiers(Modifier.PRIVATE)
                .type(Types.listOf(Types.STRING))
                .name("codes")
                .init(SourceExpr.of("new ArrayList<>()", new TypeReference("java.util.ArrayList")))
                .build();

        String code = codegen(v);
        Assertions.assertEquals(j.toString(), code);

        List<Import> imports = v.getImports();
        List<Import> expectedImports = Arrays.asList(
                new Import(Types.LIST),
                new Import(Types.STRING),
                new Import("java.util.ArrayList"),
                new Import("javax.validation.constraints.NotEmpty"));
        Assertions.assertEquals(ImportManager.sort(expectedImports), ImportManager.sort(imports));
    }

    private String codegen(Tree tree) {
        return JavaCodegen.generateCode(tree);
    }

    @Test
    public void constant() {
        Variable v = Variable.builder()
                .modifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .type(Types.LONG)
                .name("serialVersionUID")
                .init(Literal.of(1L))
                .build();

        String code = codegen(v);
        Assertions.assertEquals("private static final long serialVersionUID = 1L", code);
    }

    @Test
    public void method() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        StringJoiner b = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * 根据编码获取对应的枚举值");
        j.add(" *");
        j.add(" * @param codes 编码列表");
        j.add(" * @return 匹配的枚举值，匹配不到返回 null");
        j.add(" */");
        j.add("@Nullable");
        j.add("public static List<TestEnum> getByCodes(@NotNull @NotEmpty(message = \"codes 不能为空\")"
                + " List<Integer> codes) {");
        b.add("    if (codes == null) {");
        b.add("        return null;");
        b.add("    }");
        b.add("");
        b.add("    for (Integer code : codes) {");
        b.add("        if (Objects.equals(e.code, code)) {");
        b.add("            return e;");
        b.add("        }");
        b.add("    }");
        b.add("");
        b.add("    return null;");
        j.merge(b);
        j.add("}");
        j.add("");

        Method m = Method.builder()
                .javadoc(DocComment.builder()
                        .summary("根据编码获取对应的枚举值")
                        .tag(new DocTagParam("codes", "编码列表"))
                        .tag(new DocTagReturn("匹配的枚举值，匹配不到返回 null"))
                        .build())
                .annotation(Annotation.of(new TypeReference("org.checkerframework.checker.nullness.qual.Nullable")))
                .modifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returnType(Types.listOf(new TypeReference("demo.TestEnum")))
                .name("getByCodes")
                .parameter(Variable.builder()
                        .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                        .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotEmpty"),
                                "message = \"codes 不能为空\""))
                        .type(Types.listOf(Types.INT_OBJ))
                        .name("codes")
                        .build())
                .body(b.toString())
                .build();

        String code = codegen(m);

        Assertions.assertEquals(j.toString(), code);
    }

    @Test
    public void interfaces() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        j.add("/**");
        j.add(" * Demo 数据仓库");
        j.add(" *");
        j.add(" * @author hyc");
        j.add(" * @since 2025-12-18");
        j.add(" * @version 1.0.0");
        j.add(" */");
        j.add("@FunctionalInterface");
        j.add("interface DemoRepo extends BaseRepo<Demo> {");
        j.add("");
        j.add("    boolean insert(@NotNull Demo demo);");
        j.add("");
        j.add("    @Nullable");
        j.add("    Demo findById(@NotNull Long id);");
        j.add("");
        j.add("}");
        j.add("");

        Class c = Class.builder()
                .javadoc(DocComment.builder()
                        .summary("Demo 数据仓库")
                        .tag(new DocTagAuthor("hyc"))
                        .tag(new DocTagSince("2025-12-18"))
                        .tag(new DocTagVersion("1.0.0"))
                        .build())
                .kind(Tree.Kind.INTERFACE)
                .annotation(Annotation.of(new TypeReference("java.lang.FunctionalInterface")))
                .name("DemoRepo")
                .extend(new ParameterizedType(new TypeReference("demo.BaseRepo"), new TypeReference("demo.Demo")))
                .method(Method.builder()
                        .returnType(Types.BOOLEAN)
                        .name("insert")
                        .parameter(Variable.builder()
                                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                                .type(new TypeReference("demo.Demo"))
                                .name("demo")
                                .build())
                        .build())
                .method(Method.builder()
                        .annotation(
                                Annotation.of(new TypeReference("org.checkerframework.checker.nullness.qual.Nullable")))
                        .returnType(new TypeReference("demo.Demo"))
                        .name("findById")
                        .parameter(Variable.builder()
                                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                                .type(Types.LONG_OBJ)
                                .name("id")
                                .build())
                        .build())
                .build();

        String code = codegen(c);

        Assertions.assertEquals(j.toString(), code);
    }

    @Test
    public void classes() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        StringJoiner a = new StringJoiner(System.lineSeparator());
        StringJoiner b = new StringJoiner(System.lineSeparator());
        j.add("@Repository");
        j.add("public class DemoRepoImpl implements DemoRepo {");
        j.add("");
        j.add("    @Autowired");
        j.add("    private DemoMapper mapper;");
        j.add("");
        j.add("    /**");
        j.add("     * 插入数据");
        j.add("     *");
        j.add("     * @param demo 待插入数据");
        j.add("     * @return 插入成功返回 true");
        j.add("     */");
        j.add("    @Override");
        j.add("    public boolean insert(@NotNull Demo demo) {");
        a.add("        int n = mapper.insert(demo);");
        a.add("        boolean ok = n > 0;");
        a.add("        return ok;");
        j.merge(a);
        j.add("    }");
        j.add("");
        j.add("    /**");
        j.add("     * 按 ID 查询数据");
        j.add("     *");
        j.add("     * @param id 待查询 ID");
        j.add("     * @return 查询不到返回 null");
        j.add("     */");
        j.add("    @Override");
        j.add("    @Nullable");
        j.add("    public Demo findById(@NotNull Long id) {");
        b.add("        Demo demo = mapper.findById(id);");
        b.add("        return demo;");
        j.merge(b);
        j.add("    }");
        j.add("");
        j.add("}");
        j.add("");

        Class c = Class.builder()
                .kind(Tree.Kind.CLASS)
                .annotation(Annotation.of(new TypeReference("org.springframework.stereotype.Repository")))
                .modifiers(Modifier.PUBLIC)
                .name("DemoRepoImpl")
                .implement(new TypeReference("demo.DemoRepo"))
                .field(Variable.builder()
                        .annotation(Annotation
                                .of(new TypeReference("org.springframework.beans.factory.annotation.Autowired")))
                        .modifiers(Modifier.PRIVATE)
                        .type(new TypeReference("demo.DemoMapper"))
                        .name("mapper")
                        .build())
                .method(Method.builder()
                        .javadoc(DocComment.builder()
                                .summary("插入数据")
                                .tag(new DocTagParam("demo", "待插入数据"))
                                .tag(new DocTagReturn("插入成功返回 true"))
                                .build())
                        .annotation(Annotation.of("java.lang.Override"))
                        .modifiers(Modifier.PUBLIC)
                        .returnType(Types.BOOLEAN)
                        .name("insert")
                        .parameter(Variable.builder()
                                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                                .type(new TypeReference("demo.Demo"))
                                .name("demo")
                                .build())
                        .body(a.toString())
                        .build())
                .method(Method.builder()
                        .javadoc(DocComment.builder()
                                .summary("按 ID 查询数据")
                                .tag(new DocTagParam("id", "待查询 ID"))
                                .tag(new DocTagReturn("查询不到返回 null"))
                                .build())
                        .annotation(Annotation.of("java.lang.Override"))
                        .annotation(Annotation.of("org.checkerframework.checker.nullness.qual.Nullable"))
                        .modifiers(Modifier.PUBLIC)
                        .returnType(new TypeReference("demo.Demo"))
                        .name("findById")
                        .parameter(Variable.builder()
                                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                                .type(Types.LONG_OBJ)
                                .name("id")
                                .build())
                        .body(b.toString())
                        .build())
                .build();

        String code = codegen(c);

        Assertions.assertEquals(j.toString(), code);
    }

    @Test
    public void enums() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        StringJoiner a = new StringJoiner(System.lineSeparator());
        StringJoiner b = new StringJoiner(System.lineSeparator());
        j.add("@Getter");
        j.add("public enum DemoStatus implements IBaseEnum<String> {");
        j.add("");
        j.add("    /**");
        j.add("     * 初始状态");
        j.add("     */");
        j.add("    @Generated(\"codegen.groovy\")");
        j.add("    INIT(\"init\", \"初始\"),");
        j.add("");
        j.add("    /**");
        j.add("     * 成功状态");
        j.add("     */");
        j.add("    @Generated(\"codegen.groovy\")");
        j.add("    OK(\"ok\", \"成功\"),");
        j.add("");
        j.add("    ;");
        j.add("");
        j.add("    private final String code;");
        j.add("");
        j.add("    private final String desc;");
        j.add("");
        j.add("    DemoStatus(String code, String desc) {");
        b.add("        this.code = code;");
        b.add("        this.desc = desc;");
        j.merge(b);
        j.add("    }");
        j.add("");
        j.add("    public static DemoStatus getByCode(@Nullable String code) {");
        a.add("        if (code == null) {");
        a.add("            return null;");
        a.add("        }");
        a.add("        for (DemoStatus status : values()) {");
        a.add("            if (Objects.equals(code, status.getCode())) {");
        a.add("                return status;");
        a.add("            }");
        a.add("        }");
        a.add("        return null;");
        j.merge(a);
        j.add("    }");
        j.add("");
        j.add("}");
        j.add("");

        Class c = Class.builder()
                .kind(Tree.Kind.ENUM)
                .annotation(Annotation.of(new TypeReference("lombok.Getter")))
                .modifiers(Modifier.PUBLIC)
                .name("DemoStatus")
                .implement(new ParameterizedType(new TypeReference("demo.IBaseEnum"), Types.STRING))
                .enumConstant(Variable.builder()
                        .javadoc(DocComment.builder()
                                .summary("初始状态")
                                .build())
                        .annotation(Annotation.of("javax.annotation.processing.Generated", "\"codegen.groovy\""))
                        .name("INIT")
                        .init(new SourceExpr("(\"init\", \"初始\")"))
                        .build())
                .enumConstant(Variable.builder()
                        .javadoc(DocComment.builder()
                                .summary("成功状态")
                                .build())
                        .annotation(Annotation.of("javax.annotation.processing.Generated", "\"codegen.groovy\""))
                        .name("OK")
                        .init(new SourceExpr("(\"ok\", \"成功\")"))
                        .build())
                .field(Variable.builder()
                        .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .type(Types.STRING)
                        .name("code")
                        .build())
                .field(Variable.builder()
                        .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .type(Types.STRING)
                        .name("desc")
                        .build())
                .method(Method.builder()
                        .name("DemoStatus")
                        .parameter(Variable.builder()
                                .type(Types.STRING)
                                .name("code")
                                .build())
                        .parameter(Variable.builder()
                                .type(Types.STRING)
                                .name("desc")
                                .build())
                        .body(b.toString())
                        .build())
                .method(Method.builder()
                        .modifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returnType(new TypeReference("demo.DemoStatus"))
                        .name("getByCode")
                        .parameter(Variable.builder()
                                .annotation(Annotation
                                        .of(new TypeReference("org.checkerframework.checker.nullness.qual.Nullable")))
                                .type(Types.STRING)
                                .name("code")
                                .build())
                        .body(a.toString())
                        .build())
                .build();

        String code = codegen(c);
        Assertions.assertEquals(j.toString(), code);
    }

    @Test
    public void compileUnit() {
        StringJoiner j = new StringJoiner(System.lineSeparator());
        StringJoiner a = new StringJoiner(System.lineSeparator());
        j.add("package demo;");
        j.add("");
        j.add("import javax.validation.constraints.NotNull;");
        j.add("");
        j.add("import org.checkerframework.checker.nullness.qual.Nullable;");
        j.add("import org.springframework.beans.factory.annotation.Autowired;");
        j.add("import org.springframework.stereotype.Repository;");
        j.add("");
        j.add("@FunctionalInterface");
        j.add("public interface DemoRepo extends BaseRepo<Demo> {");
        j.add("");
        j.add("    boolean insert(@NotNull Demo demo);");
        j.add("");
        j.add("    @Nullable");
        j.add("    Demo findById(@NotNull Long id);");
        j.add("");
        j.add("}");
        j.add("");
        j.add("@Repository");
        j.add("public static class DemoRepoImpl implements DemoRepo {");
        j.add("");
        j.add("    @Autowired");
        j.add("    private DemoMapper mapper;");
        j.add("");
        j.add("    @Nullable");
        j.add("    public Demo findById(@NotNull Long id) {");
        a.add("        Demo demo = mapper.findById(id);");
        a.add("        return demo;");
        j.merge(a);
        j.add("    }");
        j.add("");
        j.add("}");
        j.add("");

        CompileUnit u = new CompileUnit();
        Class i = hyc.codegen.tree.Class.builder()
                .pkg("demo")
                .kind(Tree.Kind.INTERFACE)
                .annotation(Annotation.of(new TypeReference("java.lang.FunctionalInterface")))
                .modifiers(Modifier.PUBLIC)
                .name("DemoRepo")
                .extend(new ParameterizedType(new TypeReference("demo.BaseRepo"), new TypeReference("demo.Demo")))
                .method(Method.builder()
                        .returnType(Types.BOOLEAN)
                        .name("insert")
                        .parameter(Variable.builder()
                                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                                .type(new TypeReference("demo.Demo"))
                                .name("demo")
                                .build())
                        .build())
                .method(Method.builder()
                        .annotation(
                                Annotation.of(new TypeReference("org.checkerframework.checker.nullness.qual.Nullable")))
                        .returnType(new TypeReference("demo.Demo"))
                        .name("findById")
                        .parameter(Variable.builder()
                                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                                .type(Types.LONG_OBJ)
                                .name("id")
                                .build())
                        .build())
                .build();
        u.addClass(i);

        Class c = Class.builder()
                .pkg("demo")
                .kind(Tree.Kind.CLASS)
                .annotation(Annotation.of("org.springframework.stereotype.Repository"))
                .modifiers(Modifier.PUBLIC, Modifier.STATIC)
                .name("DemoRepoImpl")
                .implement(new TypeReference("demo.DemoRepo"))
                .field(Variable.builder()
                        .annotation(Annotation
                                .of(new TypeReference("org.springframework.beans.factory.annotation.Autowired")))
                        .modifiers(Modifier.PRIVATE)
                        .type(new TypeReference("demo.DemoMapper"))
                        .name("mapper")
                        .build())
                .method(Method.builder()
                        .annotation(
                                Annotation.of(new TypeReference("org.checkerframework.checker.nullness.qual.Nullable")))
                        .modifiers(Modifier.PUBLIC)
                        .returnType(new TypeReference("demo.Demo"))
                        .name("findById")
                        .parameter(Variable.builder()
                                .annotation(Annotation.of(new TypeReference("javax.validation.constraints.NotNull")))
                                .type(Types.LONG_OBJ)
                                .name("id")
                                .build())
                        .body(a.toString())
                        .build())
                .build();
        u.addClass(c);

        String code = codegen(u);
        Assertions.assertEquals(j.toString(), code);
    }

}
