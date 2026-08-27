package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.VariableTree;

public final class Variable implements VariableTree {

    DocComment javadoc;

    VariableKind kind;

    ModifiersTree modifiers;

    Name name;

    ExpressionTree nameExpr;

    Tree type;

    ExpressionTree initExpr;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ModifiersTree getModifiers() {
        if (modifiers == null) {
            modifiers = new Modifiers();
        }

        if (kind == VariableKind.PARAMETER) {
            if (modifiers instanceof Modifiers) {
                ((Modifiers)modifiers).annotationInline = true;
            }
        }

        return modifiers;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public ExpressionTree getNameExpression() {
        return nameExpr;
    }

    @Override
    public Tree getType() {
        return type;
    }

    @Override
    public ExpressionTree getInitializer() {
        return initExpr;
    }

    @Override
    public Kind getKind() {
        return Kind.VARIABLE;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitVariable(this, data);
    }

    public List<Import> getImports() {
        List<Import> imports = new ArrayList<>();

        if (type instanceof TypeReference) {
            imports.add(((TypeReference)type).getImport());
        } else if (type instanceof ParameterizedType) {
            imports.addAll(((ParameterizedType)type).getImports());
        }

        if (modifiers instanceof Modifiers) {
            imports.addAll(((Modifiers)modifiers).getImports());
        }

        if (initExpr instanceof SourceExpr) {
            imports.addAll(((SourceExpr)initExpr).getImports());
        }

        return imports;
    }

    public void addAnnotation(AnnotationTree a) {
        if (a == null) {
            return;
        }

        if (modifiers instanceof Modifiers) {
            ((Modifiers)modifiers).addAnnotation(a);
        } else {
            Modifiers mod = new Modifiers();
            this.modifiers = mod;
        }
    }

    public static final class Builder {

        Variable v;

        public Builder() {
            this.v = new Variable();
        }

        public Builder kind(VariableKind kind) {
            v.kind = kind;
            return this;
        }

        public Builder javadoc(DocComment doc) {
            v.javadoc = doc;
            return this;
        }

        public Builder annotation(Annotation anno) {
            if (anno == null) {
                return this;
            }

            if (v.modifiers == null) {
                v.modifiers = new Modifiers();
            }

            if (v.modifiers instanceof Modifiers) {
                ((Modifiers)v.modifiers).addAnnotation(anno);
            }

            return this;
        }

        public Builder modifiers(Modifier... modifiers) {
            Modifiers mods = Modifiers.of(modifiers);
            if (v.modifiers instanceof Modifiers) {
                mods.annotations.addAll(((Modifiers)v.modifiers).annotations);
            }
            v.modifiers = mods;
            return this;
        }

        public Builder mods(Modifiers modifiers) {
            v.modifiers = modifiers;
            return this;
        }

        public Builder type(PrimitiveType type) {
            v.type = type;
            return this;
        }

        public Builder type(TypeReference type) {
            v.type = type;
            return this;
        }

        public Builder type(ParameterizedType type) {
            v.type = type;
            return this;
        }

        public Builder type(Tree type) {
            v.type = type;
            return this;
        }

        public Builder name(String name) {
            v.name = new StringName(name);
            return this;
        }

        public Builder nameExpr(ExpressionTree expr) {
            v.nameExpr = expr;
            return this;
        }

        public Builder init(ExpressionTree init) {
            v.initExpr = init;
            return this;
        }

        public Variable build() {
            return v;
        }

    }

}
