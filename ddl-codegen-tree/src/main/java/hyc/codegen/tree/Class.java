package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import hyc.codegen.tree.utils.U;

public final class Class implements ClassTree {

    DocComment javadoc;

    Package pkg;

    ModifiersTree modifiers;

    Kind kind;

    Name name;

    List<TypeParameterTree> typeParameters = new ArrayList<>();

    Tree extend;

    List<Tree> impls = new ArrayList<>();

    List<VariableTree> fields = new ArrayList<>();

    List<MethodTree> methods = new ArrayList<>();

    List<ClassTree> innerClasses = new ArrayList<>();

    @Override
    public ModifiersTree getModifiers() {
        return modifiers;
    }

    @Override
    public Name getSimpleName() {
        return name;
    }

    @Override
    public List<? extends TypeParameterTree> getTypeParameters() {
        return new ArrayList<>(typeParameters);
    }

    @Override
    public Tree getExtendsClause() {
        return extend;
    }

    @Override
    public List<? extends Tree> getImplementsClause() {
        return new ArrayList<>(impls);
    }

    @Override
    public List<? extends Tree> getMembers() {
        List<Tree> members = new ArrayList<>();

        members.addAll(fields);
        members.addAll(methods);
        members.addAll(innerClasses);

        return members;
    }

    public void addMember(Tree member) {
        if (member instanceof VariableTree) {
            fields.add((VariableTree)member);
        } else if (member instanceof MethodTree) {
            methods.add((MethodTree)member);
        } else if (member instanceof ClassTree) {
            innerClasses.add((ClassTree)member);
        }
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitClass(this, data);
    }

    public List<Import> getImports() {
        return ImportCollector.collect(this);
    }

    public TypeReference getTypeReference() {
        return new TypeReference(pkg.path, name.toString());
    }

    public void addAnnotation(AnnotationTree a) {
        if (a == null) {
            return;
        }

        if (modifiers instanceof Modifiers) {
            ((Modifiers)modifiers).addAnnotation(a);
        } else {
            Modifiers mod = new Modifiers();
            modifiers = mod;
        }
    }

    public void addField(VariableTree field) {
        if (field == null) {
            return;
        }
        fields.add(field);
    }

    public void addProperty(VariableTree prop) {
        addProperty(prop, null, null);
    }

    public void addProperty(VariableTree prop, @Nullable Consumer<Method> getterFn,
            @Nullable Consumer<Method> setterFn) {
        if (prop == null) {
            return;
        }
        addField(prop);
        addGetter(prop, getterFn);
        addSetter(prop, setterFn);
    }

    public void addGetter(VariableTree prop, @Nullable Consumer<Method> fn) {
        String propName = String.valueOf(prop.getName());
        Method getter = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(prop.getType())
                .name("get" + U.capitalize(propName))
                .body("return " + propName + ";")
                .build();

        if (fn != null) {
            fn.accept(getter);
        }

        addMethod(getter);
    }

    public void addSetter(VariableTree prop, @Nullable Consumer<Method> fn) {
        String propName = String.valueOf(prop.getName());
        Method setter = Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(Types.VOID)
                .name("set" + U.capitalize(propName))
                .parameter(Variable.builder()
                        .type(prop.getType())
                        .name(propName)
                        .build())
                .body(String.format("this.%s = %s;", propName, propName))
                .build();

        if (fn != null) {
            fn.accept(setter);
        }

        addMethod(setter);
    }

    public void addMethod(MethodTree method) {
        if (method == null) {
            return;
        }
        this.methods.add(method);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        Class c;

        public Builder() {
            this.c = new Class();
            c.kind = Kind.CLASS;
        }

        public Builder pkg(String path) {
            if (path == null) {
                return this;
            }
            c.pkg = Package.of(path);
            return this;
        }

        public Builder pkg(Package pkg) {
            c.pkg = pkg;
            return this;
        }

        public Builder kind(Kind kind) {
            c.kind = kind;
            return this;
        }

        public Builder javadoc(DocComment doc) {
            c.javadoc = doc;
            return this;
        }

        public Builder annotation(Annotation anno) {
            if (c.modifiers == null) {
                c.modifiers = new Modifiers();
            }

            if (c.modifiers instanceof Modifiers) {
                ((Modifiers)c.modifiers).addAnnotation(anno);
            }

            return this;
        }

        public Builder modifiers(Modifier... modifiers) {
            Modifiers mods = Modifiers.of(modifiers);
            if (c.modifiers != null) {
                if (c.modifiers instanceof Modifiers) {
                    mods.annotations.addAll(((Modifiers)c.modifiers).annotations);
                }
            }
            c.modifiers = mods;
            return this;
        }

        public Builder mods(Modifiers modifiers) {
            c.modifiers = modifiers;
            return this;
        }

        public Builder name(String name) {
            c.name = new StringName(name);
            return this;
        }

        public Builder typeParameter(TypeParameter p) {
            c.typeParameters.add(p);
            return this;
        }

        public Builder extend(TypeReference p) {
            c.extend = p;
            return this;
        }

        public Builder extend(ParameterizedType p) {
            c.extend = p;
            return this;
        }

        public Builder implement(TypeReference i) {
            c.impls.add(i);
            return this;
        }

        public Builder implement(ParameterizedType i) {
            c.impls.add(i);
            return this;
        }

        public Builder enumConstant(Variable f) {
            if (f.kind == null) {
                f.kind = VariableKind.ENUM_CONSTANT;
            }
            c.fields.add(f);
            return this;
        }

        public Builder field(Variable f) {
            if (f.kind == null) {
                f.kind = VariableKind.FIELD;
            }
            c.addField(f);
            return this;
        }

        public Builder property(VariableTree prop) {
            c.addProperty(prop);
            return this;
        }

        public Builder method(Method m) {
            c.addMethod(m);
            return this;
        }

        public Class build() {
            return c;
        }

    }

}
