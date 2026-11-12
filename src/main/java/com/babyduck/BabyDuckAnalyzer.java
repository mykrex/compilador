package com.babyduck;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import com.babyduck.intermediate.QuadrupleQueue;
import com.babyduck.semantic.FunctionDirectory;

import java.util.List;

/**
 * Punto único para ejecutar el Scanner (lexer) y el Parser
 * Lo usan tanto Main como las pruebas, para que ambos analicen exactamente igual
 */
public final class BabyDuckAnalyzer {

    private BabyDuckAnalyzer() { }

    /**
     * Resultado del análisis: el árbol de parse, la lista de errores, el directorio de
     * funciones y la fila de cuádruplos (null si hubo cualquier error: se descarta)
     */
    public record Result(BabyDuckParser parser, ParseTree tree,
                         List<ErrorCollector.CompileError> errors,
                         FunctionDirectory directory,
                         QuadrupleQueue quadruples) {
        public boolean isValid() {
            return errors.isEmpty();
        }
    }

    /** Ejecuta lexer + parser sobre un programa completo, empezando en la regla 'program' */
    public static Result analyze(CharStream input) {
        ErrorCollector errors = new ErrorCollector();

        BabyDuckLexer lexer = new BabyDuckLexer(input);
        lexer.removeErrorListeners();          // quita el listener que imprime en consola
        lexer.addErrorListener(errors);

        BabyDuckParser parser = new BabyDuckParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errors);

        ParseTree tree = parser.program();      // regla inicial de la gramática
        return new Result(parser, tree, errors.getErrors(), null, null);
    }

    /**
     * Se ejecuta léxico + sintaxis y, si no hubo errores, la semántica se ejecuta
     * Con un árbol incompleto la semántica produciría errores falsos
     * La semántica genera los cuádruplos en la misma pasada; si encontró errores, la fila se descarta
     */
    public static Result compile(CharStream input) {
        Result syntax = analyze(input);
        if (!syntax.isValid()) {
            return syntax;
        }
        ErrorCollector errors = new ErrorCollector();
        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.visit(syntax.tree());
        QuadrupleQueue quadruples = errors.hasErrors() ? null : semantic.quadruples();
        return new Result(syntax.parser(), syntax.tree(), errors.getErrors(), semantic.directory(), quadruples);
    }

    /** Ejecuta solo el lexer y regresa la lista de tokens (sin EOF) */
    public static List<? extends Token> tokenize(CharStream input, ErrorCollector errors) {
        BabyDuckLexer lexer = new BabyDuckLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errors);
        return lexer.getAllTokens();
    }

    /** Nombre simbólico de un token, por ejemplo "ID" o "CTE_INT" */
    public static String tokenName(Token token) {
        return BabyDuckLexer.VOCABULARY.getSymbolicName(token.getType());
    }
}
