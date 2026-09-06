package hyc.codegen.tree;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import hyc.codegen.tree.utils.CodePrinter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

// 扇出抑制依据（元素驱动而非逻辑混杂，见 docs/static-rules-review.md §6）：
// 本类是 TreeScanner 分发器，每个节点类型对应一个 visit 方法，引用类型数 ≈ 节点类型数；
// 已抽取 import 管理（ImportManager）后残余扇出仍 39，实证为结构性不可降；单方法引用类型 ≤3。
@SuppressWarnings({"ClassFanOutComplexity", "NPathComplexity"})
public final class JavaCodegen extends TreeScanner<Boolean, CodePrinter> {

    /**
     * 显式建模打印的节点类型集合。未在集合中的节点统一走 {@link #printLines} 原文兜底
     * （javac 节点 toString 忠实于源码），保证 round-trip 语义零丢失。
     * 新增 visit 方法时必须把对应类型登记进来，否则会被兜底吞掉。
     */
    private static final Set<Tree.Kind> HANDLED_KINDS = EnumSet.of(
            Tree.Kind.COMPILATION_UNIT, Tree.Kind.PACKAGE, Tree.Kind.IMPORT,
            Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE,
            Tree.Kind.METHOD, Tree.Kind.VARIABLE, Tree.Kind.MODIFIERS,
            Tree.Kind.ANNOTATION, Tree.Kind.PRIMITIVE_TYPE,
            Tree.Kind.PARAMETERIZED_TYPE, Tree.Kind.TYPE_PARAMETER,
            Tree.Kind.IDENTIFIER, Tree.Kind.BLOCK, Tree.Kind.ARRAY_TYPE,
            Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.FLOAT_LITERAL,
            Tree.Kind.DOUBLE_LITERAL, Tree.Kind.BOOLEAN_LITERAL,
            Tree.Kind.CHAR_LITERAL, Tree.Kind.STRING_LITERAL, Tree.Kind.NULL_LITERAL);

    @SuppressWarnings("unchecked")
    private static <T> List<T> collectMembers(ClassTree node, java.lang.Class<T> expected,
            @Nullable Predicate<T> filter) {
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

    /** 解码 javac toString 输出的 {@code \\uXXXX}（前面无成对反斜杠者）；其余原样保留。 */
    private static String decodeUnicodeEscapes(String code) {
        StringBuilder sb = new StringBuilder(code.length());
        for (int i = 0; i < code.length();) {
            char c = code.charAt(i);
            if (c == '\\' && i + 1 < code.length() && code.charAt(i + 1) == '\\') {
                // 真实反斜杠（javac 输出 \\）：成对拷贝
                sb.append('\\').append('\\');
                i += 2;
            } else if (c == '\\' && i + 5 < code.length() && code.charAt(i + 1) == 'u' && isHex(code, i + 2)) {
                sb.append((char)Integer.parseInt(code.substring(i + 2, i + 6), 16));
                i += 6;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 枚举常量 init 的规范化文本（打印器 {@link #visitEnumConstants} 与 reconcile 字段签名共用，避免两处漂移）：
     * {@code new X(a)} → {@code (a)}；裸值（字符串字面量等）→ {@code (value)}；已括号开头原样；空白归一化为单空格。
     * <p>
     * javac 的 toString 会把字符串内的非 ASCII 输出为 {@code \\uXXXX} 转义；生成侧 SourceExpr 写原文——
     * 统一在此解码回字符（仅当 {@code \\u} 前无反斜杠对，真实反斜杠被 javac 输出为 {@code \\} 不受影响），
     * 保证两侧规范化文本一致（幂等、可读）。
     */
    public static String enumConstantInitText(ExpressionTree init) {
        String code = generateCode(init);
        if (code.startsWith("new") && code.contains("(")) {
            // new Foo(x) → (x)
            code = code.substring(code.indexOf('('));
        } else if (!code.startsWith("(")) {
            // 裸值（如字符串字面量）→ (value)
            code = "(" + code + ")";
        }
        return decodeUnicodeEscapes(code).replaceAll("\\s+", " ").trim();
    }

    /**
     * Java 字符字面量转义。
     */
    private static String escapeChar(char c) {
        switch (c) {
            case '\'':
                return "\\'";
            case '\\':
                return "\\\\";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            default:
                if (c < 0x20) {
                    return String.format("\\u%04x", (int)c);
                }
                return String.valueOf(c);
        }
    }

    /**
     * Java 字符串字面量转义：仅转义必须转义的字符，非 ASCII 保持原文（可读性）。
     */
    private static String escapeString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static void generate(Tree node, CodePrinter out) {
        JavaCodegen g = new JavaCodegen();
        g.scan(node, out);
    }

    public static void generate(Tree node, Writer out) {
        generate(node, new CodePrinter(out));
    }

    public static String generateCode(Tree node) {
        StringWriter out = new StringWriter();
        generate(node, out);
        return out.toString();
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

    private static boolean isHex(String s, int from) {
        for (int i = from; i < from + 4; i++) {
            char value = s.charAt(i);
            boolean digit = value >= '0' && value <= '9';
            boolean lowerHex = value >= 'a' && value <= 'f';
            boolean upperHex = value >= 'A' && value <= 'F';
            if (!(digit || lowerHex || upperHex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断变量是否为可变参数。模型节点由转换器在解析时检测并标记（{@link Variable#isVarargs()}）；
     * 未经转换的 javac 节点靠 toString 中的 "..." 识别（JDK 11 无 Modifier.VARARGS）。
     */
    private static boolean isVarargs(VariableTree node) {
        if (!(node.getType() instanceof ArrayTypeTree)) {
            return false;
        }
        if (node instanceof Variable) {
            return ((Variable)node).isVarargs();
        }
        return node.toString().contains("...");
    }

    private static int leadingSpaces(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static String removeIndents(String line, int removal) {
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

    private static String trimTrailingSpaces(String line) {
        int end = line.length();
        while (end > 0 && line.charAt(end - 1) == ' ') {
            end--;
        }
        return line.substring(0, end);
    }

    private <T> void foreachWith(Collection<? extends @UnknownKeyFor T> items, Consumer<T> fn, Runnable separator) {
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

    private boolean isHandled(Tree tree) {
        if (tree instanceof SourceExpr || tree instanceof SourceBlock
                || tree instanceof Literal || tree instanceof TypeReference
                || tree instanceof Identifier || tree instanceof ParameterizedType) {
            return true;
        }
        Tree.Kind kind = tree.getKind();
        return kind != null && HANDLED_KINDS.contains(kind);
    }

    private void printClassHead(ClassTree node, CodePrinter p) {
        ModifiersTree modifiers = node.getModifiers();
        if (modifiers != null) {
            scan(modifiers, p);
        }

        Tree.Kind kind = node.getKind();
        if (kind == Tree.Kind.CLASS) {
            p.write("class ");
        } else if (kind == Tree.Kind.ENUM) {
            p.write("enum ");
        } else if (kind == Tree.Kind.INTERFACE) {
            p.write("interface ");
        }

        p.write(node.getSimpleName());

        List<? extends TypeParameterTree> typeParameters = node.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            p.write("<");
            foreachWith(typeParameters, t -> scan(t, p), () -> p.write(", "));
            p.write(">");
        }

        Tree extend = node.getExtendsClause();
        if (extend != null) {
            p.write(" extends ");
            scan(extend, p);
        }

        List<? extends Tree> impls = node.getImplementsClause();
        if (!impls.isEmpty()) {
            p.write(" implements ");
            foreachWith(impls, i -> scan(i, p), () -> p.write(", "));
        }
    }

    private void printEnumConstants(ClassTree node, CodePrinter p) {
        if (node.getKind() != Tree.Kind.ENUM) {
            return;
        }

        List<VariableTree> constants = collectMembers(node, VariableTree.class, m -> isEnumConstant(node, m));
        boolean hasBodyMembers = !collectMembers(node, VariableTree.class, m -> !isEnumConstant(node, m)).isEmpty()
                || !collectMembers(node, MethodTree.class, null).isEmpty()
                || !collectMembers(node, ClassTree.class, null).isEmpty();
        if (constants.isEmpty() && !hasBodyMembers) {
            // 空枚举（无常量亦无成员）：合法，无需分号
            return;
        }

        for (VariableTree v : constants) {
            visitEnumConstants(v, p);
            p.line(",");
            p.newline();
        }

        // 枚举体只要有成员（含仅常量），常量列表后必须打 ';'（JLS：enum 体成员前需分号）
        p.line(";");
        p.newline();
    }

    private void printFields(ClassTree node, CodePrinter p) {
        List<VariableTree> fields = collectMembers(node, VariableTree.class, m -> !isEnumConstant(node, m));

        for (Tree field : fields) {
            scan(field, p);
            p.line(";");
            p.newline();
        }
    }

    private void printInnerClasses(ClassTree node, CodePrinter p) {
        List<ClassTree> classes = collectMembers(node, ClassTree.class, null);

        for (ClassTree c : classes) {
            scan(c, p);
            p.newline();
        }
    }

    /**
     * 逐行打印文本：以首行缩进为基准，将整个文本相对当前缩进重对齐；
     * 空行不补缩进（避免尾随空格）。
     */
    private void printLines(@Nullable String code, CodePrinter p) {
        if (code == null || code.isEmpty()) {
            return;
        }

        String[] lines = code.split("\n", -1);
        int end = lines.length;
        // 去掉末尾换行产生的空行
        if (end > 1 && lines[end - 1].isEmpty()) {
            end--;
        }

        int originIndents = leadingSpaces(lines[0]);
        int shift = p.indentSpaces() - originIndents;

        for (int i = 0; i < end; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                p.rawLine(line);
                continue;
            }
            // javac 某些节点 toString 会带尾随空格（如 switch 的 case 行），统一清理
            line = trimTrailingSpaces(line);
            if (shift >= 0) {
                p.raw(" ".repeat(shift));
                p.rawLine(line);
            } else {
                p.rawLine(removeIndents(line, -shift));
            }
        }
    }

    private void printMethods(ClassTree node, CodePrinter p) {
        List<MethodTree> methods = collectMembers(node, MethodTree.class, null);

        for (Tree method : methods) {
            scan(method, p);
            p.newline();
        }
    }

    private void printParameters(MethodTree node, CodePrinter p) {
        p.write("(");
        VariableTree receiver = node.getReceiverParameter();
        if (receiver != null) {
            scan(receiver, p);
            if (!node.getParameters().isEmpty()) {
                p.write(", ");
            }
        }
        List<? extends VariableTree> parameters = node.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            foreachWith(parameters, v -> scan(v, p), () -> p.write(", "));
        }
        p.write(")");
    }

    /**
     * 打印语句：局部变量声明补分号，多行文本按当前缩进重对齐。
     */
    private void printStatement(StatementTree stmt, CodePrinter p) {
        String code = stmt.toString();
        if (stmt instanceof VariableTree) {
            // javac 的局部变量声明 toString 不含分号
            code += ";";
        }
        printLines(code, p);
    }

    private void printThrows(MethodTree node, CodePrinter p) {
        List<? extends ExpressionTree> throwsList = node.getThrows();
        if (throwsList == null || throwsList.isEmpty()) {
            return;
        }
        p.write(" throws ");
        foreachWith(throwsList, t -> scan(t, p), () -> p.write(", "));
    }

    private void printTypeParameters(MethodTree node, CodePrinter p) {
        List<? extends TypeParameterTree> typeParams = node.getTypeParameters();
        if (typeParams == null || typeParams.isEmpty()) {
            return;
        }
        p.write("<");
        foreachWith(typeParams, t -> scan(t, p), () -> p.write(", "));
        p.write("> ");
    }

    @Override
    @Nullable
    // TreeScanner 契约：返回 null 表示「无替换结果」（javac 语义，调用方忽略返回值）
    @SuppressWarnings("override.return")
    public Boolean scan(Tree tree, CodePrinter p) {
        if (tree == null) {
            return Boolean.FALSE;
        }
        if (!isHandled(tree)) {
            // 兜底：未显式建模的节点直接内联输出 javac 源码文本（toString 忠实于源码）
            p.write(tree.toString());
            return false;
        }
        return super.scan(tree, p);
    }

    @Override
    public Boolean visitAnnotation(AnnotationTree node, CodePrinter p) {
        if (node instanceof Annotation) {
            Annotation a = (Annotation)node;
            p.write("@", ((TypeReference)a.getAnnotationType()).getName());
        } else {
            p.write("@", node.getAnnotationType());
        }

        List<? extends ExpressionTree> args = node.getArguments();
        if (args != null && !args.isEmpty()) {
            p.write("(");
            foreachWith(args, arg -> scan(arg, p), () -> p.write(", "));
            p.write(")");
        }

        return true;
    }

    @Override
    public Boolean visitArrayType(ArrayTypeTree node, CodePrinter p) {
        scan(node.getType(), p);
        p.write("[]");
        return false;
    }

    @Override
    public Boolean visitBlock(BlockTree node, CodePrinter p) {
        if (node.isStatic()) {
            p.write("static ");
        }

        p.line("{");
        p.indent();

        if (node instanceof SourceBlock) {
            printLines(((SourceBlock)node).getCode(), p);
        } else {
            for (StatementTree stmt : node.getStatements()) {
                printStatement(stmt, p);
            }
        }

        p.undent();
        p.line("}");

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

        p.line(" {");
        p.newline();
        p.indent();

        printEnumConstants(node, p);
        printFields(node, p);
        printMethods(node, p);
        printInnerClasses(node, p);

        p.undent();
        p.line("}");

        return true;
    }

    @Override
    public Boolean visitCompilationUnit(CompilationUnitTree node, CodePrinter p) {
        if (visitPackage(node.getPackage(), p)) {
            p.newline();
        }

        ImportManager.print(node.getImports(), node.getPackage(), p, this::visitImport);
        foreachWith(node.getTypeDecls(), d -> scan(d, p), () -> p.newline());

        return true;
    }

    public Boolean visitEnumConstants(VariableTree node, CodePrinter p) {
        if (node instanceof Variable) {
            DocCommentTree javadoc = ((Variable)node).getJavadoc();
            if (javadoc != null) {
                JavadocCodegen.generate(javadoc, p);
            }
        }

        visitModifierAnnotations(node.getModifiers(), p);

        p.write(node.getName());

        ExpressionTree init = node.getInitializer();
        if (init != null) {
            p.write(enumConstantInitText(init));
        }

        return false;
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree node, CodePrinter p) {
        p.write(node.getName());
        return false;
    }

    @Override
    public Boolean visitImport(ImportTree node, CodePrinter p) {
        if (node == null) {
            return false;
        }
        if (node instanceof Import) {
            p.line(node, ";");
        } else {
            p.write(node);
        }
        return true;
    }

    @Override
    public Boolean visitLiteral(LiteralTree node, CodePrinter p) {
        if (node instanceof SourceExpr) {
            p.write(((SourceExpr)node).getCode());
            return true;
        }

        String value;
        Tree.Kind kind = node.getKind();

        switch (kind) {
            case LONG_LITERAL:
                value = String.format("%sL", node.getValue());
                break;
            case STRING_LITERAL:
                // javac 的 toString 会把字符串内单引号转义为 \'，且无法处理生成侧未转义的值；
                // 统一在此做转义感知输出，保证 round-trip 与生成两侧一致
                value = "\"" + escapeString((String)node.getValue()) + "\"";
                break;
            case CHAR_LITERAL:
                value = "'" + escapeChar((char)node.getValue()) + "'";
                break;
            case FLOAT_LITERAL:
                value = String.format("%sf", node.getValue());
                break;
            default:
                value = String.valueOf(node.getValue());
        }

        p.write(value);

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
            scan(modifiers, p);
        }

        printTypeParameters(node, p);

        Tree returnType = node.getReturnType();
        if (returnType != null) {
            scan(returnType, p);
            p.write(" ");
        }
        p.write(node.getName());

        printParameters(node, p);
        printThrows(node, p);

        BlockTree body = node.getBody();
        if (body == null) {
            p.line(";");
        } else {
            p.write(" ");
            scan(body, p);
        }

        return true;
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
                scan(anno, p);
                if (annotationInline) {
                    p.write(" ");
                } else {
                    p.newline();
                }
            }
        }

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
                p.write(flag);
                p.write(" ");
            }
        }

        return true;
    }

    @Override
    public Boolean visitPackage(PackageTree node, CodePrinter p) {
        if (node == null) {
            return false;
        }
        if (node instanceof Package) {
            p.line("package ", ((Package)node).getPath(), ";");
        } else {
            p.write(node);
        }
        return true;
    }

    @Override
    public Boolean visitParameterizedType(ParameterizedTypeTree node, CodePrinter p) {
        Tree type = node.getType();
        scan(type, p);

        List<? extends Tree> args = node.getTypeArguments();
        if (!args.isEmpty()) {
            p.write("<");
            foreachWith(args, arg -> scan(arg, p), () -> p.write(", "));
            p.write(">");
        }

        return true;
    }

    @Override
    public Boolean visitPrimitiveType(PrimitiveTypeTree node, CodePrinter p) {
        TypeKind kind = node.getPrimitiveTypeKind();
        String name = kind.name();
        p.write(name.toLowerCase(Locale.ROOT));
        return true;
    }

    @Override
    public Boolean visitTypeParameter(TypeParameterTree node, CodePrinter p) {
        p.write(node.getName());

        // 支持 wildcard 和 bounds
        List<? extends Tree> bounds = node.getBounds();
        if (!bounds.isEmpty()) {
            bounds.forEach(b -> scan(b, p));
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
            scan(modifiers, p);
        }

        boolean varargs = isVarargs(node);
        Tree type = node.getType();
        if (type != null) {
            if (varargs && type instanceof ArrayTypeTree) {
                scan(((ArrayTypeTree)type).getType(), p);
                p.write("...");
            } else {
                scan(type, p);
                p.write(" ");
            }
        }

        p.write(node.getName());

        boolean enumConstant = false;
        if (node instanceof Variable) {
            enumConstant = ((Variable)node).getVariableKind() == VariableKind.ENUM_CONSTANT;
        }

        ExpressionTree init = node.getInitializer();
        if (init != null) {
            if (!enumConstant) {
                p.write(" = ");
            }
            scan(init, p);
        }

        return false;
    }

}
