package com.babyduck.intermediate;

import com.babyduck.semantic.Op;
import com.babyduck.semantic.SemanticCube;
import com.babyduck.semantic.Type;
import com.babyduck.semantic.VariableInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Generador de cuádruplos: las tres pilas (operadores, operandos y tipos) y la fila
 *
 * SemanticAnalyzer lo llama desde sus puntos neurálgicos. Esta clase no conoce el árbol
 * de ANTLR ni el ErrorCollector: detecta los errores de tipos al sacar operandos de las
 * pilas y los regresa en un {@link Outcome}; SemanticAnalyzer arma el mensaje y lo reporta
 *
 * Reglas que mantiene:
 *  - Las pilas de operandos y de tipos avanzan juntas: solo se modifican con
 *    {@link #pushOperand} y {@link #popOperand}, que verifican que el tipo coincida
 *  - Toda expresión deja exactamente un operando en la pila, aunque sea {@link Operand#ERROR};
 *    así las pilas nunca se desbalancean y un error no produce más mensajes ni cuádruplos
 *  - Un error interno (pila vacía, tipos desincronizados, fondo falso perdido) es un bug del
 *    compilador y lanza IllegalStateException; los errores del programa nunca usan excepciones
 */
public final class QuadrupleGenerator {

    /** Elemento de la pila de operadores: el operador y dónde está, o el fondo falso */
    private record OperatorEntry(Op op, int line, int column) {
        boolean isFakeBottom() {
            return op == null;
        }
    }

    private static final OperatorEntry FAKE_BOTTOM = new OperatorEntry(null, 0, 0);

    private final SemanticCube cube;
    private final Deque<OperatorEntry> operators = new ArrayDeque<>();   // pila de operadores
    private final Deque<Operand> operands = new ArrayDeque<>();          // pila de operandos
    private final Deque<Type> types = new ArrayDeque<>();                // pila de tipos
    private final QuadrupleQueue queue = new QuadrupleQueue();           // fila de cuádruplos
    private int tempCount = 0;

    public QuadrupleGenerator(SemanticCube cube) {
        this.cube = cube;
    }

    public QuadrupleQueue queue() {
        return queue;
    }

    // Fábrica de operandos: único lugar donde se crean (en la entrega 4 asignarán dirección)

    /** Operando de una variable o parámetro ya declarado */
    public Operand variable(VariableInfo variable) {
        return new Operand(OperandKind.VARIABLE, variable.name(), variable.type(), null);
    }

    /**
     * Operando de una constante numérica, convirtiendo su texto. Si el signo va pegado,
     * el texto lo incluye ("-2147483648" es válido aunque "2147483648" no quepa en int)
     *
     * @return la constante, o {@link Operand#ERROR} si no cabe en su tipo
     *         (int fuera de rango, o float que resulta infinito); quien llama reporta
     */
    public Operand constant(String text, Type type) {
        switch (type) {
            case INT -> {
                try {
                    return new Operand(OperandKind.CONSTANT, text, Type.INT, Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    return Operand.ERROR;
                }
            }
            case FLOAT -> {
                double value = Double.parseDouble(text);
                if (Double.isInfinite(value)) {
                    return Operand.ERROR;
                }
                return new Operand(OperandKind.CONSTANT, text, Type.FLOAT, value);
            }
            default -> throw new IllegalArgumentException("Tipo de constante inválido: " + type);
        }
    }

    /** Operando de un letrero; el valor es el contenido sin comillas (los escapes se interpretan en la máquina virtual) */
    public Operand string(String literal) {
        String content = literal.substring(1, literal.length() - 1);
        return new Operand(OperandKind.STRING, literal, Type.STRING, content);
    }

    /** Temporal nuevo: t1, t2, ... (contador simple, sin reutilizar) */
    public Operand newTemp(Type type) {
        tempCount++;
        return new Operand(OperandKind.TEMPORARY, "t" + tempCount, type, null);
    }

    // Pilas de operandos y de tipos (siempre juntas)

    public void pushOperand(Operand operand) {
        operands.push(operand);
        types.push(operand.type());
    }

    /** Saca el operando del tope junto con el tipo que sale de la pila de tipos */
    public TypedOperand popOperand() {
        if (operands.isEmpty() || types.isEmpty()) {
            throw new IllegalStateException("La pila de operandos está vacía");
        }
        Operand operand = operands.pop();
        Type type = types.pop();
        if (operand.type() != type) {
            throw new IllegalStateException("Pilas desincronizadas: " + operand + " es "
                    + operand.type() + " pero la pila de tipos dice " + type);
        }
        return new TypedOperand(operand, type);
    }

    // Pila de operadores

    public void pushOperator(Op op, int line, int column) {
        operators.push(new OperatorEntry(op, line, column));
    }

    /** Fondo falso: separa una expresión anidada (paréntesis o argumento) de los operadores de afuera */
    public void pushFakeBottom() {
        operators.push(FAKE_BOTTOM);
    }

    public void popFakeBottom() {
        if (operators.isEmpty() || !operators.peek().isFakeBottom()) {
            throw new IllegalStateException("Se esperaba el fondo falso en el tope de la pila de operadores");
        }
        operators.pop();
    }

    /** ¿El tope de la pila de operadores es alguno de estos? (el fondo falso nunca coincide) */
    public boolean topIs(Op... ops) {
        OperatorEntry top = operators.peek();
        if (top == null || top.isFakeBottom()) {
            return false;
        }
        for (Op op : ops) {
            if (top.op() == op) {
                return true;
            }
        }
        return false;
    }

    // Generación

    /**
     * Saca el operador del tope y dos operandos con sus tipos, consulta el cubo, genera
     * (op, izq, der, temporal) y mete el temporal. Si algún operando ya era ERROR, o el
     * cubo rechaza la combinación, mete ERROR y no genera nada
     */
    public Outcome generateBinary() {
        if (operators.isEmpty() || operators.peek().isFakeBottom()) {
            throw new IllegalStateException("No hay operador en el tope de la pila");
        }
        OperatorEntry entry = operators.pop();
        TypedOperand right = popOperand();
        TypedOperand left = popOperand();

        Type result = (left.isError() || right.isError())
                ? Type.ERROR
                : cube.result(left.type(), entry.op(), right.type());
        if (result == Type.ERROR) {
            pushOperand(Operand.ERROR);
        } else {
            Operand temp = newTemp(result);
            emit(QuadOp.from(entry.op()), left.operand(), right.operand(), temp);
            pushOperand(temp);
        }
        return new Outcome(entry.op(), left.type(), right.type(), result, entry.line(), entry.column());
    }

    /**
     * Signo unario sobre el operando del tope: el + no genera cuádruplo; el - genera
     * (UMINUS, x, _, temporal). Las constantes negativas (-2) no pasan por aquí:
     * se crean directamente con {@link #constant}
     */
    public Outcome generateUnary(Op sign, int line, int column) {
        TypedOperand operand = popOperand();
        if (operand.isError()) {
            pushOperand(Operand.ERROR);
            return new Outcome(sign, null, Type.ERROR, Type.ERROR, line, column);
        }
        Type result = cube.unaryResult(sign, operand.type());
        if (result == Type.ERROR) {
            pushOperand(Operand.ERROR);
        } else if (sign == Op.PLUS) {
            pushOperand(operand.operand());
        } else {
            Operand temp = newTemp(result);
            emit(QuadOp.UMINUS, operand.operand(), null, temp);
            pushOperand(temp);
        }
        return new Outcome(sign, null, operand.type(), result, line, column);
    }

    /**
     * Asignación: saca el valor del tope, revisa con el cubo destino ← valor y genera
     * (=, valor, _, destino). El destino no entra a las pilas: no es operando de una expresión
     *
     * @param target la variable destino, o {@link Operand#ERROR} si no está declarada (ya reportado)
     */
    public Outcome generateAssign(Operand target, int line, int column) {
        TypedOperand value = popOperand();
        Type targetType = target.type();
        if (target.isError() || value.isError()) {
            return new Outcome(Op.ASSIGN, targetType, value.type(), Type.ERROR, line, column);
        }
        Type result = cube.result(targetType, Op.ASSIGN, value.type());
        if (result != Type.ERROR) {
            emit(QuadOp.ASSIGN, value.operand(), null, target);
        }
        return new Outcome(Op.ASSIGN, targetType, value.type(), result, line, column);
    }

    /** (PRINT, elemento, _, _); un elemento con error no genera nada */
    public void emitPrint(Operand item) {
        if (!item.isError()) {
            emit(QuadOp.PRINT, item, null, null);
        }
    }

    /** (PRINTLN, _, _, _) al final de cada print */
    public void emitPrintln() {
        emit(QuadOp.PRINTLN, null, null, null);
    }

    /** (READ, _, _, variable); una variable con error no genera nada */
    public void emitRead(Operand target) {
        if (!target.isError()) {
            emit(QuadOp.READ, null, null, target);
        }
    }

    /** (END, _, _, _) al final de main */
    public void emitEnd() {
        emit(QuadOp.END, null, null, null);
    }

    /** Nota en la posición actual de la fila (código que se generará en entregas siguientes) */
    public void note(String text) {
        queue.addNote(text);
    }

    /** true si las tres pilas están vacías (al terminar cada estatuto deben estarlo) */
    public boolean stacksEmpty() {
        return operators.isEmpty() && operands.isEmpty() && types.isEmpty();
    }

    private void emit(QuadOp op, Operand left, Operand right, Operand result) {
        queue.add(new Quadruple(op, left, right, result));
    }
}
