package ru.nsu.crossfitbuddy.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "ru.nsu.crossfitbuddy", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleArchitectureTest {
    @ArchTest
    static final ArchRule moduleDependencies = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Core").definedBy("..core..")
            .layer("Infrastructure").definedBy("..config..")
            .whereLayer("Core").mayOnlyBeAccessedByLayers("Infrastructure");
}
