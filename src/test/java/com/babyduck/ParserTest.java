package com.babyduck;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del Parser: cada archivo .bd de test/resources/ es un caso.
 *   - valid/   : debe analizarse sin ningún error.
 *   - invalid/ : debe producir al menos un error, y el primero debe ser sintáctico
 *                (así se asegura que la prueba falla por la gramática y no por el lexer).
 */
class ParserTest {

    @TestFactory
    Stream<DynamicTest> validPrograms() throws Exception {
        return casesIn("/valid").map(file -> DynamicTest.dynamicTest(name(file), () -> {
            BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.analyze(CharStreams.fromPath(file));
            assertTrue(result.isValid(), () -> "Errores inesperados: " + result.errors());
        }));
    }

    @TestFactory
    Stream<DynamicTest> invalidPrograms() throws Exception {
        return casesIn("/invalid").map(file -> DynamicTest.dynamicTest(name(file), () -> {
            BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.analyze(CharStreams.fromPath(file));
            assertFalse(result.isValid(), "Se esperaba al menos un error");
            assertEquals(ErrorCollector.Kind.SINTACTICO, result.errors().get(0).kind());
            System.out.println(name(file) + "  ->  " + result.errors().get(0));
        }));
    }

    // ---------- utilidades ----------
    private Stream<Path> casesIn(String folder) throws IOException, URISyntaxException {
        Path dir = Path.of(getClass().getResource(folder).toURI());
        return Files.list(dir).filter(p -> p.toString().endsWith(".bd")).sorted();
    }

    private static String name(Path file) {
        return file.getFileName().toString().replace(".bd", "");
    }
}
