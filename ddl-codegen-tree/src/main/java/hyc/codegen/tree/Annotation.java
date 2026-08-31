package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;

public final class Annotation implements AnnotationTree {

    private TypeReference type;

    private List<ExpressionTree> arguments;

    public Annotation(TypeReference type, List<? extends ExpressionTree> arguments) {
        this.type = type;
        this.arguments = new ArrayList<>(arguments);
    }

    public static Annotation of(String qname) {
        return of(new TypeReference(qname));
    }

    public static Annotation of(String qname, Map<String, Object> arguments) {
        List<String> args = new ArrayList<>();
        arguments.forEach((k, v) -> {
            Object value = v;
            if (v instanceof String) {
                value = "\"" + v + "\"";
            }
            args.add(k + "=" + value);
        });

        return of(new TypeReference(qname), args);
    }

    public static Annotation of(String qname, String argument) {
        return of(new TypeReference(qname), argument);
    }

    public static Annotation of(TypeReference type) {
        return new Annotation(type, new ArrayList<>());
    }

    public static Annotation of(TypeReference type, List<String> arguments) {
        List<SourceExpr> args = arguments.stream()
                .map(arg -> new SourceExpr(arg))
                .collect(Collectors.toList());
        return new Annotation(type, args);
    }

    public static Annotation of(TypeReference type, String argument) {
        return new Annotation(type, Arrays.asList(new SourceExpr(argument)));
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotation(this, data);
    }

    @Override
    public Tree getAnnotationType() {
        return type;
    }

    @Override
    public List<? extends ExpressionTree> getArguments() {
        return new ArrayList<>(arguments);
    }

    public List<Import> getImports() {
        List<Import> imports = new ArrayList<>();

        imports.add(new Import(type));

        if (arguments != null) {
            for (ExpressionTree arg : arguments) {
                if (arg instanceof SourceExpr) {
                    imports.addAll(((SourceExpr)arg).getImports());
                }
            }
        }

        return imports;
    }

    @Override
    public Kind getKind() {
        // Kind.TYPE_ANNOTATION;
        return Kind.ANNOTATION;
    }

}
