package com.babyduck;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del Scanner (lexer): verifican qué tokens se generan
 * para una entrada, o que una entrada inválida produzca error léxico.
 */
class LexerTest {

    // ---------- Casos válidos: entrada -> tokens esperados ----------
    static Stream<Arguments> validCases() {
        return Stream.of(
            Arguments.of("L01 Palabras reservadas",
                "program main end void var print int float while do if else read return",
                "PROGRAM MAIN END VOID VAR PRINT INT FLOAT WHILE DO IF ELSE READ RETURN"),
            Arguments.of("L02 Identificadores válidos",
                "_X_ _Contador_ _A1_b_ _A__",
                "ID ID ID ID"),
            Arguments.of("L03 Constantes enteras y flotantes",
                "0 42 3.14 10.0",
                "CTE_INT CTE_INT CTE_FLOAT CTE_FLOAT"),
            Arguments.of("L04 Operadores (gana el token más largo: == y !=)",
                "= == != < > + - * /",
                "ASSIGN EQ NEQ LT GT PLUS MINUS MULT DIV"),
            Arguments.of("L05 Caracteres especiales",
                "; , : { } ( ) [ ]",
                "SEMI COMMA COLON LBRACE RBRACE LPAREN RPAREN LBRACKET RBRACKET"),
            Arguments.of("L06 Letreros (vacío y con comillas escapadas)",
                "\"hola\" \"\" \"dice \\\"hola\\\"\"",
                "CTE_STRING CTE_STRING CTE_STRING"),
            Arguments.of("L07 Se ignoran espacios y comentarios; no se requieren espacios",
                "_X_=_Y_+1; // comentario\n\t_Y_",
                "ID ASSIGN ID PLUS CTE_INT SEMI ID")
        );
    }

    // ---------- Casos inválidos: deben producir error léxico ----------
    static Stream<Arguments> invalidCases() {
        return Stream.of(
            Arguments.of("L08 Id con minúscula después de '_'", "_x_"),
            Arguments.of("L09 Id sin '_' final", "_X"),
            Arguments.of("L10 Id que empieza con dígito", "_9A_"),
            Arguments.of("L11 Flotante incompleto", "3."),
            Arguments.of("L12 Letrero sin cerrar", "\"hola"),
            Arguments.of("L13 Caracteres no reconocidos", "@ # !")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validCases")
    @DisplayName("Tokens válidos")
    void validTokens(String id, String input, String expected) {
        ErrorCollector errors = new ErrorCollector();
        List<String> actual = BabyDuckAnalyzer.tokenize(CharStreams.fromString(input), errors)
                .stream().map(BabyDuckAnalyzer::tokenName).toList();

        assertFalse(errors.hasErrors(), () -> "No se esperaban errores: " + errors.getErrors());
        assertEquals(List.of(expected.split(" ")), actual);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    @DisplayName("Errores léxicos")
    void invalidTokens(String id, String input) {
        ErrorCollector errors = new ErrorCollector();
        BabyDuckAnalyzer.tokenize(CharStreams.fromString(input), errors);

        assertTrue(errors.hasErrors(), "Se esperaba un error léxico para: " + input);
        assertEquals(ErrorCollector.Kind.LEXICO, errors.getErrors().get(0).kind());
        System.out.println(id + "  ->  " + errors.getErrors().get(0));
    }
}
