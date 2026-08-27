package hyc.codegen.tree.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import hyc.codegen.tree.Annotation;
import hyc.codegen.tree.JavaCodegen;

public final class U {

    private U() {}

    public static String lowerCamelCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        String[] words = s.split("_");
        StringBuilder sb = new StringBuilder(words[0].toLowerCase(Locale.ROOT));

        for (int i = 1; i < words.length; i++) {
            sb.append(capitalize(words[i]));
        }

        return sb.toString();
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1)
                .toUpperCase(Locale.ROOT) + s.substring(1);
    }

    public static String upperCamelCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        String[] words = s.split("_");
        StringBuilder sb = new StringBuilder(capitalize(words[0]));

        for (int i = 1; i < words.length; i++) {
            sb.append(capitalize(words[i]));
        }

        return sb.toString();
    }

    public static Annotation generated(Class<?> generator, @Nullable String comment) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("value", generator.getName());
        args.put("date", now());
        if (comment != null) {
            args.put("comments", comment);
        }

        return Annotation.of("javax.annotation.processing.Generated");
    }

    public static boolean generatedMarked(ModifiersTree mod) {
        List<? extends AnnotationTree> annotations = mod.getAnnotations();
        if (annotations.isEmpty()) {
            return false;
        }

        for (AnnotationTree a : annotations) {
            String code = JavaCodegen.generateCode(a.getAnnotationType());
            if ("Generated".equals(code) || "javax.annotation.Generated".equals(code)) {
                return true;
            }
        }

        return false;
    }

    public static String today() {
        return LocalDate.now()
                .format(DateTimeFormatter.ISO_DATE);
    }

    public static String now() {
        return LocalDateTime.now()
                .withNano(0)
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static Annotation nullable() {
        return Annotation.of("javax.annotation.Nullable");
    }

}
