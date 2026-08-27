package hyc.codegen.tree.utils;

import java.io.IOException;
import java.io.Writer;

public final class CodePrinter {

    static final String SEP = System.lineSeparator();

    private final Writer out;

    private final int indentWidth;

    private int level;

    private boolean newline;

    public CodePrinter(Writer out) {
        this(out, 4);
    }

    public CodePrinter(Writer out, int indentWidth) {
        this.out = out;
        this.indentWidth = indentWidth;
    }

    public void indent() {
        level++;
    }

    public void undent() {
        level--;
    }

    public int getIndents() {
        return indentWidth * level;
    }

    public void printSpace(int width) {
        for (int i = 0; i < width; i++) {
            try {
                out.write(' ');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void printRaw(Object... s) {
        print0(false, s);
    }

    private void print0(boolean align, Object... s) {
        try {
            for (Object o : s) {
                if (newline) {
                    if (align) {
                        align();
                    }
                    newline = false;
                }
                if (o != null) {
                    out.write(o.toString());
                }
                if (SEP.equals(o)) {
                    newline = false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void align() {
        try {
            for (int i = 0, spaces = level * indentWidth; i < spaces; i++) {
                out.write(' ');
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void print(Object... s) {
        print0(true, s);
    }

    public void printlnRaw(Object... s) {
        println0(false, s);
    }

    private void println0(boolean align, Object... s) {
        try {
            if (newline && s.length > 0) {
                if (align) {
                    align();
                }
                newline = false;
            }

            for (Object o : s) {
                if (o != null) {
                    out.write(o.toString());
                }
            }

            out.write(SEP);
            newline = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void println(Object... s) {
        println0(true, s);
    }

    public void printf(String f, Object... args) {
        String s = String.format(f, args);
        print0(true, s);
    }

    public void printfln(String f, Object... args) {
        String s = String.format(f, args);
        println0(true, s);
    }

    public void stmt(Object... s) {
        try {
            align();
            newline = false;

            for (Object o : s) {
                if (o != null) {
                    out.write(o.toString());
                }
            }

            out.write(';');
            out.write(SEP);
            newline = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
