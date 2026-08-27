package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ThrowsTree;

public final class DocTagThrows implements ThrowsTree {

    ReferenceTree name;

    List<? extends DocTree> descs;

    public DocTagThrows(String name, String desc) {
        this(new DocReference(name), Arrays.asList(new DocText(desc)));
    }

    public DocTagThrows(ReferenceTree name, List<? extends DocTree> descs) {
        this.name = name;
        this.descs = new ArrayList<>(descs);
    }

    @Override
    public ReferenceTree getExceptionName() {
        return name;
    }

    @Override
    public List<? extends DocTree> getDescription() {
        return new ArrayList<>(descs);
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.THROWS;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitThrows(this, data);
    }

}
