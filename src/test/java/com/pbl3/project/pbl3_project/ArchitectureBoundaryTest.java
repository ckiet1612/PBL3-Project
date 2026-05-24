package com.pbl3.project.pbl3_project;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {

    @Test
    void domainServicesMustNotDependOnUiContext() throws Exception {
        Path serviceRoot = Path.of("src/main/java/com/pbl3/project/pbl3_project/service");
        try (var paths = Files.walk(serviceRoot)) {
            List<Path> offenders = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(this::containsUiDependency)
                .toList();

            assertTrue(offenders.isEmpty(), "Service classes must not import UI context: " + offenders);
        }
    }

    @Test
    void nonUiFeatureModulesMustNotDependOnJavaFxUi() throws Exception {
        Path featureRoot = Path.of("src/main/java/com/pbl3/project/pbl3_project/feature");
        if (!Files.exists(featureRoot)) {
            return;
        }
        try (var paths = Files.walk(featureRoot)) {
            List<Path> offenders = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().contains("/ui/"))
                .filter(this::containsJavaFxDependency)
                .toList();

            assertTrue(offenders.isEmpty(), "Non-UI feature modules must stay UI-framework independent: " + offenders);
        }
    }

    private boolean containsUiDependency(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("com.pbl3.project.pbl3_project.ui.")
                || source.contains("SceneRuntimeContext");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read " + path, ex);
        }
    }

    private boolean containsJavaFxDependency(Path path) {
        try {
            return Files.readString(path).contains("javafx.");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read " + path, ex);
        }
    }
}
