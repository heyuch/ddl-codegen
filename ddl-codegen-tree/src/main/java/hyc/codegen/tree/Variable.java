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

    private DocComment javadoc;

    private VariableKind kind;

    private ModifiersTree modifiers;

    private Name name;

    private ExpressionTree nameExpr;

    private Tree type;

    private ExpressionTree initExpr;

    /** 是否为可变参数（仅方法参数有意义，JDK 11 无 Modifier.VARARGS，由转换器从 javac toString 检测） */
    private boolean varargs;

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回变量 javadoc 注释。
     */
    public DocComment getJavadoc() {
        return javadoc;
    }

    /**
     * 返回是否为可变参数。
     */
    public boolean isVarargs() {
        return varargs;
    }

    /**
     * 设置是否为可变参数。
     */
    public void setVarargs(boolean varargs) {
        this.varargs = varargs;
    }

    /**
     * 设置变量 javadoc 注释。
     */
    public void setJavadoc(DocComment javadoc) {
        this.javadoc = javadoc;
    }

    /**
     * 返回变量种类（字段/参数/枚举常量）。
     */
    public VariableKind getVariableKind() {
        return kind;
    }

    /**
     * 设置变量种类。
     */
    public void setVariableKind(VariableKind kind) {
        this.kind = kind;
    }

    /**
     * 设置修饰符。
     */
    public void setModifiers(ModifiersTree modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * 设置变量名。
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * 设置接收者名称表达式。
     */
    public void setNameExpr(ExpressionTree nameExpr) {
        this.nameExpr = nameExpr;
    }

    /**
     * 设置变量类型。
     */
    public void setType(Tree type) {
        this.type = type;
    }

    /**
     * 设置初始化表达式。
     */
    public void setInitExpr(ExpressionTree initExpr) {
        this.initExpr = initExpr;
    }

    @Override
    public ModifiersTree getModifiers() {
        if (modifiers == null) {
            modifiers = new Modifiers();
        }

        if (kind == VariableKind.PARAMETER) {
            if (modifiers instanceof Modifiers) {
                ((Modifiers)modifiers).setAnnotationInline(true);
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
                mods.addAnnotations(((Modifiers)v.modifiers).getAnnotations());
            }
            v.modifiers = mods;
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
