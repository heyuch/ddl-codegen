package io.github.heyuch.codegen.core.gen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import io.github.heyuch.codegen.core.io.ChangeStatus;
import io.github.heyuch.codegen.core.io.FileWriter;
import io.github.heyuch.codegen.core.io.PathResolver;
import io.github.heyuch.codegen.core.model.Column;
import io.github.heyuch.codegen.tree.Class;
import io.github.heyuch.codegen.tree.CompileUnit;
import io.github.heyuch.codegen.tree.Import;
import io.github.heyuch.codegen.tree.JavaCodegen;
import io.github.heyuch.codegen.tree.JavaParser;
import io.github.heyuch.codegen.tree.Method;
import io.github.heyuch.codegen.tree.Package;
import io.github.heyuch.codegen.tree.Variable;
import io.github.heyuch.codegen.tree.VariableKind;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Java artifact 生成器基类：定位 / 解析 / @Generated 成员级 reconcile / 拦截器 / 打印 / 写盘 的完整管线。
 * <p>
 * 子类只需实现 {@link #buildClass}（从模型构建期望成员）与 {@link #kind}，并可覆盖
 * {@link #shouldGenerate}（如 enum 文件仅在存在 enum 列时生成）。
 * <p>
 * reconcile 语义（DESIGN §1/§4）：模型有而文件无 → 增；有而模型无 → 删；类型/签名变 → 替换；一致 → 跳过。
 * 只动 {@code @Generated} 成员，用户手写成员永不触碰；解析失败 → 记警告跳过（不覆盖）。
 */
// 生成器基类聚合 reconcile 所需的签名类型引用（ExpressionTree/VariableKind + 双日志），fanout 21/20；
// 类内职责内聚，豁免优于改动静态检查配置
@SuppressWarnings("ClassFanOutComplexity")
public abstract class AbstractJavaGenerator implements Generator {

    private static final System.Logger LOG = System.getLogger(AbstractJavaGenerator.class.getName());

    private final JavaParser parser = new JavaParser();

    /** 从模型构建期望成员（所有成员会由基类自动打上 {@code @Generated}）。 */
    protected abstract void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx);

    /** 构建期望模型并标记全部成员。 */
    private Class buildFresh(TableContext ctx, String className,
            java.util.function.Consumer<Class.Builder> builderFn) {
        Class.Builder builder = Class.builder()
                .name(className)
                .pkg(ctx.packageName());
        builder.modifiers(finalClass()
                ? new Modifier[] {Modifier.PUBLIC, Modifier.FINAL}
                : new Modifier[] {Modifier.PUBLIC});
        builderFn.accept(builder);
        Class fresh = builder.build();
        markGenerated(fresh);
        return fresh;
    }

    /** 类名（框架默认：命名策略 + 产物后缀；特殊命名逻辑可覆盖）。 */
    @Override
    public String className(TableContext ctx) {
        return ctx.getNaming().artifactClassName(ctx.getTable().getName(), ctx.getArtifactName());
    }

    /** 不再需要该文件时删除（drop 的删除由 CodeGenerator 处理，此处处理 shouldGenerate=false 的清理）。 */
    protected void deleteIfExists(TableContext ctx, GenerationContext gctx) {
        try {
            if (FileWriter.deleteIfExists(ctx.javaFile(gctx.getProjectRoot()))) {
                gctx.getReport()
                        .add(ctx.javaFile(gctx.getProjectRoot()), ChangeStatus.DELETED,
                                ctx.getArtifactName() + " " + ctx.className() + "（不再适用）");
            }
        } catch (IOException e) {
            throw new IllegalStateException("删除文件失败: " + ctx.javaFile(gctx.getProjectRoot()), e);
        }
    }

    /**
     * 额外 import（方法体字符串引用的类型不会自动收集，由子类在此显式登记，见 docs/progress.md 决策）。
     */
    protected List<Import> extraImports(TableContext ctx, GenerationContext gctx) {
        return java.util.Collections.emptyList();
    }

    /** 字段名（框架默认：命名策略；特殊逻辑可覆盖）。 */
    @Override
    public String fieldName(Column column, TableContext ctx) {
        return ctx.getNaming().columnFieldName(column.getName());
    }

    /** 成员类型（框架默认：SQL 类型映射；特性/特殊类型逻辑在生成器内覆盖）。 */
    @Override
    public String fieldType(Column column, TableContext ctx) {
        return ctx.getTypeMapper().resolveType(ctx.getTable().getName(), column);
    }

    /** 生成类是否为 final（leaf 实现类，如 repositoryImpl/converter；接口/实体/枚举保持非 final）。 */
    protected boolean finalClass() {
        return false;
    }

    private @Nullable Variable findField(List<Variable> fields, String name) {
        for (Variable field : fields) {
            if (name.equals(field.getName().toString())) {
                return field;
            }
        }
        return null;
    }

    private @Nullable Method findMethod(List<Method> methods, String name) {
        for (Method method : methods) {
            if (name.equals(method.getName().toString())) {
                return method;
            }
        }
        return null;
    }

    /** 生成/更新该表该 artifact 的 Java 文件。 */
    @Override
    public void generate(TableContext ctx, GenerationContext gctx) {
        if (!shouldGenerate(ctx)) {
            deleteIfExists(ctx, gctx);
            return;
        }
        generateClass(ctx, gctx, ctx.className(), builder -> buildClass(builder, ctx, gctx));
    }

    /**
     * 生成单个类文件（支持一表多文件的 artifact，如 enum 类按列生成多个文件）。
     *
     * @param ctx       表上下文
     * @param gctx      全局上下文
     * @param className 目标类名（决定文件路径与 reconcile 的类名匹配）
     * @param builderFn 用给定 builder 构建该类成员
     */
    protected void generateClass(TableContext ctx, GenerationContext gctx, String className,
            java.util.function.Consumer<Class.Builder> builderFn) {
        Class fresh = buildFresh(ctx, className, builderFn);
        File file = PathResolver.javaFile(gctx.getProjectRoot(),
                ctx.getArtifactConfig().getModule(), ctx.packageName(), className).toFile();

        CompileUnit existingCu = parse(file);
        Class target;
        CompileUnit cu;
        if (existingCu == null) {
            target = fresh;
            cu = new CompileUnit();
        } else {
            Class existingClass = existingCu.getClass(className);
            if (existingClass == null) {
                // 契约：以 config 为准，文件里没有期望类名 → 视为新建（用户迁移代码未改 config 的后果自负）
                target = fresh;
            } else {
                reconcile(existingClass, fresh);
                target = existingClass;
            }
            // 复用原 CU：保留用户 import；包与类以 config 为准（addClass 会替换同名类）
            existingCu.setPackage(Package.of(ctx.packageName()));
            target.setPkg(Package.of(ctx.packageName()));
            cu = existingCu;
        }

        cu.addClass(target);
        for (Import imp : extraImports(ctx, gctx)) {
            cu.addImport(imp);
        }
        String code = JavaCodegen.generateCode(cu);
        writeFile(file.toPath(), ctx, gctx, code, className);
    }

    private List<Variable> generatedFields(Class cls) {
        List<Variable> generated = new ArrayList<>();
        for (Variable field : cls.getFields()) {
            if (GeneratedSupport.isGenerated(field)) {
                generated.add(field);
            }
        }
        return generated;
    }

    private List<Method> generatedMethods(Class cls) {
        List<Method> generated = new ArrayList<>();
        for (Method method : cls.getMethods()) {
            if (GeneratedSupport.isGenerated(method)) {
                generated.add(method);
            }
        }
        return generated;
    }

    /** artifact 类型名（对应 config {@code generator=<名>}）。 */
    @Override
    public abstract String kind();

    /** 给期望模型的全部成员打上生成标记。 */
    private void markGenerated(Class fresh) {
        for (Variable f : fresh.getFields()) {
            GeneratedSupport.mark(f);
        }
        for (Method m : fresh.getMethods()) {
            GeneratedSupport.mark(m);
        }
    }

    /** 解析现有文件；不存在或解析失败返回 null（解析失败已记警告）。 */
    private @Nullable CompileUnit parse(File file) {
        if (!file.isFile()) {
            return null;
        }
        try {
            List<CompileUnit> units = parser.parse(file);
            return units.isEmpty() ? null : units.get(0);
        } catch (Exception e) {
            // 契约：解析失败不碰文件，报告错误；--force 由 CLI 层处理
            throw new IllegalStateException("解析失败: " + file + "（" + e.getMessage() + "），未修改该文件", e);
        }
    }

    /** 成员级 reconcile：把现有类的 {@code @Generated} 成员对齐到期望模型。 */
    private void reconcile(Class target, Class fresh) {
        reconcileFields(target, fresh.getFields(), generatedFields(target));
        reconcileMethods(target, fresh.getMethods(), generatedMethods(target));
    }

    private void reconcileFields(Class target, List<Variable> expected, List<Variable> generated) {
        for (Variable exp : expected) {
            Variable match = findField(generated, exp.getName().toString());
            if (match == null) {
                // 既有同名非 @Generated 成员（用户手写常量/字段）→ 保留用户版，跳过新增防重名
                if (findField(target.getFields(), exp.getName().toString()) != null) {
                    LOG.log(System.Logger.Level.WARNING,
                            "跳过新增 @Generated 字段（存在同名用户手写成员，保留用户版本）: {0} {1}",
                            target.getSimpleName(), exp.getName());
                    continue;
                }
                target.addField(exp);
                continue;
            }
            generated.remove(match);
            replaceIfSignatureChanged(target, exp, match);
        }
        for (Variable stale : generated) {
            target.removeField(stale);
        }
    }

    private void reconcileMethods(Class target, List<Method> expected, List<Method> generated) {
        for (Method exp : expected) {
            Method match = findMethod(generated, exp.getName().toString());
            if (match == null) {
                // 既有同名非 @Generated 方法（用户手写）→ 保留用户版，跳过新增防重名
                if (findMethod(target.getMethods(), exp.getName().toString()) != null) {
                    LOG.log(System.Logger.Level.WARNING,
                            "跳过新增 @Generated 方法（存在同名用户手写方法，保留用户版本）: {0} {1}",
                            target.getSimpleName(), exp.getName());
                    continue;
                }
                target.addMethod(exp);
                continue;
            }
            generated.remove(match);
            replaceIfSignatureChanged(target, exp, match);
        }
        for (Method stale : generated) {
            target.removeMethod(stale);
        }
    }

    private void replaceIfSignatureChanged(Class target, Method expected, Method match) {
        if (signature(expected).equals(signature(match))) {
            return;
        }
        target.removeMethod(match);
        target.addMethod(expected);
    }

    private void replaceIfSignatureChanged(Class target, Variable expected, Variable match) {
        if (signature(expected).equals(signature(match))) {
            return;
        }
        if (expected.getVariableKind() == VariableKind.ENUM_CONSTANT) {
            // 枚举常量签名含 init：code/desc 值改 → 原位替换，保持声明顺序（防 ordinal/values() 漂移）
            if (!target.replaceField(match, expected)) {
                target.removeField(match);
                target.addField(expected);
            }
            return;
        }
        target.removeField(match);
        target.addField(expected);
    }

    /** 是否应该生成此文件；返回 false 时若文件已存在则删除（如 enum 文件随 enum 列消失而清理）。 */
    protected boolean shouldGenerate(TableContext ctx) {
        return true;
    }

    /** 方法签名：返回类型 + 参数类型序列 + 方法体（空白归一化，体变更也能触发替换）+ 方法注解集。 */
    private String signature(Method method) {
        StringBuilder sb = new StringBuilder();
        com.sun.source.tree.Tree returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(returnType);
        }
        sb.append('(');
        for (VariableTree p : method.getParameters()) {
            sb.append(p.getType()).append(',');
        }
        sb.append(')');
        // 注解纳入签名（20260906-13）：开/关 springCache 只增删注解、方法体不变时也要触发替换。
        // 用 JavaCodegen.generateCode 渲染 = 与打印同源（FQN/简单名归一一致），两侧一致则零 churn。
        com.sun.source.tree.ModifiersTree mods = method.getModifiers();
        if (mods != null) {
            for (AnnotationTree ann : mods.getAnnotations()) {
                sb.append('\n').append(JavaCodegen.generateCode(ann));
            }
        }
        com.sun.source.tree.BlockTree body = method.getBody();
        if (body != null) {
            sb.append(':')
                    .append(body.toString().replaceAll("\\s+", " "));
        }
        return sb.toString();
    }

    /**
     * 字段签名：普通字段 = 类型；枚举常量 = 类型 + init 规范化文本
     * （code/desc 值变化触发替换，见 {@link JavaCodegen#enumConstantInitText}）。
     */
    private String signature(Variable field) {
        String type = String.valueOf(field.getType());
        if (field.getVariableKind() != VariableKind.ENUM_CONSTANT) {
            return type;
        }
        ExpressionTree init = field.getInitializer();
        return type + ":" + (init == null ? "" : JavaCodegen.enumConstantInitText(init));
    }

    private void writeFile(Path file, TableContext ctx, GenerationContext gctx, String code, String className) {
        try {
            ChangeStatus status = FileWriter.writeIfChanged(file, code);
            gctx.getReport().add(file, status, ctx.getArtifactName() + " " + className);
        } catch (IOException e) {
            throw new IllegalStateException("写文件失败: " + file, e);
        }
    }

}
