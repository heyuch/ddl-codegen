package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.ReferenceTree;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

public final class DocLink implements LinkTree {

    private ReferenceTree ref;
    private List<? extends DocTree> labels;

    public DocLink(String ref) {
        this(new DocReference(ref), new ArrayList<@UnknownKeyFor DocTree>());
    }

    public DocLink(ReferenceTree ref, List<? extends DocTree> labels) {
        this.ref = ref;
        this.labels = new ArrayList<@UnknownKeyFor DocTree>(labels);
    }

    public DocLink(String ref, String label) {
        this(new DocReference(ref), Arrays.asList(new DocText(label)));
    }

    @Override
    public ReferenceTree getReference() {
        return ref;
    }

    @Override
    public List<? extends DocTree> getLabel() {
        return new ArrayList<@UnknownKeyFor DocTree>(labels);
    }

    @Override
    public String getTagName() {
        return getKind().tagName;
    }

    @Override
    public Kind getKind() {
        return Kind.LINK;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitLink(this, data);
    }

}
