package com.babyduck;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas semánticas sobre archivos .bd (mismo esquema que ParserTest).
 *
 * valid: el programa completo (léxico + sintaxis + semántica) no debe tener errores
 * invalid: debe haber exactamente un error, de tipo SEMANTICO, y su mensaje debe contener el texto de la línea
 *   {@code // esperado: ...} del archivo
 *
 */
class SemanticTest {

    private static final Path BASE = Path.of("src/test/resources/semantic");
    private static final String EXPECTED_PREFIX = "// esperado:";

    @TestFactory
    Stream<DynamicTest> validPrograms() throws IOException {
        return files("valid").stream().map(path -> DynamicTest.dynamicTest(name(path), () -> {
            BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.compile(CharStreams.fromPath(path));
            assertTrue(result.isValid(), () -> "Errores inesperados: " + result.errors());
        }));
    }

    @TestFactory
    Stream<DynamicTest> invalidPrograms() throws IOException {
        return files("invalid").stream().map(path -> DynamicTest.dynamicTest(name(path), () -> {
            BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.compile(CharStreams.fromPath(path));
            List<ErrorCollector.CompileError> errors = result.errors();

            assertEquals(1, errors.size(), () -> "Se esperaba exactamente un error: " + errors);
            ErrorCollector.CompileError error = errors.get(0);
            assertEquals(ErrorCollector.Kind.SEMANTICO, error.kind(), () -> "Error obtenido: " + error);

            String expected = expectedMessage(path);
            assertNotNull(expected, "Falta la línea '" + EXPECTED_PREFIX + " ...' en el archivo");
            assertTrue(error.message().contains(expected),
                    () -> "Mensaje esperado: '" + expected + "', obtenido: '" + error.message() + "'");
        }));
    }

    private static List<Path> files(String folder) throws IOException {
        try (Stream<Path> stream = Files.list(BASE.resolve(folder))) {
            return stream.filter(p -> p.toString().endsWith(".bd")).sorted().toList();
        }
    }

    private static String name(Path path) {
        return path.getFileName().toString();
    }

    /** Texto después de "// esperado:" en el archivo, o null si no existe. */
    private static String expectedMessage(Path path) throws IOException {
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.strip();
            if (trimmed.startsWith(EXPECTED_PREFIX)) {
                return trimmed.substring(EXPECTED_PREFIX.length()).strip();
            }
        }
        return null;
    }
}