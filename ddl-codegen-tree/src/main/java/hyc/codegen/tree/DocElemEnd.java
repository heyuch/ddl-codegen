package hyc.codegen.tree;

import javax.lang.model.element.Name;

import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.EndElementTree;

public final class DocElemEnd implements EndElementTree {

    private String name;

    public DocElemEnd(String name) {
        this.name = name;
    }

    @Override
    public Name getName() {
        return new StringName(name);
    }

    @Override
    public Kind getKind() {
        return Kind.END_ELEMENT;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitEndElement(this, data);
    }

}
