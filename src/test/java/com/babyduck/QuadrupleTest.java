package com.babyduck;

import com.babyduck.intermediate.Note;
import com.babyduck.intermediate.QuadrupleQueue;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la fila de cuádruplos sobre archivos .bd (mismo esquema que SemanticTest)
 *
 * valid: el programa no tiene errores y su fila, en estilo SYMBOLIC, es igual al bloque
 *   {@code // cuadruplos:} del archivo (se ignoran los espacios repetidos). Se compara el
 *   texto impreso, nunca objetos con equals. SYMBOLIC no cambiará con la memoria virtual
 * invalid: cada línea {@code // esperado: ...} es un error semántico, en orden, y la fila es null
 * Q06 además compara las notas con su bloque {@code // nota: posición texto}
 */
class QuadrupleTest {

    private static final Path BASE = Path.of("src/test/resources/quads");
    private static final String QUADS_MARK = "// cuadruplos:";
    private static final String NOTE_PREFIX = "// nota:";
    private static final String EXPECTED_PREFIX = "// esperado:";

    @TestFactory
    Stream<DynamicTest> validPrograms() throws IOException {
        return files("valid").stream().map(path -> DynamicTest.dynamicTest(name(path), () -> {
            BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.compile(CharStreams.fromPath(path));
            assertTrue(result.isValid(), () -> "Errores inesperados: " + result.errors());

            List<String> expected = expectedQuadruples(path);
            assertFalse(expected.isEmpty(), "Falta el bloque '" + QUADS_MARK + "' en el archivo");
            List<String> actual = normalize(List.of(
                    result.quadruples().format(QuadrupleQueue.Style.SYMBOLIC).split("\n")));
            assertEquals(String.join("\n", expected), String.join("\n", actual));
        }));
    }

    /** Las notas de lo que se generará en entregas siguientes existen y están en su posición */
    @Test
    void notasDeQ06() throws IOException {
        Path path = BASE.resolve("valid/Q06_estatutos_de_entregas_siguientes.bd");
        BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.compile(CharStreams.fromPath(path));
        assertTrue(result.isValid(), () -> "Errores inesperados: " + result.errors());

        List<String> expected = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line.strip().startsWith(NOTE_PREFIX)) {
                expected.add(line.strip().substring(NOTE_PREFIX.length()).strip());
            }
        }
        assertFalse(expected.isEmpty(), "Faltan las líneas '" + NOTE_PREFIX + "' en Q06");
        List<String> actual = new ArrayList<>();
        for (Note note : result.quadruples().notes()) {
            actual.add(note.position() + " " + note.text());
        }
        assertEquals(String.join("\n", expected), String.join("\n", actual));
    }

    @TestFactory
    Stream<DynamicTest> invalidPrograms() throws IOException {
        return files("invalid").stream().map(path -> DynamicTest.dynamicTest(name(path), () -> {
            BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.compile(CharStreams.fromPath(path));
            List<ErrorCollector.CompileError> errors = result.errors();
            List<String> expected = linesWithPrefix(path, EXPECTED_PREFIX);

            assertFalse(expected.isEmpty(), "Falta la línea '" + EXPECTED_PREFIX + " ...' en el archivo");
            assertEquals(expected.size(), errors.size(), () -> "Errores obtenidos: " + errors);
            for (int i = 0; i < expected.size(); i++) {
                ErrorCollector.CompileError error = errors.get(i);
                String message = expected.get(i);
                assertEquals(ErrorCollector.Kind.SEMANTICO, error.kind(), () -> "Error obtenido: " + error);
                assertTrue(error.message().contains(message),
                        () -> "Mensaje esperado: '" + message + "', obtenido: '" + error.message() + "'");
            }
            assertNull(result.quadruples());                             // con errores la fila se descarta
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

    /** Líneas del bloque "// cuadruplos:": cada una empieza con su número de cuádruplo */
    private static List<String> expectedQuadruples(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        boolean inBlock = false;
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.strip();
            if (trimmed.equals(QUADS_MARK)) {
                inBlock = true;
            } else if (inBlock && trimmed.matches("//\\s*\\d+\\s.*")) {
                lines.add(trimmed.substring(2));
            } else if (inBlock) {
                break;
            }
        }
        return normalize(lines);
    }

    private static List<String> linesWithPrefix(Path path, String prefix) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line.strip().startsWith(prefix)) {
                lines.add(line.strip().substring(prefix.length()).strip());
            }
        }
        return lines;
    }

    /** Quita espacios al inicio y al final, y reduce los espacios repetidos a uno */
    private static List<String> normalize(List<String> lines) {
        return lines.stream().map(l -> l.strip().replaceAll("\\s+", " ")).filter(l -> !l.isEmpty()).toList();
    }
}
