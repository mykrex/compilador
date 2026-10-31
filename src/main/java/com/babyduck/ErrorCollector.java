package com.babyduck;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Recolecta los errores del lexer y del parser en una lista,
 * en lugar de solo imprimirlos en consola (comportamiento por defecto de ANTLR).
 * Así Main puede decidir qué mostrar y las pruebas pueden contar los errores.
 */
public class ErrorCollector extends BaseErrorListener {

    /** Fase del compilador donde ocurrió el error. */
    public enum Kind { LEXICO, SINTACTICO }

    /** Un error con su fase, posición y mensaje. */
    public record CompileError(Kind kind, int line, int column, String message) {
        @Override
        public String toString() {
            String phase = (kind == Kind.LEXICO) ? "léxico" : "sintáctico";
            return String.format("[Error %s] línea %d:%d -> %s", phase, line, column, message);
        }
    }

    private final List<CompileError> errors = new ArrayList<>();

    /** ANTLR llama a este método cada vez que el lexer o el parser encuentran un error. */
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        // Si quien reporta es el lexer, es error léxico; si es el parser, es sintáctico.
        Kind kind = (recognizer instanceof Lexer) ? Kind.LEXICO : Kind.SINTACTICO;
        errors.add(new CompileError(kind, line, charPositionInLine, msg));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<CompileError> getErrors() {
        return errors;
    }
}
