package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TreeVisitor;

public final class SourceExpr implements LiteralTree {

    private String code;

    private List<Import> imports = new ArrayList<>();

    public SourceExpr(String code) {
        if (code == null) {
            throw new NullPointerException("code is null");
        }
        this.code = code;
    }

    public SourceExpr(String code, List<Import> imports) {
        this.code = code;
        this.imports = new ArrayList<>(imports);
    }

    public static SourceExpr of(String code, ParameterizedType type) {
        return new SourceExpr(code, type.getImports());
    }

    public static SourceExpr of(String code, TypeReference type) {
        return new SourceExpr(code, Arrays.asList(type.getImport()));
    }

    public static SourceExpr of(String code, List<Object> types) {
        Set<Import> imports = new HashSet<>();
        for (Object type : types) {
            if (type instanceof TypeReference) {
                imports.add(((TypeReference)type).getImport());
            } else if (type instanceof ParameterizedType) {
                imports.addAll(((ParameterizedType)type).getImports());
            }
        }

        return new SourceExpr(code, new ArrayList<>(imports));
    }

    @Override
    public Object getValue() {
        return code;
    }

    /**
     * 返回原始源码字符串。
     */
    public String getCode() {
        return code;
    }

    @Override
    public Kind getKind() {
        return Kind.STRING_LITERAL;
    }

    public List<Import> getImports() {
        return new ArrayList<>(imports);
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitLiteral(this, data);
    }

    @Override
    public String toString() {
        return code;
    }

}
