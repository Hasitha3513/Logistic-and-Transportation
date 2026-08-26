package com.transportlogistics.app.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LombokUsageArchitectureTest {

    private static final Path SOURCE_ROOT = Paths.get("src/main/java/com/transportlogistics/app");

    @Test
    void noDataAnnotationOnEntitiesOrDomain() throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("Entity.java") || p.toString().contains("domain"))
                    .filter(this::containsDataAnnotation)
                    .map(Path::toString)
                    .toList();

            assertTrue(violations.isEmpty(),
                    "ADR forbids @Data on JPA entities and domain classes. Violations found in: " + violations);
        }
    }

    @Test
    void noEqualsAndHashCodeOnEntities() throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("Entity.java"))
                    .filter(this::containsEqualsAndHashCodeAnnotation)
                    .map(Path::toString)
                    .toList();

            assertTrue(violations.isEmpty(),
                    "ADR forbids @EqualsAndHashCode on JPA entities. Violations found in: " + violations);
        }
    }

    @Test
    void noClassLevelToStringOnEntities() throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("Entity.java"))
                    .filter(this::containsToStringAnnotation)
                    .map(Path::toString)
                    .toList();

            assertTrue(violations.isEmpty(),
                    "ADR forbids @ToString on JPA entities. Violations found in: " + violations);
        }
    }

    private boolean containsDataAnnotation(Path path) {
        try {
            String content = Files.readString(path);
            return content.contains("@Data") && !content.contains("// @Data");
        } catch (IOException e) {
            return false;
        }
    }

    private boolean containsEqualsAndHashCodeAnnotation(Path path) {
        try {
            String content = Files.readString(path);
            return content.contains("@EqualsAndHashCode");
        } catch (IOException e) {
            return false;
        }
    }

    private boolean containsToStringAnnotation(Path path) {
        try {
            String content = Files.readString(path);
            return content.contains("@ToString");
        } catch (IOException e) {
            return false;
        }
    }
}