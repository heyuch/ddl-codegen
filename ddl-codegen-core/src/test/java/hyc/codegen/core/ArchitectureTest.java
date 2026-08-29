package hyc.codegen.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 架构强制规则（设计见 docs/changes/2026-08-29-chore-archunit-rules/design.md）。
 * <p>
 * 分层矩阵以 jdeps 字节码级实证为准：叶子（model/config/io）无内部依赖；
 * 低层（annotation/naming/types/ddl）只向下；gen 是顶层。模块方向由 Maven 自身强制，这里只约束包级依赖与循环。
 */
@AnalyzeClasses(packages = "hyc.codegen.core", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** 叶子包（model/config/io）不得依赖任何其他 core 包。 */
    @ArchTest
    static final ArchRule leafPackagesHaveNoInternalDependencies = noClasses()
            .that()
            .resideInAnyPackage(
                    "hyc.codegen.core.model..",
                    "hyc.codegen.core.config..",
                    "hyc.codegen.core.io..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "hyc.codegen.core.annotation..",
                    "hyc.codegen.core.naming..",
                    "hyc.codegen.core.types..",
                    "hyc.codegen.core.ddl..",
                    "hyc.codegen.core.gen..",
                    "hyc.codegen.core.interceptor..");

    /** 低层（annotation/naming/types/ddl）不得依赖生成层（gen/interceptor）。 */
    @ArchTest
    static final ArchRule lowerLayersDoNotReachIntoGeneration = noClasses()
            .that()
            .resideInAnyPackage(
                    "hyc.codegen.core.annotation..",
                    "hyc.codegen.core.naming..",
                    "hyc.codegen.core.types..",
                    "hyc.codegen.core.ddl..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "hyc.codegen.core.gen..",
                    "hyc.codegen.core.interceptor..");

    /** core 包之间无循环依赖。 */
    @ArchTest
    static final ArchRule packagesAreFreeOfCycles = slices()
            .matching("hyc.codegen.core.(*)..")
            .should()
            .beFreeOfCycles();

}
