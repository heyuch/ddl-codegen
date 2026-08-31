package hyc.codegen.tree;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TreeVisitor;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Literal implements LiteralTree {

    private @Nullable Object value;

    private Kind kind;

    public Literal(@Nullable Object value, Kind kind) {
        this.value = value;
        this.kind = kind;
    }

    public static Literal of(String value) {
        return new Literal(value, Kind.STRING_LITERAL);
    }

    public static Literal of(boolean value) {
        return new Literal(value, Kind.BOOLEAN_LITERAL);
    }

    public static Literal of(char value) {
        return new Literal(value, Kind.CHAR_LITERAL);
    }

    public static Literal of(double value) {
        return new Literal(value, Kind.DOUBLE_LITERAL);
    }

    public static Literal of(float value) {
        return new Literal(value, Kind.FLOAT_LITERAL);
    }

    public static Literal of(int value) {
        return new Literal(value, Kind.INT_LITERAL);
    }

    public static Literal of(long value) {
        return new Literal(value, Kind.LONG_LITERAL);
    }

    public static Literal ofNull() {
        return new Literal(null, Kind.NULL_LITERAL);
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitLiteral(this, data);
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    @Nullable
    // javac tree API 语义：NULL_LITERAL 的 getValue() 返回 null
    @SuppressWarnings("override.return")
    public Object getValue() {
        return value;
    }

}
