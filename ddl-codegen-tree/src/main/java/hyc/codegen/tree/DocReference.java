package hyc.codegen.tree;

import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ReferenceTree;

public final class DocReference implements ReferenceTree {

    private String signature;

    public DocReference(String signature) {
        this.signature = signature;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitReference(this, data);
    }

    @Override
    public Kind getKind() {
        return Kind.REFERENCE;
    }

    @Override
    public String getSignature() {
        return signature;
    }

}
