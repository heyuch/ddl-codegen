package hyc.codegen.tree;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import hyc.codegen.tree.utils.CodePrinter;

public final class JavaCodegen extends TreeScanner<Boolean, CodePrinter> {

    public static String generateCode(Tree node) {
        StringWriter out = new StringWriter();
        generate(node, out);
        return out.toString();
    }

    public static void generate(Tree node, Writer out) {
        generate(node, new CodePrinter(out));
    }

    public static void generate(Tree node, CodePrinter out) {
        JavaCodegen g = new JavaCodegen();
        g.scan(node, out);
    }

    @Override
    public Boolean visitCompilationUnit(CompilationUnitTree node, CodePrinter p) {
        if (visitPackage(node.getPackage(), p)) {
            p.println();
        }

        ImportManager.print(node.getImports(), node.getPackage(), p, this::visitImport);
        foreachWith(node.getTypeDecls(), d -> d.accept(this, p), () -> p.println());

        return true;
    }

    @Override
    public Boolean visitPackage(PackageTree node, CodePrinter p) {
        if (node == null) {
            return false;
        }
        if (node instanceof Package) {
            p.stmt("package ", ((Package)node).getPath());
        } else {
            p.print(node);
        }
        return true;
    }

    private <T> void foreachWith(Collection<T> items, Consumer<T> fn, Runnable separator) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<T> list = new ArrayList<>(items);

        for (int i = 0, size = list.size(); i < size; i++) {
            T item = list.get(i);
            fn.accept(item);
            if (i < size - 1) {
                separator.run();
            }
        }
    }

    @Override
    public Boolean visitImport(ImportTree node, CodePrinter p) {
        if (node == null) {
            return false;
        }
        if (node instanceof Import) {
            p.stmt(node);
        } else {
            p.print(node);
        }
        return true;
    }

    @Override
    public Boolean visitClass(ClassTree node, CodePrinter p) {
        if (node instanceof Class) {
            DocCommentTree javadoc = ((Class)node).getJavadoc();
            if (javadoc != null) {
                JavadocCodegen.generate(javadoc, p);
            }
        }

        printClassHead(node, p);

        p.println(" {");
        p.println();
        p.indent();

        printEnumConstants(node, p);
        printFields(node, p);
        printMethods(node, p);
        printInnerClasses(node, p);

        p.undent();
        p.println("}");

        return true;
    }

    private void printClassHead(ClassTree node, CodePrinter p) {
        ModifiersTree modifiers = node.getModifiers();
        if (modifiers != null) {
            modifiers.accept(this, p);
        }

        Tree.Kind kind = node.getKind();
        if (kind == Tree.Kind.CLASS) {
            p.print("class ");
        } else if (kind == Tree.Kind.ENUM) {
            p.print("enum ");
        } else if (kind == Tree.Kind.INTERFACE) {
            p.print("interface ");
        }

        p.print(node.getSimpleName());

        List<? extends TypeParameterTree> typeParameters = node.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            p.print("<");
            foreachWith(typeParameters, t -> t.accept(this, p), () -> p.print(", "));
            p.print(">");
        }

        Tree extend = node.getExtendsClause();
        if (extend != null) {
            p.print(" extends ");
            extend.accept(this, p);
        }

        List<? extends Tree> impls = node.getImplementsClause();
        if (!impls.isEmpty()) {
            p.print(" implements ");
            foreachWith(impls, i -> i.accept(this, p), () -> p.print(", "));
        }
    }

    private void printEnumConstants(ClassTree node, CodePrinter p) {
        if (node.getKind() != Tree.Kind.ENUM) {
            return;
        }

        List<VariableTree> constants = collectMembers(node, VariableTree.class, m -> isEnumConstant(node, m));
        if (constants.isEmpty()) {
            return;
        }

        for (VariableTree v : constants) {
            visitEnumConstants(v, p);
            p.println(",");
            p.println();
        }

        p.println(";");
        p.println();
    }

    private void printFields(ClassTree node, CodePrinter p) {
        List<VariableTree> fields = collectMembers(node, VariableTree.class, m -> !isEnumConstant(node, m));

        for (Tree field : fields) {
            field.accept(this, p);
            p.println(";");
            p.println();
        }
    }

    private void printMethods(ClassTree node, CodePrinter p) {
        List<MethodTree> methods = collectMembers(node, MethodTree.class, null);

        for (Tree method : methods) {
            method.accept(this, p);
            p.println();
        }
    }

    private void printInnerClasses(ClassTree node, CodePrinter p) {
        List<ClassTree> classes = collectMembers(node, ClassTree.class, null);

        for (ClassTree c : classes) {
            c.accept(this, p);
            p.println();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> collectMembers(ClassTree node, java.lang.Class<T> expected, Predicate<T> filter) {
        List<? extends Tree> members = node.getMembers();
        List<T> result = new ArrayList<>();

        for (Tree member : members) {
            java.lang.Class<? extends Tree> c = member.getClass();
            if (expected.isAssignableFrom(c)) {
                if (filter != null) {
                    if (filter.test((T)member)) {
                        result.add((T)member);
                    }
                } else {
                    result.add((T)member);
                }
            }
        }

        return result;
    }

    private static boolean isEnumConstant(ClassTree c, VariableTree v) {
        if (c.getKind() != Tree.Kind.ENUM) {
            return false;
        }

        if (v instanceof Variable && ((Variable)v).getVariableKind() == VariableKind.ENUM_CONSTANT) {
            return true;
        }

        ModifiersTree mods = v.getModifiers();
        if (mods == null) {
            return false;
        }
        Set<Modifier> flags = mods.getFlags();
        if (!flags.contains(Modifier.PUBLIC)
                || !flags.contains(Modifier.STATIC)
                || !flags.contains(Modifier.FINAL)) {
            return false;
        }

        String className = String.valueOf(c.getSimpleName());
        String vtype = String.valueOf(v.getType());

        return vtype.equals(className);
    }

    public Boolean visitEnumConstants(VariableTree node, CodePrinter p) {
        if (node instanceof Variable) {
            DocCommentTree javadoc = ((Variable)node).getJavadoc();
            if (javadoc != null) {
                JavadocCodegen.generate(javadoc, p);
            }
        }

        visitModifierAnnotations(node.getModifiers(), p);

        p.print(node.getName());

        ExpressionTree init = node.getInitializer();
        if (init != null) {
            String code = generateCode(init);
            if (code.startsWith("new") && code.contains("(")) {
                code = code.substring(code.indexOf('('));
            }
            p.print(code);
        }

        return false;
    }

    public Boolean visitModifierAnnotations(ModifiersTree node, CodePrinter p) {
        if (node == null) {
            return false;
        }

        boolean annotationInline = false;
        if (node instanceof Modifiers) {
            annotationInline = ((Modifiers)node).isAnnotationInline();
        }

        List<? extends AnnotationTree> annotations = node.getAnnotations();
        if (annotations != null && !annotations.isEmpty()) {
            for (AnnotationTree anno : annotations) {
                anno.accept(this, p);
                if (annotationInline) {
                    p.print(" ");
                } else {
                    p.println();
                }
            }
        }

        return true;
    }

    @Override
    public Boolean visitNewClass(NewClassTree node, CodePrinter p) {
        p.print("new ", node.getIdentifier());
        p.print("(");
        List<? extends ExpressionTree> args = node.getArguments();
        foreachWith(args, arg -> arg.accept(this, p), () -> p.print(", "));
        p.print(")");
        return true;
    }

    @Override
    public Boolean visitModifiers(ModifiersTree node, CodePrinter p) {
        if (node == null) {
            return false;
        }

        visitModifierAnnotations(node, p);

        Set<Modifier> flags = node.getFlags();
        if (flags != null) {
            for (Modifier flag : flags) {
                p.print(flag);
                p.print(" ");
            }
        }

        return true;
    }

    @Override
    public Boolean visitVariable(VariableTree node, CodePrinter p) {
        if (node instanceof Variable) {
            DocCommentTree javadoc = ((Variable)node).getJavadoc();
            if (javadoc != null) {
                JavadocCodegen.generate(javadoc, p);
            }
        }

        ModifiersTree modifiers = node.getModifiers();
        if (modifiers != null) {
            modifiers.accept(this, p);
        }

        Tree type = node.getType();
        if (type != null) {
            type.accept(this, p);
            p.print(" ");
        }

        p.print(node.getName());

        boolean enumConstant = false;
        if (node instanceof Variable) {
            enumConstant = ((Variable)node).getVariableKind() == VariableKind.ENUM_CONSTANT;
        }

        ExpressionTree init = node.getInitializer();
        if (init != null) {
            if (!enumConstant) {
                p.print(" = ");
            }
            init.accept(this, p);
        }

        return false;
    }

    @Override
    public Boolean visitLiteral(LiteralTree node, CodePrinter p) {
        if (node instanceof SourceExpr) {
            p.print(((SourceExpr)node).getCode());
            return true;
        }

        String value;
        Tree.Kind kind = node.getKind();

        switch (kind) {
            case LONG_LITERAL:
                value = String.format("%sL", node.getValue());
                break;
            case STRING_LITERAL:
                value = String.format("\"%s\"", node.getValue());
                break;
            case CHAR_LITERAL:
                value = String.format("'%s'", node.getValue());
                break;
            case FLOAT_LITERAL:
                value = String.format("%sf", node.getValue());
                break;
            default:
                value = String.valueOf(node.getValue());
        }

        p.print(value);

        return true;
    }

    @Override
    public Boolean visitPrimitiveType(PrimitiveTypeTree node, CodePrinter p) {
        TypeKind kind = node.getPrimitiveTypeKind();
        String name = kind.name();
        p.print(name.toLowerCase(Locale.ROOT));
        return true;
    }

    @Override
    public Boolean visitParameterizedType(ParameterizedTypeTree node, CodePrinter p) {
        Tree type = node.getType();
        type.accept(this, p);

        List<? extends Tree> args = node.getTypeArguments();
        if (!args.isEmpty()) {
            p.print("<");
            foreachWith(args, arg -> arg.accept(this, p), () -> p.print(", "));
            p.print(">");
        }

        return true;
    }

    @Override
    public Boolean visitTypeParameter(TypeParameterTree node, CodePrinter p) {
        p.print(node.getName());

        // 支持 wildcard 和 bounds
        List<? extends Tree> bounds = node.getBounds();
        if (!bounds.isEmpty()) {
            bounds.forEach(b -> b.accept(this, p));
        }

        return true;
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree node, CodePrinter p) {
        p.print(node.getName());
        return false;
    }

    @Override
    public Boolean visitAnnotation(AnnotationTree node, CodePrinter p) {
        if (node instanceof Annotation) {
            Annotation a = (Annotation)node;
            p.print("@", ((TypeReference)a.getAnnotationType()).getName());
        } else {
            p.print("@", node.getAnnotationType());
        }

        List<? extends ExpressionTree> args = node.getArguments();
        if (args != null && !args.isEmpty()) {
            p.print("(");
            foreachWith(args, arg -> arg.accept(this, p), () -> p.print(", "));
            p.print(")");
        }

        return true;
    }

    @Override
    public Boolean visitMethod(MethodTree node, CodePrinter p) {
        if (node instanceof Method) {
            DocCommentTree javadoc = ((Method)node).getJavadoc();
            if (javadoc != null) {
                JavadocCodegen.generate(javadoc, p);
            }
        }

        ModifiersTree modifiers = node.getModifiers();
        if (modifiers != null) {
            modifiers.accept(this, p);
        }

        Tree returnType = node.getReturnType();
        if (returnType != null) {
            returnType.accept(this, p);
            p.print(" ");
        }
        p.print(node.getName());

        p.print("(");
        List<? extends VariableTree> parameters = node.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            foreachWith(parameters, v -> v.accept(this, p), () -> p.print(", "));
        }
        p.print(")");

        BlockTree body = node.getBody();
        if (body == null) {
            p.println(";");
        } else {
            p.print(" ");
            body.accept(this, p);
        }

        return true;
    }

    @Override
    public Boolean visitBlock(BlockTree node, CodePrinter p) {
        if (node.isStatic()) {
            p.print("static ");
        }

        p.println("{");
        p.indent();

        if (node instanceof SourceBlock) {
            visitSourceBlock((SourceBlock)node, p);
        } else {
            for (StatementTree stmt : node.getStatements()) {
                p.println(stmt);
            }
        }

        p.undent();
        p.println("}");

        return true;
    }

    private Boolean visitSourceBlock(SourceBlock node, CodePrinter p) {
        String code = node.getCode();
        if (code == null || code.isEmpty()) {
            return false;
        }

        int originIndents = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c != ' ') {
                originIndents = i;
                break;
            }
        }

        int currentIndents = p.getIndents();

        if (currentIndents > originIndents) {
            int adds = currentIndents - originIndents;
            String[] lines = code.split(System.lineSeparator());
            for (String line : lines) {
                p.printSpace(adds);
                p.printlnRaw(line);
            }
        } else if (currentIndents < originIndents) {
            int removal = originIndents - currentIndents;
            String[] lines = code.split(System.lineSeparator());
            for (String line : lines) {
                String trimmed = removeIndents(line, removal);
                p.printlnRaw(trimmed);
            }
        } else {
            String[] lines = code.split(System.lineSeparator());
            for (String line : lines) {
                p.printlnRaw(line);
            }
        }

        return true;
    }

    private String removeIndents(String line, int removal) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        for (int i = 0; i < removal; i++) {
            if (line.charAt(i) != ' ') {
                return line.substring(i);
            }
        }

        return line.substring(removal);
    }

}
