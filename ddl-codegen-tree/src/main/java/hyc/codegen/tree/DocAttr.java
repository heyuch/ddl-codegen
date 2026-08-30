package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Name;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;

// KeyFor（Map key）子检查对 JDK 泛型通配符（List<? extends DocTree> 的 capture）推断缺陷，
// 与 Map 无关的误报；仅本类抑制，其余代码 KeyFor 检查保留。
@SuppressWarnings("keyfor")
public final class DocAttr implements AttributeTree {

    private String name;

    private ValueKind valueKind;

    private List<? extends DocTree> value;

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
