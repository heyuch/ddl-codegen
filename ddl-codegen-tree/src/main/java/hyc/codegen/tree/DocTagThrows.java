package hyc.codegen.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ThrowsTree;

// KeyFor（Map key）子检查对 JDK 泛型通配符（List<? extends DocTree> 的 capture）推断缺陷，
// 与 Map 无关的误报；仅本类抑制，其余代码 KeyFor 检查保留。
@SuppressWarnings("keyfor")
public final class DocTagThrows implements ThrowsTree {

    private ReferenceTree name;

    private List<? extends DocTree> descs;

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
