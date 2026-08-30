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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// 可修改 AST 节点（AGENTS.md「自研可修改 Java AST」）：字段由静态工厂/builder 在构造后设置，
public final class Variable implements VariableTree {

    private @Nullable DocComment javadoc;

    private @Nullable VariableKind kind;

    @MonotonicNonNull
    private ModifiersTree modifiers;

    @MonotonicNonNull
    private Name name;

    private @Nullable ExpressionTree nameExpr;

    @MonotonicNonNull
    private Tree type;

    private @Nullable ExpressionTree initExpr;

    /** 是否为可变参数（仅方法参数有意义，JDK 11 无 Modifier.VARARGS，由转换器从 javac toString 检测） */
    private boolean varargs;

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回变量 javadoc 注释。
     */
    public @Nullable DocComment getJavadoc() {
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
    public void setJavadoc(@Nullable DocComment javadoc) {
        this.javadoc = javadoc;
    }

    /**
     * 返回变量种类（字段/参数/枚举常量）。
     */
    public @Nullable VariableKind getVariableKind() {
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
    public void setNameExpr(@Nullable ExpressionTree nameExpr) {
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
        if (name == null) {
            throw new IllegalStateException("name 未初始化（构建器/转换器未设置）");
        }
        return name;
    }

    @Override
    @Nullable
    // javac tree API 语义：非注解参数变量 getNameExpression 返回 null
    @SuppressWarnings("override.return")
    public ExpressionTree getNameExpression() {
        return nameExpr;
    }

    @Override
    public Tree getType() {
        if (type == null) {
            throw new IllegalStateException("type 未初始化（构建器/转换器未设置）");
        }
        return type;
    }

    @Override
    @Nullable
    // javac tree API 语义：无初始化值的变量 getInitializer 返回 null
    @SuppressWarnings("override.return")
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
            if (((TypeReference)type).getPkg() != null) {
                imports.add(((TypeReference)type).getImport());
            }
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

        if (modifiers == null) {
            Modifiers mod = new Modifiers();
            mod.addAnnotation(a);
            this.modifiers = mod;
        } else if (modifiers instanceof Modifiers) {
            ((Modifiers)modifiers).addAnnotation(a);
        } else {
            // 非模型 ModifiersTree（如解析出的 javac 节点）：复制现有注解与修饰符到模型容器，避免丢失
            Modifiers mod = new Modifiers(modifiers.getFlags());
            mod.addAnnotations(new ArrayList<>(modifiers.getAnnotations()));
            mod.addAnnotation(a);
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
