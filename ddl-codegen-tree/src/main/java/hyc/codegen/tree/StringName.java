package hyc.codegen.tree;

import javax.lang.model.element.Name;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class StringName implements Name {

    private String name;

    public StringName(String name) {
        this.name = name;
    }

    @Override
    public char charAt(int index) {
        return name.charAt(index);
    }

    @Override
    public boolean contentEquals(CharSequence cs) {
        return name.equals(cs.toString());
    }

    @Override
    public int length() {
        return name.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }

    @Override
    public String toString() {
        return name;
    }

}
