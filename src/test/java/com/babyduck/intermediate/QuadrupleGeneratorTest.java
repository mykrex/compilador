package com.babyduck.intermediate;

import com.babyduck.semantic.Op;
import com.babyduck.semantic.SemanticCube;
import com.babyduck.semantic.Type;
import com.babyduck.semantic.VariableInfo;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las pilas y la fila sin pasar por el parser
 *
 * Las expresiones se simulan con la misma secuencia de llamadas que hace SemanticAnalyzer:
 * después de cada operando se revisa el tope de la pila de operadores
 */
class QuadrupleGeneratorTest {

    private final SemanticCube cube = new SemanticCube();
    private final QuadrupleGenerator gen = new QuadrupleGenerator(cube);

    private Operand var(String name, Type type) {
        return gen.variable(new VariableInfo(name, type));
    }

    /** Lo que hace el analizador al terminar un factor dentro de un término */
    private void afterFactor() {
        if (gen.topIs(Op.MULT, Op.DIV)) gen.generateBinary();
    }

    /** Lo que hace el analizador al terminar un término dentro de una exp */
    private void afterTerm() {
        if (gen.topIs(Op.PLUS, Op.MINUS)) gen.generateBinary();
    }

    private String listing() {
        return gen.queue().format(QuadrupleQueue.Style.SYMBOLIC);
    }

    @Test
    void fondoFalsoSeparaLoDeAdentroDelParentesis() {
        // _A_ * (_B_ + _C_)
        gen.pushOperand(var("_A_", Type.INT)); afterFactor();
        gen.pushOperator(Op.MULT, 1, 3);
        gen.pushFakeBottom();                                    // (
        gen.pushOperand(var("_B_", Type.INT)); afterFactor(); afterTerm();
        gen.pushOperator(Op.PLUS, 1, 9);
        gen.pushOperand(var("_C_", Type.INT)); afterFactor(); afterTerm();
        gen.popFakeBottom();                                     // )
        afterFactor(); afterTerm();

        assertEquals("0 + _B_ _C_ t1\n1 * _A_ t1 t2\n", listing());
        assertEquals("t2", gen.popOperand().operand().name());
        assertTrue(gen.stacksEmpty());
    }

    @Test
    void sinFondoFalsoSeMezclanLosOperadoresDeAfuera() {
        // La misma secuencia sin fondo falso: al terminar _B_ el tope es el * de afuera
        gen.pushOperand(var("_A_", Type.INT)); afterFactor();
        gen.pushOperator(Op.MULT, 1, 3);
        gen.pushOperand(var("_B_", Type.INT)); afterFactor();
        assertEquals("0 * _A_ _B_ t1\n", listing());             // incorrecto: por eso existe el fondo falso
    }

    @Test
    void asociatividadIzquierda() {
        // _A_ - _B_ - _C_  →  (_A_ - _B_) - _C_
        gen.pushOperand(var("_A_", Type.INT)); afterFactor(); afterTerm();
        gen.pushOperator(Op.MINUS, 1, 4);
        gen.pushOperand(var("_B_", Type.INT)); afterFactor(); afterTerm();
        gen.pushOperator(Op.MINUS, 1, 10);
        gen.pushOperand(var("_C_", Type.INT)); afterFactor(); afterTerm();

        assertEquals("0 - _A_ _B_ t1\n1 - t1 _C_ t2\n", listing());
    }

    @Test
    void elTipoDelTemporalLoDaElCubo() {
        gen.pushOperand(gen.constant("7", Type.INT));
        gen.pushOperand(gen.constant("2", Type.INT));
        gen.pushOperator(Op.DIV, 1, 2);
        gen.generateBinary();
        assertEquals(Type.FLOAT, gen.popOperand().type());       // / siempre da float

        gen.pushOperand(var("_A_", Type.INT));
        gen.pushOperand(gen.constant("2.5", Type.FLOAT));
        gen.pushOperator(Op.LT, 1, 2);
        gen.generateBinary();
        assertEquals(Type.BOOL, gen.popOperand().type());
    }

    /** Genera {@code left op right} y regresa el temporal resultante (el estilo SYMBOLIC no muestra tipos) */
    private Operand tempOf(Operand left, Op op, Operand right) {
        gen.pushOperand(left);
        gen.pushOperand(right);
        gen.pushOperator(op, 1, 0);
        gen.generateBinary();
        TypedOperand result = gen.popOperand();
        assertEquals(result.type(), result.operand().type());   // la copia del operando coincide con la pila
        return result.operand();
    }

    @Test
    void cadaTemporalTieneElTipoDelCubo() {
        Operand i = var("_I_", Type.INT);
        Operand f = var("_F_", Type.FLOAT);

        Operand sum = tempOf(i, Op.PLUS, i);
        assertEquals(OperandKind.TEMPORARY, sum.kind());
        assertEquals(Type.INT, sum.type());                          // int + int
        assertEquals(Type.FLOAT, tempOf(i, Op.MULT, f).type());      // int * float
        assertEquals(Type.FLOAT, tempOf(f, Op.MINUS, i).type());     // float - int
        assertEquals(Type.FLOAT, tempOf(i, Op.DIV, i).type());       // int / int: siempre float
        assertEquals(Type.FLOAT, tempOf(f, Op.DIV, f).type());
        assertEquals(Type.BOOL, tempOf(i, Op.LT, f).type());         // relacional
        assertEquals(Type.BOOL, tempOf(f, Op.EQ, f).type());
        assertEquals(Type.BOOL, tempOf(i, Op.NEQ, i).type());

        gen.pushOperand(i);
        gen.generateUnary(Op.MINUS, 1, 0);
        assertEquals(Type.INT, gen.popOperand().operand().type());   // -int
        gen.pushOperand(f);
        gen.generateUnary(Op.MINUS, 1, 0);
        assertEquals(Type.FLOAT, gen.popOperand().operand().type()); // -float
        assertTrue(gen.stacksEmpty());
    }

    @Test
    void limiteDeLosFlotantes() {
        // El máximo de double es aproximadamente 1.797e308
        assertFalse(gen.constant("9".repeat(308) + ".0", Type.FLOAT).isError());   // ~1e308: cabe
        assertTrue(gen.constant("9".repeat(309) + ".0", Type.FLOAT).isError());    // ~1e309: infinito
        assertFalse(gen.constant("-2147483648", Type.INT).isError());
        assertTrue(gen.constant("-2147483649", Type.INT).isError());
    }

    @Test
    void unErrorViajaPorLasPilasSinMasMensajesNiCuadruplos() {
        // operando ya erróneo: no hay cuádruplo y no se reporta otra vez
        gen.pushOperand(Operand.ERROR);
        gen.pushOperand(var("_A_", Type.INT));
        gen.pushOperator(Op.PLUS, 1, 2);
        Outcome cascade = gen.generateBinary();
        assertFalse(cascade.failed());
        assertSame(Operand.ERROR, gen.popOperand().operand());

        // combinación inválida de operandos válidos: se reporta una vez, en la posición del operador
        gen.pushOperand(var("_A_", Type.INT));
        gen.pushOperand(gen.constant("1", Type.INT));
        gen.pushOperator(Op.LT, 3, 7);
        gen.generateBinary();                                    // bool
        gen.pushOperand(gen.constant("1", Type.INT));
        gen.pushOperator(Op.PLUS, 3, 12);
        Outcome invalid = gen.generateBinary();                  // bool + int
        assertTrue(invalid.failed());
        assertEquals(Type.BOOL, invalid.left());
        assertEquals(3, invalid.line());
        assertEquals(12, invalid.column());
        assertSame(Operand.ERROR, gen.popOperand().operand());

        assertEquals("0 < _A_ 1 t1\n", listing());               // solo el cuádruplo válido
        assertTrue(gen.stacksEmpty());
    }

    @Test
    void signoUnario() {
        gen.pushOperand(var("_A_", Type.FLOAT));
        assertFalse(gen.generateUnary(Op.MINUS, 1, 0).failed());
        TypedOperand negated = gen.popOperand();
        assertEquals(Type.FLOAT, negated.type());

        Operand a = var("_A_", Type.INT);
        gen.pushOperand(a);
        gen.generateUnary(Op.PLUS, 1, 0);                        // + no genera cuádruplo
        assertSame(a, gen.popOperand().operand());

        assertEquals("0 UMINUS _A_ _ t1\n", listing());
    }

    @Test
    void asignacion() {
        Operand x = var("_X_", Type.INT);
        gen.pushOperand(gen.constant("2.5", Type.FLOAT));
        Outcome invalid = gen.generateAssign(x, 1, 4);           // int ← float
        assertTrue(invalid.failed());

        Operand f = var("_F_", Type.FLOAT);
        gen.pushOperand(gen.constant("3", Type.INT));
        assertFalse(gen.generateAssign(f, 2, 4).failed());       // float ← int, sin conversión

        gen.pushOperand(gen.constant("3", Type.INT));
        assertFalse(gen.generateAssign(Operand.ERROR, 3, 4).failed());  // destino no declarado: ya reportado

        assertEquals("0 = 3 _ _F_\n", listing());
        assertTrue(gen.stacksEmpty());
    }

    @Test
    void constantes() {
        Operand min = gen.constant("-2147483648", Type.INT);     // cabe con su signo
        assertEquals(Integer.MIN_VALUE, min.value());
        assertEquals(OperandKind.CONSTANT, min.kind());
        assertTrue(gen.constant("2147483648", Type.INT).isError());
        assertEquals(3.14, gen.constant("3.14", Type.FLOAT).value());
        assertTrue(gen.constant("9".repeat(400) + ".0", Type.FLOAT).isError());   // infinito

        Operand text = gen.string("\"hola\\n\"");
        assertEquals(Type.STRING, text.type());
        assertEquals("hola\\n", text.value());                  // sin comillas, escape sin interpretar
    }

    @Test
    void erroresInternosLanzanExcepcion() {
        assertThrows(IllegalStateException.class, gen::popOperand);
        assertThrows(IllegalStateException.class, gen::popFakeBottom);
        assertThrows(IllegalStateException.class, gen::generateBinary);
        gen.pushFakeBottom();
        assertFalse(gen.topIs(Op.PLUS, Op.MINUS, Op.MULT, Op.DIV));
    }

    @Test
    void estatutosYFormatoDeLaFila() {
        Operand x = var("_X_", Type.INT);
        gen.emitRead(x);
        gen.note("if (línea 3): aquí irá GotoF");
        gen.emitPrint(gen.string("\"x =\""));
        gen.emitPrint(x);
        gen.emitPrint(Operand.ERROR);                            // no genera nada
        gen.emitPrintln();
        gen.emitEnd();
        gen.note("al final");

        assertEquals("""
                0 READ _ _ _X_
                1 PRINT "x =" _ _
                2 PRINT _X_ _ _
                3 PRINTLN _ _ _
                4 END _ _ _
                """, listing());                                 // SYMBOLIC no lleva notas

        String full = gen.queue().format(QuadrupleQueue.Style.FULL);
        assertTrue(full.startsWith("#  op"));
        assertTrue(full.indexOf("· if (línea 3)") > full.indexOf("READ"));
        assertTrue(full.indexOf("· if (línea 3)") < full.indexOf("PRINT"));
        assertTrue(full.trim().endsWith("· al final"));
        assertEquals(1, gen.queue().notes().get(0).position());
    }

    @Test
    void codigosFijosDeLosOperadores() {
        assertEquals(1, QuadOp.PLUS.code());
        assertEquals(2, QuadOp.MINUS.code());
        assertEquals(3, QuadOp.MULT.code());
        assertEquals(4, QuadOp.DIV.code());
        Set<Integer> codes = new HashSet<>();
        for (QuadOp op : QuadOp.values()) {
            assertTrue(codes.add(op.code()), "código repetido: " + op);
        }
        for (Op op : Op.values()) {
            assertNotNull(QuadOp.from(op));
        }
    }

    @Test
    void stringNuncaEntraAlCubo() {
        for (Type other : Type.values()) {
            for (Op op : Op.values()) {
                assertEquals(Type.ERROR, cube.result(Type.STRING, op, other));
                assertEquals(Type.ERROR, cube.result(other, op, Type.STRING));
            }
        }
        assertEquals(Type.ERROR, cube.unaryResult(Op.MINUS, Type.STRING));
        assertFalse(cube.canAssign(Type.FLOAT, Type.STRING));
    }
}
