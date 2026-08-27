package hyc.codegen.tree;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TreeVisitor;

public final class Literal implements LiteralTree {

    private Object value;

    private Kind kind;

    public Literal(Object value, Kind kind) {
        this.value = value;
        this.kind = kind;
    }

    public static Literal of(int value) {
        return new Literal(value, Kind.INT_LITERAL);
    }

    public static Literal of(long value) {
        return new Literal(value, Kind.LONG_LITERAL);
    }

    public static Literal of(float value) {
        return new Literal(value, Kind.FLOAT_LITERAL);
    }

    public static Literal of(double value) {
        return new Literal(value, Kind.DOUBLE_LITERAL);
    }

    public static Literal of(boolean value) {
        return new Literal(value, Kind.BOOLEAN_LITERAL);
    }

    public static Literal of(char value) {
        return new Literal(value, Kind.CHAR_LITERAL);
    }

    public static Literal of(String value) {
        return new Literal(value, Kind.STRING_LITERAL);
    }

    public static Literal ofNull() {
        return new Literal(null, Kind.NULL_LITERAL);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitLiteral(this, data);
    }

}
