package com.babyduck;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.util.List;

/**
 * Punto de entrada del compilador BabyDuck.
 *
 * Uso:
 *   java com.babyduck.Main programa.bd            analiza léxico + sintaxis + semántica
 *   java com.babyduck.Main programa.bd --tokens   además imprime la lista de tokens
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Uso: Main <archivo.bd> [--tokens]");
            System.exit(2);
        }
        String path = args[0];
        boolean showTokens = args.length > 1 && args[1].equals("--tokens");

        if (showTokens) {
            printTokens(path);
        }

        BabyDuckAnalyzer.Result result = BabyDuckAnalyzer.compile(CharStreams.fromFileName(path));

        if (result.isValid()) {
            System.out.println("Programa válido: " + path);
            System.out.println(result.tree().toStringTree(result.parser()));
            System.out.println();
            System.out.println("=== Directorio de funciones ===");
            System.out.println(result.directory());
        } else {
            System.out.println("Se encontraron " + result.errors().size() + " error(es) en " + path + ":");
            result.errors().forEach(e -> System.out.println("  " + e));
            System.exit(1);
        }
    }

    /** Imprime cada token como: línea:columna  TIPO  'texto'. */
    private static void printTokens(String path) throws IOException {
        ErrorCollector lexErrors = new ErrorCollector();
        List<? extends Token> tokens = BabyDuckAnalyzer.tokenize(CharStreams.fromFileName(path), lexErrors);

        System.out.println("=== Tokens ===");
        for (Token t : tokens) {
            System.out.printf("%3d:%-3d  %-10s '%s'%n",
                    t.getLine(), t.getCharPositionInLine(),
                    BabyDuckAnalyzer.tokenName(t), t.getText());
        }
        System.out.println();
    }
}
