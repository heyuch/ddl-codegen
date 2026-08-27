package hyc.codegen.tree;

import javax.annotation.Nullable;
import javax.lang.model.element.Name;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public final class TypeReference extends Identifier {

    @Nullable
    Package pkg;

    String name;

    public TypeReference(String qname) {
        super(qname);

        int i = qname.lastIndexOf('.');
        if (i != -1) {
            this.pkg = Package.of(qname.substring(0, i));
            this.name = qname.substring(i + 1);
        } else {
            this.pkg = null;
            this.name = qname;
        }
    }

    public TypeReference(String pkg, String name) {
        super(name);
        this.pkg = Package.of(pkg);
        this.name = name;
    }

    public TypeReference(Package pkg, String name) {
        super(name);
        this.pkg = pkg;
        this.name = name;
    }

    @Override
    public Name getName() {
        return new StringName(name);
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

        if (pkg != null && pkg.path != null && !pkg.path.isEmpty()) {
            sb.append(pkg.path)
                    .append(".");
        }
        sb.append(name);

        return sb.toString();
    }

    @Nullable
    public String getPkgString() {
        if (pkg == null) {
            return null;
        }
        return pkg.path;
    }

}
