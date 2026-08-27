package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Name;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;

public final class DocAttr implements AttributeTree {

    String name;

    ValueKind valueKind;

    List<? extends DocTree> value;

    public DocAttr(String name, String value) {
        this(name, new DocText(value));
    }

    public DocAttr(String name, DocTree value) {
        this(name, ValueKind.DOUBLE, Arrays.asList(value));
    }

    public DocAttr(String name, ValueKind valueKind, List<? extends DocTree> value) {
        this.name = name;
        this.valueKind = valueKind;
        this.value = new ArrayList<>(value);
    }

    @Override
    public Name getName() {
        return new StringName(name);
    }

    @Override
    public ValueKind getValueKind() {
        return valueKind;
    }

    @Override
    public List<? extends DocTree> getValue() {
        return new ArrayList<>(value);
    }

    @Override
    public Kind getKind() {
        return Kind.ATTRIBUTE;
    }

    @Override
    public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
        return visitor.visitAttribute(this, data);
    }

}
