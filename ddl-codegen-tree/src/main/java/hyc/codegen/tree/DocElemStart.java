package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Name;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.StartElementTree;

// KeyFor（Map key）子检查对 JDK 泛型通配符（List<? extends DocTree> 的 capture）推断缺陷，
// 与 Map 无关的误报；仅本类抑制，其余代码 KeyFor 检查保留。
@SuppressWarnings("keyfor")
public final class DocElemStart implements StartElementTree {

    private String name;
    private boolean selfClosing;
    private List<? extends DocTree> attrs;

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
