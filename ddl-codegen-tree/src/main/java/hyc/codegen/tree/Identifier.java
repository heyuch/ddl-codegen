package hyc.codegen.tree;

import javax.lang.model.element.Name;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.TreeVisitor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Identifier implements IdentifierTree {

    Name name;

    public Identifier(Name name) {
        this.name = name;
    }

    public Identifier(String name) {
        this(new StringName(name));
    }

    /**
     * 分发到访问器的 {@code visitIdentifier} 方法。
     */
    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitIdentifier(this, data);
    }

    /**
     * 返回节点类型，恒为 {@link Kind#IDENTIFIER}。
     */
    @Override
    public Kind getKind() {
        return Kind.IDENTIFIER;
    }

    /**
     * 返回标识符名称。
     */
    @Override
    public Name getName() {
        return name;
    }

}
