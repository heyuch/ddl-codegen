package hyc.codegen.tree;

import javax.lang.model.type.TypeKind;

import com.sun.source.tree.Tree;

public final class Types {

    public static final PrimitiveType VOID = new PrimitiveType(TypeKind.VOID);
    public static final TypeReference VOID_OBJ = new TypeReference("java.lang.Void");
    public static final PrimitiveType BYTE = new PrimitiveType(TypeKind.BYTE);
    public static final TypeReference BYTE_OBJ = new TypeReference("java.lang.Byte");
    public static final PrimitiveType BOOLEAN = new PrimitiveType(TypeKind.BOOLEAN);
    public static final TypeReference BOOLEAN_OBJ = new TypeReference("java.lang.Boolean");
    public static final PrimitiveType CHAR = new PrimitiveType(TypeKind.CHAR);
    public static final TypeReference CHAR_OBJ = new TypeReference("java.lang.Character");
    public static final PrimitiveType SHORT = new PrimitiveType(TypeKind.SHORT);
    public static final TypeReference SHORT_OBJ = new TypeReference("java.lang.Short");
    public static final PrimitiveType INT = new PrimitiveType(TypeKind.INT);
    public static final TypeReference INT_OBJ = new TypeReference("java.lang.Integer");
    public static final PrimitiveType LONG = new PrimitiveType(TypeKind.LONG);
    public static final TypeReference LONG_OBJ = new TypeReference("java.lang.Long");
    public static final PrimitiveType FLOAT = new PrimitiveType(TypeKind.FLOAT);
    public static final TypeReference FLOAT_OBJ = new TypeReference("java.lang.Float");
    public static final PrimitiveType DOUBLE = new PrimitiveType(TypeKind.DOUBLE);
    public static final TypeReference DOUBLE_OBJ = new TypeReference("java.lang.Double");

    public static final TypeReference STRING = new TypeReference("java.lang.String");
    public static final TypeReference OBJECT = new TypeReference("java.lang.Object");

    public static final TypeReference LIST = new TypeReference("java.util.List");
    public static final TypeReference SET = new TypeReference("java.util.Set");
    public static final TypeReference MAP = new TypeReference("java.util.Map");

    public static ParameterizedType listOf(Tree elementType) {
        return new ParameterizedType(LIST, elementType);
    }

    public static ParameterizedType mapOf(TypeReference keyType, Tree valueType) {
        return new ParameterizedType(MAP, keyType, valueType);
    }

    public static ParameterizedType setOf(Tree elementType) {
        return new ParameterizedType(SET, elementType);
    }

}
