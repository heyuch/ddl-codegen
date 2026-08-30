package hyc.codegen.tree;

import javax.lang.model.element.Name;

import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(callSuper = true)
public final class TypeReference extends Identifier {

    private @Nullable Package pkg;

    private String className;

    public TypeReference(String qname) {
        super(qname);

        int i = qname.lastIndexOf('.');
        if (i != -1) {
            this.pkg = Package.of(qname.substring(0, i));
            this.className = qname.substring(i + 1);
        } else {
            this.pkg = null;
            this.className = qname;
        }
    }

    public TypeReference(String pkg, String name) {
        super(pkg + "." + name);
        this.pkg = Package.of(pkg);
        this.className = name;
    }

    public TypeReference(Package pkg, String name) {
        super(pkg == null ? name : pkg.getPath() + "." + name);
        this.pkg = pkg;
        this.className = name;
    }

    @Override
    public Name getName() {
        return new StringName(className);
    }

    /**
     * 返回所属包，简单名类型时为 {@code null}。
     */
    public @Nullable Package getPkg() {
        return pkg;
    }

    public Import getImport() {
        return new Import(this);
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }

    public String getQualifiedName() {
        StringBuilder sb = new StringBuilder();

        Package p = pkg;
        if (p != null && !p.getPath().isEmpty()) {
            sb.append(p.getPath())
                    .append(".");
        }
        sb.append(className);

        return sb.toString();
    }

    public @Nullable String getPkgString() {
        if (pkg == null) {
            return null;
        }
        return pkg.getPath();
    }

}
