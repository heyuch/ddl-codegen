package hyc.codegen.tree;

import java.util.List;
import javax.lang.model.element.Name;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;

class JavaTreeConverter extends TreeScanner<Tree, TreePath> {

    final JavadocTreeConverter javadocConverter = new JavadocTreeConverter();

    DocTrees docs;

    CompileUnit convert(CompilationUnitTree unit, DocTrees docs) {
        this.docs = docs;
        TreePath path = new TreePath(unit);

        return (CompileUnit)visitCompilationUnit(unit, path);
    }

    @Override
    public Tree visitCompilationUnit(CompilationUnitTree node, TreePath path) {
        CompileUnit u = new CompileUnit();

        u.pkg = node.getPackage();
        u.imports.addAll(node.getImports());

        for (Tree decl : node.getTypeDecls()) {
            ClassTree c = (ClassTree)decl.accept(this, TreePath.getPath(path, decl));
            u.classes.add(c);
        }

        return u;
    }

    @Override
    public Tree visitClass(ClassTree node, TreePath path) {
        Class c = new Class();

        c.javadoc = javadocConverter.convert(docs.getDocCommentTree(path));
        c.modifiers = node.getModifiers();
        c.kind = node.getKind();
        c.name = node.getSimpleName();
        c.typeParameters.addAll(node.getTypeParameters());
        c.extend = node.getExtendsClause();
        c.impls.addAll(node.getImplementsClause());

        List<? extends Tree> members = node.getMembers();
        for (Tree m : members) {
            Tree mm = m.accept(this, TreePath.getPath(path, m));

            if (mm instanceof Method) {
                Method method = (Method)mm;
                Name name = method.getName();
                if ("<init>".equals(name.toString())) {
                    method.name = c.getSimpleName();
                }
            } else if (mm instanceof Variable) {
                Variable v = (Variable)mm;
                v.kind = VariableKind.FIELD;
            }

            c.addMember(mm);
        }

        return c;
    }

    @Override
    public Tree visitVariable(VariableTree node, TreePath path) {
        Variable v = new Variable();

        v.javadoc = javadocConverter.convert(docs.getDocCommentTree(path));
        v.modifiers = node.getModifiers();
        v.name = node.getName();
        v.nameExpr = node.getNameExpression();
        v.type = node.getType();
        v.initExpr = node.getInitializer();

        return v;
    }

    @Override
    public Tree visitMethod(MethodTree node, TreePath path) {
        Method m = new Method();

        m.javadoc = javadocConverter.convert(docs.getDocCommentTree(path));
        m.modifiers = node.getModifiers();
        m.name = node.getName();
        m.returnType = node.getReturnType();
        m.typeParameters.addAll(node.getTypeParameters());
        for (VariableTree p : node.getParameters()) {
            Variable pp = (Variable)p.accept(this, TreePath.getPath(path, p));
            pp.kind = VariableKind.PARAMETER;
            m.parameters.add(pp);
        }
        m.receiverParameter = node.getReceiverParameter();
        m.body = node.getBody();
        m.defaultValue = node.getDefaultValue();

        return m;
    }

}
