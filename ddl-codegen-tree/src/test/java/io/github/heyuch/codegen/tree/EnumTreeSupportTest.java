package io.github.heyuch.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.Tree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 枚举 AST 支撑回归：空枚举（无常量带成员）打 {@code ;}、解析侧常量 kind/init 规范化、
 * {@code Class.replaceField} 原位替换保序。
 */
class EnumTreeSupportTest {

    private static Variable field(String name) {
        return Variable.builder().type(new TypeReference("int")).name(name).build();
    }

    @Test
    void enumWithoutConstantsButWithMembersPrintsSemicolon() throws Exception {
        Class cls = Class.builder()
                .kind(Tree.Kind.ENUM)
                .modifiers(Modifier.PUBLIC)
                .name("Empty")
                .field(Variable.builder()
                        .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .type(new TypeReference("int"))
                        .name("code")
                        .build())
                .build();
        String code = JavaCodegen.generateCode(cls);
        Assertions.assertTrue(code.contains("public enum Empty {"), code);
        Assertions.assertTrue(code.contains("    ;"), code);
        Assertions.assertTrue(code.contains("private final int code;"), code);
    }

    private Class firstClass(String source) throws Exception {
        CompileUnit cu = new JavaParser().parseCode(source).get(0);
        return (Class)cu.getTypeDecls().get(0);
    }

    @Test
    void fullyEmptyEnumPrintsNoSemicolon() throws Exception {
        Class cls = Class.builder()
                .kind(Tree.Kind.ENUM)
                .modifiers(Modifier.PUBLIC)
                .name("Empty")
                .build();
        String code = JavaCodegen.generateCode(cls);
        Assertions.assertTrue(code.contains("public enum Empty {"), code);
        Assertions.assertFalse(code.contains(";"), code);
    }

    @Test
    void parsedEnumConstantsCarryKindAndNormalizedInit() throws Exception {
        Class cls = firstClass("public enum Status {\n"
                + "    INIT(1, \"初始\"),\n"
                + "    ACTIVE(2, \"活跃\"),\n"
                + "    ;\n"
                + "    private final Integer code;\n"
                + "    private final String desc;\n"
                + "}\n");
        Assertions.assertEquals(Tree.Kind.ENUM, cls.getKind());

        List<String> constants = new ArrayList<>();
        for (Variable field : cls.getFields()) {
            if (field.getVariableKind() == VariableKind.ENUM_CONSTANT) {
                constants.add(field.getName().toString());
                Assertions.assertNotNull(field.getInitializer(), "枚举常量应带 init");
            } else {
                Assertions.assertEquals(VariableKind.FIELD, field.getVariableKind(), "非常量成员应为 FIELD");
            }
        }
        Assertions.assertEquals(List.of("INIT", "ACTIVE"), constants);

        Variable first = cls.getFields().get(0);
        com.sun.source.tree.ExpressionTree init = first.getInitializer();
        if (init == null) {
            throw new AssertionError("枚举常量应带 init");
        }
        Assertions.assertEquals("(1, \"初始\")", JavaCodegen.enumConstantInitText(init));
    }

    @Test
    void parsedEnumRoundTripKeepsConstantArgs() throws Exception {
        String source = "public enum Status {\n"
                + "    INIT(1, \"初始\"),\n"
                + "    ;\n"
                + "    private final Integer code;\n"
                + "}\n";
        String code = JavaCodegen.generateCode(firstClass(source));
        Assertions.assertTrue(code.contains("INIT(1, \"初始\")"), code);
    }

    @Test
    void replaceFieldKeepsOrderInPlace() throws Exception {
        Class cls = Class.builder()
                .kind(Tree.Kind.CLASS)
                .modifiers(Modifier.PUBLIC)
                .name("Demo")
                .field(field("a"))
                .field(field("b"))
                .field(field("c"))
                .build();

        List<Variable> fields = cls.getFields();
        Variable oldB = fields.get(1);
        Variable newB = Variable.builder().type(new TypeReference("int")).name("b").build();

        Assertions.assertTrue(cls.replaceField(oldB, newB));
        Assertions.assertFalse(cls.replaceField(field("absent"), newB));

        List<String> names = new ArrayList<>();
        for (Variable field : cls.getFields()) {
            names.add(field.getName().toString());
        }
        Assertions.assertEquals(List.of("a", "b", "c"), names);
    }

}
