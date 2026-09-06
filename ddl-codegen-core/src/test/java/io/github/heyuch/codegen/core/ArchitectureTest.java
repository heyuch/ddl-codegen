package io.github.heyuch.codegen.core;

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
@AnalyzeClasses(packages = "io.github.heyuch.codegen.core", importOptions = ImportOption.DoNotIncludeTests.class)
// ArchUnit 规则字段命名惯例为小驼峰（leafPackages.../lowerLayers...），与 checkstyle 常量规则冲突
@SuppressWarnings("ConstantName")
class ArchitectureTest {

    /** 叶子包（model/config/io）不得依赖任何其他 core 包。 */
    @ArchTest
    static final ArchRule leafPackagesHaveNoInternalDependencies = noClasses()
            .that()
            .resideInAnyPackage(
                    "io.github.heyuch.codegen.core.model..",
                    "io.github.heyuch.codegen.core.config..",
                    "io.github.heyuch.codegen.core.io..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "io.github.heyuch.codegen.core.annotation..",
                    "io.github.heyuch.codegen.core.naming..",
                    "io.github.heyuch.codegen.core.types..",
                    "io.github.heyuch.codegen.core.ddl..",
                    "io.github.heyuch.codegen.core.gen..",
                    "io.github.heyuch.codegen.core.interceptor..");

    /** 低层（annotation/naming/types/ddl）不得依赖生成层（gen/interceptor）。 */
    @ArchTest
    static final ArchRule lowerLayersDoNotReachIntoGeneration = noClasses()
            .that()
            .resideInAnyPackage(
                    "io.github.heyuch.codegen.core.annotation..",
                    "io.github.heyuch.codegen.core.naming..",
                    "io.github.heyuch.codegen.core.types..",
                    "io.github.heyuch.codegen.core.ddl..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "io.github.heyuch.codegen.core.gen..",
                    "io.github.heyuch.codegen.core.interceptor..");

    /** core 包之间无循环依赖。 */
    @ArchTest
    static final ArchRule packagesAreFreeOfCycles = slices()
            .matching("io.github.heyuch.codegen.core.(*)..")
            .should()
            .beFreeOfCycles();

}
