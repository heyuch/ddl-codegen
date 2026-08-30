package hyc.codegen.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * TypeReference equals 契约：lombok canEqual 保证跨类型对称；callSuper=true 下
 * 三种构造方式（qname / pkg+name / Package+name）应产生相等的值对象。
 */
class TypeReferenceTest {

    @Test
    void constructorsProduceEqualValues() {
        TypeReference byQname = new TypeReference("com.foo.Bar");
        TypeReference byParts = new TypeReference("com.foo", "Bar");
        TypeReference byPackage = new TypeReference(Package.of("com.foo"), "Bar");

        assertEquals(byQname, byParts);
        assertEquals(byQname, byPackage);
        assertEquals(byQname.hashCode(), byParts.hashCode());
        assertEquals(byQname.hashCode(), byPackage.hashCode());
    }

    @Test
    void crossTypeEqualsIsSymmetric() {
        Identifier id = new Identifier("com.foo.Bar");
        TypeReference tr = new TypeReference("com.foo.Bar");

        // canEqual 机制：Identifier.equals(TypeReference) 与反向均返回 false，严格对称
        assertNotEquals(id, tr);
        assertNotEquals(tr, id);
    }

}
