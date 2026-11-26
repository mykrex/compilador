package com.babyduck.vm;

import com.babyduck.BabyDuckAnalyzer;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de ejecución sobre archivos .bd: se compila el programa y la máquina virtual lo ejecuta
 *
 * Bloques en los comentarios del archivo (cada línea del bloque empieza con {@code //}; el
 * bloque termina en la primera línea que no es comentario):
 *   {@code // entrada:}  una línea por dato que lee READ (opcional)
 *   {@code // salida:}   lo que debe imprimir el programa, línea por línea ({@code //} sola es una
 *                        línea vacía). Se ignoran los espacios al final de cada línea
 *
 * valid: el programa compila, termina sin error y su salida es igual al bloque de salida
 * invalid: el programa compila y se detiene con el error de la línea {@code // esperado: ...}
 *   (mensaje completo, con el número de cuádruplo)
 */
class VirtualMachineTest {

    private static final Path BASE = Path.of("src/test/resources/vm");
    private static final String INPUT_MARK = "// entrada:";
    private static final String OUTPUT_MARK = "// salida:";
    private static final String EXPECTED_PREFIX = "// esperado:";

    @TestFactory
    Stream<DynamicTest> validPrograms() throws IOException {
        return files("valid").stream().map(path -> DynamicTest.dynamicTest(name(path), () -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            machine(path, bytes).run();

            List<String> expected = block(path, OUTPUT_MARK);
            assertFalse(expected.isEmpty(), "Falta el bloque '" + OUTPUT_MARK + "' en el archivo");
            assertEquals(String.join("\n", expected), String.join("\n", lines(bytes)));
        }));
    }

    @TestFactory
    Stream<DynamicTest> invalidPrograms() throws IOException {
        return files("invalid").stream().map(path -> DynamicTest.dynamicTest(name(path), () -> {
            String expected = expectedError(path);
            assertNotNull(expected, "Falta la línea '" + EXPECTED_PREFIX + " ...' en el archivo");
            VirtualMachine vm = machine(path, new ByteArrayOutputStream());
            RuntimeError error = assertThrows(RuntimeError.class, vm::run);
            assertEquals(expected, error.getMessage());
        }));
    }

    /** Compila el programa (debe ser válido) y crea la máquina con la entrada del archivo */
    private static VirtualMachine machine(Path path, ByteArrayOutputStream bytes) throws IOException {
        BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.compile(CharStreams.fromPath(path));
        assertTrue(result.isValid(), () -> "Errores de compilación inesperados: " + result.errors());
        String input = String.join("\n", block(path, INPUT_MARK)) + "\n";
        return new VirtualMachine(result.quadruples(), result.constants(), result.directory(),
                new BufferedReader(new StringReader(input)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8));
    }

    /** Salida impresa, línea por línea, sin espacios al final */
    private static List<String> lines(ByteArrayOutputStream bytes) {
        return bytes.toString(StandardCharsets.UTF_8).lines().map(String::stripTrailing).toList();
    }

    private static List<Path> files(String folder) throws IOException {
        try (Stream<Path> stream = Files.list(BASE.resolve(folder))) {
            return stream.filter(p -> p.toString().endsWith(".bd")).sorted().toList();
        }
    }

    private static String name(Path path) {
        return path.getFileName().toString();
    }

    /** Líneas del bloque que sigue a la marca, sin el "// " inicial; vacía si no está */
    private static List<String> block(Path path, String mark) throws IOException {
        List<String> lines = new ArrayList<>();
        boolean inBlock = false;
        for (String line : Files.readAllLines(path)) {
            if (line.strip().equals(mark)) {
                inBlock = true;
            } else if (inBlock && line.startsWith("//")) {
                String content = line.substring(2);
                lines.add((content.startsWith(" ") ? content.substring(1) : content).stripTrailing());
            } else if (inBlock) {
                break;
            }
        }
        return lines;
    }

    private static String expectedError(Path path) throws IOException {
        for (String line : Files.readAllLines(path)) {
            if (line.strip().startsWith(EXPECTED_PREFIX)) {
                return line.strip().substring(EXPECTED_PREFIX.length()).strip();
            }
        }
        return null;
    }
}
