package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.ParamTree;

public final class DocTagParam implements ParamTree {

    String name;
    List<? extends DocTree> descs;
    boolean typeParameter;

    public DocTagParam(String name, String descs) {
        this(name, Arrays.asList(new DocText(descs)), false);
    }

    public DocTagParam(String name, List<? extends DocTree> descs, boolean typeParameter) {
        this.name = name;
        this.descs = new ArrayList<>(descs);
        this.typeParameter = typeParameter;
    }

    public DocTagParam(String name, String descs, boolean typeParameter) {
        this(name, Arrays.asList(new DocText(descs)), typeParameter);
    }

    @Override
    public boolean isTypeParameter() {
        return typeParameter;
    }

    @Override
    public IdentifierTree getName() {
        return new DocIdent(name);
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
        return Kind.PARAM;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitParam(this, data);
    }

}
