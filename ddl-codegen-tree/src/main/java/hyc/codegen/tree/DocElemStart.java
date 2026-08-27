package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Name;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.StartElementTree;

public final class DocElemStart implements StartElementTree {

    String name;
    boolean selfClosing;
    List<? extends DocTree> attrs;

    public DocElemStart(String name) {
        this(name, false, new ArrayList<>());
    }

    public DocElemStart(String name, boolean selfClosing, List<? extends DocTree> attrs) {
        this.name = name;
        this.selfClosing = selfClosing;
        this.attrs = new ArrayList<>(attrs);
    }

    public DocElemStart(String name, boolean selfClosing) {
        this(name, selfClosing, new ArrayList<>());
    }

    @Override
    public Name getName() {
        return new StringName(name);
    }

    @Override
    public List<? extends DocTree> getAttributes() {
        return new ArrayList<>(attrs);
    }

    @Override
    public boolean isSelfClosing() {
        return selfClosing;
    }

    @Override
    public Kind getKind() {
        return Kind.START_ELEMENT;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitStartElement(this, data);
    }

}
