package hyc.codegen.tree;

import javax.lang.model.element.Name;

import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.IdentifierTree;

public final class DocIdent implements IdentifierTree {

    String name;

    public DocIdent(String name) {
        this.name = name;
    }

    @Override
    public Name getName() {
        return new StringName(name);
    }

    @Override
    public Kind getKind() {
        return Kind.IDENTIFIER;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitIdentifier(this, data);
    }

}
