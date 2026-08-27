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

        u.setPackage(node.getPackage());
        for (com.sun.source.tree.ImportTree imp : node.getImports()) {
            u.addImport(imp);
        }

        for (Tree decl : node.getTypeDecls()) {
            Class c = (Class)decl.accept(this, TreePath.getPath(path, decl));
            u.addClass(c);
        }

        return u;
    }

    @Override
    public Tree visitClass(ClassTree node, TreePath path) {
        Class c = new Class();

        c.setJavadoc(javadocConverter.convert(docs.getDocCommentTree(path)));
        c.setModifiers(node.getModifiers());
        c.setKind(node.getKind());
        c.setName(node.getSimpleName());
        c.setTypeParameters(node.getTypeParameters());
        c.setExtendsClause(node.getExtendsClause());
        for (Tree impl : node.getImplementsClause()) {
            c.addImplements(impl);
        }

        List<? extends Tree> members = node.getMembers();
        for (Tree m : members) {
            Tree mm = m.accept(this, TreePath.getPath(path, m));

            if (mm instanceof Method) {
                Method method = (Method)mm;
                Name name = method.getName();
                if ("<init>".equals(name.toString())) {
                    method.setName(c.getSimpleName());
                }
            } else if (mm instanceof Variable) {
                Variable v = (Variable)mm;
                v.setVariableKind(VariableKind.FIELD);
            }

            c.addMember(mm);
        }

        return c;
    }

    @Override
    public Tree visitVariable(VariableTree node, TreePath path) {
        Variable v = new Variable();

        v.setJavadoc(javadocConverter.convert(docs.getDocCommentTree(path)));
        v.setModifiers(node.getModifiers());
        v.setName(node.getName());
        v.setNameExpr(node.getNameExpression());
        v.setType(node.getType());
        v.setInitExpr(node.getInitializer());

        return v;
    }

    @Override
    public Tree visitMethod(MethodTree node, TreePath path) {
        Method m = new Method();

        m.setJavadoc(javadocConverter.convert(docs.getDocCommentTree(path)));
        m.setModifiers(node.getModifiers());
        m.setName(node.getName());
        m.setReturnType(node.getReturnType());
        m.setTypeParameters(node.getTypeParameters());
        for (VariableTree p : node.getParameters()) {
            Variable pp = (Variable)p.accept(this, TreePath.getPath(path, p));
            pp.setVariableKind(VariableKind.PARAMETER);
            m.addParameter(pp);
        }
        m.setReceiverParameter(node.getReceiverParameter());
        m.setBody(node.getBody());
        m.setDefaultValue(node.getDefaultValue());

        return m;
    }

}
