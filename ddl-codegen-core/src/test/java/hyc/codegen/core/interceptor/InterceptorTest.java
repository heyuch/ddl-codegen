package hyc.codegen.core.interceptor;

import javax.lang.model.element.Modifier;

import hyc.codegen.core.annotation.AnnotationRegistry;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.gen.GeneratedSupport;
import hyc.codegen.core.gen.GenerationContext;
import hyc.codegen.core.gen.TableContext;
import hyc.codegen.core.model.Table;
import hyc.codegen.core.naming.NamingService;
import hyc.codegen.core.types.TypeMapper;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.JavaCodegen;
import hyc.codegen.tree.Method;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterceptorTest {

    private TableContext context(String kind, String... options) {
        ArtifactConfig artifact = new ArtifactConfig(kind);
        artifact.setPkg("com.test");
        for (int i = 0; i < options.length; i += 2) {
            artifact.putOption(options[i], options[i + 1]);
        }
        DdlConfig config = new DdlConfig();
        config.addArtifact(artifact);
        NamingService naming = new NamingService(config);
        TypeMapper types = new TypeMapper(naming);
        GenerationContext gctx = GenerationContext.builder()
                .config(config)
                .naming(naming)
                .typeMapper(types)
                .annotationRegistry(AnnotationRegistry.builtin())
                .build();
        return gctx.tableContext(new Table("user", null), kind);
    }

    private Class sampleClass() {
        Class.Builder builder = Class.builder().name("User").pkg("com.test");
        builder.field(Variable.builder()
                .modifiers(Modifier.PRIVATE)
                .type(new TypeReference("java.lang.String"))
                .name("name")
                .build());
        builder.method(Method.builder()
                .modifiers(Modifier.PUBLIC)
                .returnType(new TypeReference("java.lang.String"))
                .name("getName")
                .body("return name;")
                .build());
        return builder.build();
    }

    private String print(Class cls) {
        hyc.codegen.tree.CompileUnit cu = new hyc.codegen.tree.CompileUnit();
        cu.addClass(cls);
        return JavaCodegen.generateCode(cu);
    }

    @Test
    void lombokAddsDataAnnotation() {
        Class cls = sampleClass();
        new LombokInterceptor().apply(cls, context("entity"));

        String code = print(cls);
        assertTrue(code.contains("@Data"), code);
        assertTrue(code.contains("import lombok.Data;"), code);
    }

    @Test
    void lombokConfigurableAnnotationList() {
        Class cls = sampleClass();
        new LombokInterceptor().apply(cls, context("entity", "lombok", "Data,Builder"));

        String code = print(cls);
        assertTrue(code.contains("@Data"), code);
        assertTrue(code.contains("@Builder"), code);
    }

    @Test
    void lombokIdempotent() {
        Class cls = sampleClass();
        LombokInterceptor interceptor = new LombokInterceptor();
        TableContext ctx = context("entity");
        interceptor.apply(cls, ctx);
        String first = print(cls);
        interceptor.apply(cls, ctx);
        String second = print(cls);
        assertEquals(first, second);
    }

    @Test
    void jsr303AddsConstraintAnnotations() {
        TableContext ctx = context("entity");
        ctx.getTable()
                .addColumn(hyc.codegen.core.model.Column.builder()
                        .name("name")
                        .sqlType("varchar")
                        .length(50)
                        .nullable(false)
                        .build());
        ctx.getTable()
                .addColumn(hyc.codegen.core.model.Column.builder()
                        .name("price")
                        .sqlType("decimal")
                        .precision(10)
                        .scale(2)
                        .nullable(true)
                        .build());
        ctx.getTable()
                .addColumn(hyc.codegen.core.model.Column.builder()
                        .name("valid")
                        .sqlType("tinyint")
                        .length(1)
                        .nullable(true)
                        .build());

        Class cls = Class.builder()
                .name("User")
                .pkg("com.test")
                .field(hyc.codegen.tree.Variable.builder()
                        .modifiers(Modifier.PRIVATE)
                        .type(new TypeReference("java.lang.String"))
                        .name("name")
                        .build())
                .build();
        GeneratedSupport.mark(cls.getFields().get(0));

        new Jsr303Interceptor().apply(cls, ctx);
        String code = print(cls);
        assertTrue(code.contains("@NotNull"), code);
        assertTrue(code.contains("@Size(max = 50)"), code);
    }

    @Test
    void jsr303DigitsOnDecimal() {
        TableContext ctx = context("entity");
        ctx.getTable()
                .addColumn(hyc.codegen.core.model.Column.builder()
                        .name("price")
                        .sqlType("decimal")
                        .precision(10)
                        .scale(2)
                        .nullable(true)
                        .build());

        Class cls = Class.builder()
                .name("User")
                .pkg("com.test")
                .field(hyc.codegen.tree.Variable.builder()
                        .modifiers(Modifier.PRIVATE)
                        .type(new TypeReference("java.math.BigDecimal"))
                        .name("price")
                        .build())
                .build();
        GeneratedSupport.mark(cls.getFields().get(0));

        new Jsr303Interceptor().apply(cls, ctx);
        String code = print(cls);
        assertTrue(code.contains("@Digits(integer = 8, fraction = 2)"), code);
    }

}
