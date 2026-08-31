package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ThrowsTree;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

public final class DocTagThrows implements ThrowsTree {

    private ReferenceTree name;

    private List<? extends DocTree> descs;

    public DocTagThrows(ReferenceTree name, List<? extends DocTree> descs) {
        this.name = name;
        this.descs = new ArrayList<@UnknownKeyFor DocTree>(descs);
    }

    public DocTagThrows(String name, String desc) {
        this(new DocReference(name), Arrays.asList(new DocText(desc)));
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitThrows(this, data);
    }

    @Override
    public List<? extends DocTree> getDescription() {
        return new ArrayList<@UnknownKeyFor DocTree>(descs);
    }

    @Override
    public ReferenceTree getExceptionName() {
        return name;
    }

    @Override
    public Kind getKind() {
        return Kind.THROWS;
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

}
