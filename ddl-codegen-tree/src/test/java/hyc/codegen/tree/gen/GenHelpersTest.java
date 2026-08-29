package hyc.codegen.tree.gen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Expr/Block 助手的字符串拼接行为。 */
class GenHelpersTest {

    @Test
    void testCallAndMember() {
        assertEquals("po.getId()", Expr.call("po", "getId"));
        assertEquals("Gender.fromValue(po.getGender())", Expr.call("Gender", "fromValue", "po.getGender()"));
        assertEquals("converter.toEntity(mapper.findById(id))",
                Expr.call("converter", "toEntity", Expr.call("mapper", "findById", "id")));
        assertEquals("user.name", Expr.member("user", "name"));
    }

    @Test
    void testTernaryAndNullSafe() {
        assertEquals("a ? b : c", Expr.ternary("a", "b", "c"));
        assertEquals("po.getGender() == null ? null : Gender.fromValue(po.getGender())",
                Expr.nullSafe("po.getGender()", Expr.call("Gender", "fromValue", "po.getGender()")));
    }

    @Test
    void testStatements() {
        String body = Block.statements(
                "User u = new User();",
                "u.setId(po.getId());",
                "return u;");
        String expected = String.join(System.lineSeparator(),
                "User u = new User();",
                "u.setId(po.getId());",
                "return u;");
        assertEquals(expected, body);
    }

    @Test
    void testIfStmt() {
        String code = Block.ifStmt("po.getGender() != null", "u.setGender(po.getGender().name());");
        String expected = "if (po.getGender() != null) {" + System.lineSeparator()
                + "    u.setGender(po.getGender().name());" + System.lineSeparator()
                + "}";
        assertEquals(expected, code);
    }

    /** docs/design.md §10 的 plain converter toEntity 方法体示例。 */
    @Test
    void testToEntityBody() {
        String body = Block.statements(
                "User u = new User();",
                "u.setId(po.getId());",
                "u.setGender("
                        + Expr.nullSafe("po.getGender()", Expr.call("Gender", "fromValue", "po.getGender()"))
                        + ");",
                "return u;");
        String expected = String.join(System.lineSeparator(),
                "User u = new User();",
                "u.setId(po.getId());",
                "u.setGender(po.getGender() == null ? null : Gender.fromValue(po.getGender()));",
                "return u;");
        assertEquals(expected, body);
    }

}
