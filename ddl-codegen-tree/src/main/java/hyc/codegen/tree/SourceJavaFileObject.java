package hyc.codegen.tree;

import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import javax.tools.SimpleJavaFileObject;

public final class SourceJavaFileObject extends SimpleJavaFileObject {

    final String code;

    SourceJavaFileObject(String className, String code) {
        super(URI.create(className + ".java"), Kind.SOURCE);
        this.code = code;
    }

    SourceJavaFileObject(URI uri, String code) {
        super(uri, Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return CharBuffer.wrap(code);
    }

}
