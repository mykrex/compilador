package com.babyduck;

import com.babyduck.intermediate.ConstantTable;
import com.babyduck.intermediate.Operand;
import com.babyduck.intermediate.Outcome;
import com.babyduck.intermediate.QuadrupleGenerator;
import com.babyduck.intermediate.QuadrupleQueue;
import com.babyduck.intermediate.Segment;
import com.babyduck.intermediate.TypedOperand;
import com.babyduck.intermediate.VirtualMemory;
import com.babyduck.semantic.FunctionDirectory;
import com.babyduck.semantic.FunctionDirectory.NameKind;
import com.babyduck.semantic.FunctionInfo;
import com.babyduck.semantic.MemorySize;
import com.babyduck.semantic.Op;
import com.babyduck.semantic.SemanticCube;
import com.babyduck.semantic.Type;
import com.babyduck.semantic.VariableInfo;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Recorrido semántico del árbol sintáctico (Visitor), en una sola pasada
 *
 * Llena el directorio de funciones y las tablas de variables, y detecta
 * declaraciones repetidas. También valida el uso: variables y funciones
 * no declaradas, llamadas, asignaciones, condiciones, expresiones y return
 *
 * Además genera los cuádruplos de las expresiones, los estatutos lineales y los saltos
 * de if y while con {@link QuadrupleGenerator}. Las expresiones no devuelven su tipo: dejan su resultado
 * (operando y tipo) en las pilas del generador, y quien las usa lo saca de ahí.
 * Si una parte de una expresión ya es ERROR, ese error ya se reportó y no se reporta otro
 *
 * Se usa un Visitor, aunque ya no devuelva tipos, porque permite acciones a mitad de
 * una regla (por ejemplo, el GOTOF entre la condición y el cuerpo de un if o un while)
 */
public class SemanticAnalyzer extends BabyDuckBaseVisitor<Void> {

    private final ErrorCollector errors;
    private final FunctionDirectory directory = new FunctionDirectory();
    private final SemanticCube cube = new SemanticCube();
    private final QuadrupleGenerator generator = new QuadrupleGenerator(cube);

    /** Función cuyo cuerpo se está analizando; {@code null} = alcance global / main */
    private FunctionInfo currentFunction;

    public SemanticAnalyzer(ErrorCollector errors) {
        this.errors = errors;
    }

    public FunctionDirectory directory() {
        return directory;
    }

    /** Fila de cuádruplos generada (solo es válida si no hubo errores) */
    public QuadrupleQueue quadruples() {
        return generator.queue();
    }

    /** Tabla de constantes con sus direcciones (solo es válida si no hubo errores) */
    public ConstantTable constants() {
        return generator.constants();
    }

    // Programa

    @Override
    public Void visitProgram(BabyDuckParser.ProgramContext ctx) {
        directory.setProgramName(ctx.ID().getText());                    // punto 2: nombre del programa
        generator.startProgram();                                        // punto 1: GOTO a main pendiente en el cuádruplo 0
        if (ctx.vars() != null) {
            visit(ctx.vars());                                           // globales
        }
        for (BabyDuckParser.FuncDeclContext function : ctx.funcDecl()) {
            visit(function);
        }
        // punto 3: inicio de main
        currentFunction = null;                                          // main solo ve globales
        generator.memory().startScope();                                 // temporales desde la base
        generator.startMain();                                           // se rellena el GOTO del cuádruplo 0
        visit(ctx.body());
        // punto 4: fin del programa (END y tamaño del segmento global y de los temporales de main)
        generator.emitEnd();
        directory.setSizes(sizeOf(Segment.GLOBAL), sizeOf(Segment.TEMPORARY));
        return null;                                                     // la tabla de globales se conserva
    }

    // Variables

    /** {@code _A_, _B_ : int;} — el tipo ya está en el mismo nodo que los nombres */
    @Override
    public Void visitVarDecl(BabyDuckParser.VarDeclContext ctx) {
        Type type = Type.fromKeyword(ctx.type().getText());
        for (TerminalNode id : ctx.ID()) {
            declareVariable(id.getSymbol(), type, false);                // punto 5: declarar con dirección
        }
        return null;
    }

    // Funciones

    @Override
    public Void visitFuncDecl(BabyDuckParser.FuncDeclContext ctx) {
        Token nameToken = ctx.ID().getSymbol();
        String name = nameToken.getText();
        Type returnType = Type.fromKeyword(ctx.funcType().getText());
        FunctionInfo function = new FunctionInfo(name, returnType);      // ya trae su tabla

        // punto 6: registrar la función (dirección de regreso, todos los caminos, alcance nuevo)
        // Se registra antes de los parámetros y el cuerpo, así puede llamarse a sí misma
        NameKind use = directory.globalUse(name);
        if (use == NameKind.FUNCTION) {
            error(nameToken, "la función " + name + " ya fue declarada");
        } else if (use != null) {
            error(nameToken, name + " ya se usa como " + describe(use));
        } else {
            directory.addFunction(function);
        }
        // Aunque el nombre esté repetido, se revisa su contenido para reportar todos los errores

        // Dirección de regreso: una función con valor deja su resultado en una dirección global
        if (returnType != Type.VOID) {
            function.setReturnAddress(generator.memory().allocate(Segment.GLOBAL, returnType));
            reportMemoryOverflow(nameToken);
        }

        // Una función con valor debe regresar en todos los caminos. Se revisa antes del
        // cuerpo porque solo inspecciona el árbol, y así el error sale en orden de línea
        if (returnType != Type.VOID && !bodyAlwaysReturns(ctx.body())) {
            error(nameToken, "la función " + name + " no regresa un valor en todos los caminos");
        }

        currentFunction = function;
        generator.memory().startScope();                                 // inicio de función: locales y temporales desde la base
        if (ctx.params() != null) {                                      // los parámetros reciben las primeras direcciones locales
            visit(ctx.params());
        }
        if (ctx.vars() != null) {
            visit(ctx.vars());                                           // variables locales
        }
        function.setStartQuad(generator.queue().nextIndex());            // punto 8: inicio de la función
        visit(ctx.body());
        // punto 9: fin de la función: ENDFUNC, tamaño (antes del siguiente startScope) y se libera su tabla local
        generator.emitEndFunc();
        function.setSizes(sizeOf(Segment.LOCAL), sizeOf(Segment.TEMPORARY));
        function.releaseLocals();
        currentFunction = null;
        return null;
    }

    /** {@code _X_ : int} dentro de los paréntesis de una función */
    @Override
    public Void visitParam(BabyDuckParser.ParamContext ctx) {
        Type type = Type.fromKeyword(ctx.type().getText());
        declareVariable(ctx.ID().getSymbol(), type, true);               // punto 7: parámetro con dirección local
        return null;
    }

    // Estatutos

    /** {@code { estatutos }}: cuerpo de una función o de main, if, else y while */
    @Override
    public Void visitBody(BabyDuckParser.BodyContext ctx) {
        visitStatements(ctx.statement());
        return null;
    }

    /** El bloque {@code [ estatutos ]} se revisa igual que un cuerpo; el resto, por su propio método */
    @Override
    public Void visitStatement(BabyDuckParser.StatementContext ctx) {
        if (ctx.LBRACKET() != null) {
            visitStatements(ctx.statement());
            return null;
        }
        visitChildren(ctx);
        reportMemoryOverflow(ctx.getStart());                            // temporales o constantes que ya no cupieron
        return null;
    }

    /**
     * {@code _X_ = expresión;} — destino ← valor
     * El destino no entra a las pilas (no es operando de una expresión): se valida y se
     * conserva; después de la expresión se saca el valor, se revisa con el cubo y se genera =
     */
    @Override
    public Void visitAssign(BabyDuckParser.AssignContext ctx) {
        Token target = ctx.ID().getSymbol();
        Operand destination = variableOperand(target);                   // punto 10: validar el destino
        visit(ctx.expression());
        Token assign = ctx.ASSIGN().getSymbol();
        Outcome outcome = generator.generateAssign(destination,          // punto 11: cubo y cuádruplo =
                assign.getLine(), assign.getCharPositionInLine());
        if (outcome.failed()) {
            error(assign, "no se puede asignar " + outcome.right().display()
                    + " a " + target.getText() + ", que es " + outcome.left().display());
        }
        return null;
    }

    /**
     * {@code if (condición) {...} else {...};} — GOTOF después de la condición; con else,
     * GOTO al terminar el primer cuerpo; los saltos se rellenan con la pila de saltos
     */
    @Override
    public Void visitCondition(BabyDuckParser.ConditionContext ctx) {
        TypedOperand condition = checkCondition(ctx.expression(), "if");
        generator.startIf(condition.operand());                          // punto 12: condición bool y GOTOF
        visit(ctx.body(0));
        if (ctx.ELSE() != null) {
            generator.startElse();                                       // punto 13: GOTO y se rellena el GOTOF
            visit(ctx.body(1));
        }
        generator.endIf();                                               // punto 14: se rellena el salto pendiente
        return null;
    }

    /**
     * {@code while (condición) do {...};} — se guarda el inicio de la condición, GOTOF
     * después de ella, y al final GOTO al inicio y se rellena el GOTOF
     */
    @Override
    public Void visitCycle(BabyDuckParser.CycleContext ctx) {
        generator.markLoopStart();                                       // punto 15: regreso (inicio de la condición)
        TypedOperand condition = checkCondition(ctx.expression(), "while");
        generator.startLoopBody(condition.operand());                    // punto 12: condición bool y GOTOF
        visit(ctx.body());
        generator.endLoop();                                             // punto 16: GOTO al regreso y se rellena el GOTOF
        return null;
    }

    /** {@code read(_X_);} — solo puede leer en una variable declarada */
    @Override
    public Void visitRead(BabyDuckParser.ReadContext ctx) {
        generator.emitRead(variableOperand(ctx.ID().getSymbol()));       // punto 24: READ
        return null;
    }

    /** {@code print(e1, e2, ...);} — un PRINT por elemento, en orden, y un PRINTLN al final */
    @Override
    public Void visitPrint(BabyDuckParser.PrintContext ctx) {
        for (BabyDuckParser.PrintItemContext item : ctx.printItem()) {
            if (item.CTE_STRING() != null) {
                generator.emitPrint(generator.string(item.CTE_STRING().getText()));   // punto 22: PRINT
            } else {
                // acepta BOOL; una función void dentro de print ya la reporta checkCall
                generator.emitPrint(evaluate(item.expression()).operand());           // punto 22: PRINT
            }
        }
        generator.emitPrintln();                                         // punto 23: PRINTLN
        return null;
    }

    /** Llamada usada como estatuto ({@code _F_(2);}); si regresa un valor, se descarta */
    @Override
    public Void visitFuncCall(BabyDuckParser.FuncCallContext ctx) {
        checkCall(ctx, false);
        return null;
    }

    /** Punto 25: reglas de return y su cuádruplo RETURN (regresa al llamador en ese momento) */
    @Override
    public Void visitReturnStmt(BabyDuckParser.ReturnStmtContext ctx) {
        Token keyword = ctx.RETURN().getSymbol();
        BabyDuckParser.ExpressionContext value = ctx.expression();

        if (currentFunction == null) {
            error(keyword, "return solo puede usarse dentro de una función");
            if (value != null) evaluate(value);                          // errores dentro del valor
            return null;
        }

        String name = currentFunction.name();
        Type returnType = currentFunction.returnType();
        if (returnType == Type.VOID) {
            if (value != null) {
                error(keyword, "la función " + name + " es void, no puede regresar un valor");
                evaluate(value);
            }
            if (value == null) {
                generator.emitReturn(null, null);                        // return; en void: RETURN _ _ _
            }
            return null;                                                 // return; en void es válido
        }
        if (value == null) {
            error(keyword, "la función " + name + " debe regresar un valor " + returnType.display());
            return null;
        }
        TypedOperand result = evaluate(value);
        if (!result.isError() && !cube.canAssign(returnType, result.type())) {
            error(value.getStart(), "la función " + name + " regresa " + returnType.display()
                    + ", no puede regresar " + result.type().display());
        } else if (!result.isError()) {
            // return con valor: RETURN valor _ dirección de regreso (global de la función)
            generator.emitReturn(result.operand(),
                    generator.returnSlot(name, returnType, currentFunction.returnAddress()));
        }
        return null;
    }

    // Expresiones: dejan su resultado en las pilas. Después de cada operando se revisa
    // el tope de la pila de operadores para decidir si se genera el cuádruplo

    /** {@code exp (relOp exp)?} */
    @Override
    public Void visitExpression(BabyDuckParser.ExpressionContext ctx) {
        visit(ctx.exp(0));
        if (ctx.relOp() != null) {
            pushOperator(ctx.relOp().getStart());                        // punto 26: meter el operador
            visit(ctx.exp(1));
            if (generator.topIs(Op.GT, Op.LT, Op.EQ, Op.NEQ)) {
                report(generator.generateBinary());                      // punto 27: cuádruplo relacional
            }
        }
        return null;
    }

    /** {@code term ((+|-) term)*}, de izquierda a derecha */
    @Override
    public Void visitExp(BabyDuckParser.ExpContext ctx) {
        for (int i = 0; i < ctx.term().size(); i++) {
            if (i > 0) {
                pushOperator(operatorBefore(ctx, i));                    // punto 26: meter el operador
            }
            visit(ctx.term(i));
            if (generator.topIs(Op.PLUS, Op.MINUS)) {
                report(generator.generateBinary());                      // punto 28: cuádruplo + -
            }
        }
        return null;
    }

    /** {@code factor ((*|/) factor)*}, de izquierda a derecha */
    @Override
    public Void visitTerm(BabyDuckParser.TermContext ctx) {
        for (int i = 0; i < ctx.factor().size(); i++) {
            if (i > 0) {
                pushOperator(operatorBefore(ctx, i));                    // punto 26: meter el operador
            }
            visit(ctx.factor(i));
            if (generator.topIs(Op.MULT, Op.DIV)) {
                report(generator.generateBinary());                      // punto 28: cuádruplo * /
            }
        }
        return null;
    }

    /** {@code (+|-)? ( (expresión) | llamada | _Id_ | constante )} */
    @Override
    public Void visitFactor(BabyDuckParser.FactorContext ctx) {
        TerminalNode sign = (ctx.MINUS() != null) ? ctx.MINUS() : ctx.PLUS();

        if (ctx.cte() != null) {
            pushConstant(ctx.cte(), sign);                               // punto 29 (el signo va con la constante)
            return null;
        }
        if (ctx.expression() != null) {
            generator.pushFakeBottom();                                  // punto 18: fondo falso en (
            visit(ctx.expression());
            generator.popFakeBottom();                                   // punto 19: se quita en )
        } else if (ctx.funcCall() != null) {
            checkCall(ctx.funcCall(), true);                             // punto 29: deja el temporal del regreso
        } else {
            generator.pushOperand(variableOperand(ctx.ID().getSymbol())); // punto 29: operando con dirección
        }

        if (sign != null) {                                              // punto 30: signo unario
            Token signToken = sign.getSymbol();
            Outcome outcome = generator.generateUnary(Op.fromSymbol(sign.getText()),
                    signToken.getLine(), signToken.getCharPositionInLine());
            if (outcome.failed()) {
                error(signToken, "no se puede aplicar el signo " + sign.getText()
                        + " a " + outcome.right().display());
            }
        }
        return null;
    }

    // Auxiliares

    /**
     * Declara una variable o parámetro en el alcance actual
     * Se reporta un solo error por declaración.
     */
    private void declareVariable(Token token, Type type, boolean isParam) {
        String name = token.getText();

        if (currentFunction == null) {                                   // global
            NameKind use = directory.globalUse(name);
            if (use == NameKind.GLOBAL_VARIABLE) {
                error(token, "la variable " + name + " ya fue declarada");
            } else if (use != null) {
                error(token, name + " ya se usa como " + describe(use));
            } else {
                directory.globals().add(withAddress(token, name, type, Segment.GLOBAL));
            }
            return;
        }

        // Local o parámetro, primero la propia función, luego el espacio de nombres global
        if (currentFunction.locals().contains(name)) {
            error(token, name + " ya fue declarado en la función " + currentFunction.name());
            return;
        }
        NameKind use = directory.globalUse(name);
        if (use != null) {
            error(token, name + " ya se usa como " + describe(use));
            return;
        }
        VariableInfo variable = withAddress(token, name, type, Segment.LOCAL);
        if (isParam) {
            currentFunction.addParam(variable);
        } else {
            currentFunction.addLocal(variable);
        }
    }

    /**
     * Variable con la siguiente dirección libre del segmento. Solo se pide cuando la
     * declaración es válida, así una declaración repetida no gasta direcciones
     */
    private VariableInfo withAddress(Token token, String name, Type type, Segment segment) {
        VariableInfo variable = new VariableInfo(name, type, generator.memory().allocate(segment, type));
        reportMemoryOverflow(token);
        return variable;
    }

    /**
     * Reporta los rangos de direcciones que se llenaron (variables, temporales o constantes).
     * Cada rango se reporta una sola vez; si la variable no cupo, sus usos no generan más errores
     */
    private void reportMemoryOverflow(Token where) {
        for (String message : generator.memory().takeOverflows()) {
            error(where, message);
        }
    }

    /**
     * Operando de un nombre usado como variable. Si no es una variable visible,
     * reporta qué es en realidad y devuelve {@link Operand#ERROR}
     */
    private Operand variableOperand(Token token) {
        String name = token.getText();
        VariableInfo variable = directory.lookupVariable(name, currentFunction);
        if (variable != null) {
            return generator.variable(variable);
        }
        NameKind use = directory.globalUse(name);
        if (use == NameKind.FUNCTION) {
            error(token, name + " es una función, no una variable");
        } else if (use == NameKind.PROGRAM) {
            error(token, name + " es el nombre del programa");
        } else {
            error(token, "la variable " + name + " no está declarada");
        }
        return Operand.ERROR;
    }

    /**
     * Mete una constante a las pilas. Si el signo va pegado, se convierte junto con ella
     * ({@code -2} es una constante negativa, sin cuádruplo); el + no cambia nada
     */
    private void pushConstant(BabyDuckParser.CteContext cte, TerminalNode sign) {
        Type type = (cte.CTE_INT() != null) ? Type.INT : Type.FLOAT;
        String text = cte.getText();
        if (sign != null && sign.getSymbol().getType() == BabyDuckParser.MINUS) {
            text = "-" + text;
        }
        Operand constant = generator.constant(text, type);
        if (constant.isError()) {
            Token where = (sign != null) ? sign.getSymbol() : cte.getStart();
            error(where, "la constante " + text + " no cabe en " + type.display());
        }
        generator.pushOperand(constant);
    }

    /** Visita una expresión completa y saca su resultado de las pilas */
    private TypedOperand evaluate(BabyDuckParser.ExpressionContext expression) {
        visit(expression);
        return generator.popOperand();
    }

    /** Un argumento se evalúa entre fondos falsos, para que no use operadores de afuera */
    private TypedOperand evaluateArgument(BabyDuckParser.ExpressionContext argument) {
        generator.pushFakeBottom();                                      // punto 18: fondo falso
        visit(argument);
        generator.popFakeBottom();                                       // punto 19: se quita
        return generator.popOperand();
    }

    /**
     * Revisa una llamada. Los errores salen en orden: primero los del nombre,
     * después los de cada argumento. Primero se evalúan todos los argumentos y después se
     * generan ERA, un PARAM por argumento (en orden, desde 1) y GOSUB: así una llamada
     * anidada, como f(g(1)), termina completa antes del ERA de f
     *
     * @param inExpression {@code true} si la llamada está dentro de una expresión: entonces
     *                     deja en las pilas un temporal con el tipo de retorno, o ERROR
     */
    private void checkCall(BabyDuckParser.FuncCallContext ctx, boolean inExpression) {
        Token nameToken = ctx.ID().getSymbol();
        String name = nameToken.getText();
        List<BabyDuckParser.ExpressionContext> args = ctx.expression();
        FunctionInfo function = directory.getFunction(name);

        // punto 17: validar la función (existe, no void en expresión, número de argumentos)
        if (function == null) {
            if (directory.lookupVariable(name, currentFunction) != null) {
                error(nameToken, name + " es una variable, no una función");
            } else if (directory.globalUse(name) == NameKind.PROGRAM) {
                error(nameToken, name + " es el nombre del programa");
            } else {
                error(nameToken, "la función " + name + " no está declarada");
            }
            args.forEach(this::evaluateArgument);                        // errores dentro de los argumentos
            if (inExpression) {
                generator.pushOperand(Operand.ERROR);
            }
            return;
        }

        boolean voidValue = inExpression && function.returnType() == Type.VOID;
        if (voidValue) {
            error(nameToken, "la función " + name + " es void y no devuelve un valor");
        }

        int expected = function.paramCount();
        List<Operand> values = new ArrayList<>();                        // resultado de cada argumento
        if (args.size() != expected) {
            // Un solo error por llamada: con otro número de argumentos no se comparan los tipos
            error(nameToken, name + " espera " + expected
                    + (expected == 1 ? " argumento" : " argumentos") + ", recibió " + args.size());
            args.forEach(this::evaluateArgument);
        } else {
            for (int i = 0; i < expected; i++) {
                TypedOperand arg = evaluateArgument(args.get(i));
                Type paramType = function.params().get(i).type();
                if (!arg.isError() && !cube.canAssign(paramType, arg.type())) {
                    error(args.get(i).getStart(), "argumento " + (i + 1) + " de " + name
                            + ": no se puede pasar " + arg.type().display() + " a " + paramType.display());
                }
                values.add(arg.operand());                               // punto 20: guardar y revisar el argumento
            }
        }

        // punto 21: ERA, PARAM por argumento, GOSUB y, en expresión, copia del regreso
        generator.emitEra(name);                                         // ERA después de los argumentos
        for (int i = 0; i < values.size(); i++) {
            generator.emitParam(values.get(i), i + 1);                   // PARAM argumento _ k
        }
        generator.emitGosub(name, function.startQuad());                 // GOSUB al inicio de la función

        if (!inExpression) {
            return;                                                      // como estatuto, el valor se descarta
        }
        if (voidValue) {
            generator.pushOperand(Operand.ERROR);
        } else {
            // llamada en expresión: el valor de regreso se copia a un temporal propio
            generator.emitCallResult(generator.returnSlot(name, function.returnType(), function.returnAddress()));
        }
    }

    /** Tamaño usado en el segmento hasta ahora, por tipo (para el directorio de funciones) */
    private MemorySize sizeOf(Segment segment) {
        VirtualMemory memory = generator.memory();
        return new MemorySize(memory.count(segment, Type.INT), memory.count(segment, Type.FLOAT),
                memory.count(segment, Type.BOOL));
    }

    /** La condición de if/while debe ser BOOL; regresa el operando de la condición */
    private TypedOperand checkCondition(BabyDuckParser.ExpressionContext condition, String statement) {
        TypedOperand result = evaluate(condition);
        if (!result.isError() && result.type() != Type.BOOL) {
            error(condition.getStart(), "la condición del " + statement
                    + " debe ser una comparación (bool), se obtuvo " + result.type().display());
        }
        return result;
    }

    /** Mete a la pila el operador de este token, con su posición */
    private void pushOperator(Token op) {
        generator.pushOperator(Op.fromSymbol(op.getText()), op.getLine(), op.getCharPositionInLine());
    }

    /** Reporta una operación binaria que el cubo rechazó (si un operando ya era ERROR no se reporta) */
    private void report(Outcome outcome) {
        if (!outcome.failed()) {
            return;
        }
        // "y" cambia a "e" antes de una palabra que suena a "i" (bool e int)
        String and = outcome.right().display().startsWith("i") ? " e " : " y ";
        errors.semantic(outcome.line(), outcome.column(), "no se puede aplicar " + outcome.op().symbol()
                + " entre " + outcome.left().display() + and + outcome.right().display());
    }

    /**
     * Operador entre el operando {@code i-1} y el {@code i} de {@code exp} o {@code term}.
     * Sin etiquetas en la gramática, los hijos quedan alternados:
     * [operando, op, operando, op, operando], así que el operador i está en la posición 2i-1
     */
    private static Token operatorBefore(org.antlr.v4.runtime.ParserRuleContext ctx, int i) {
        return ((TerminalNode) ctx.getChild(2 * i - 1)).getSymbol();
    }

    /**
     * Revisa una lista de estatutos en orden. Si uno siempre regresa, el siguiente es
     * código inalcanzable: se reporta una vez y se siguen revisando todos.
     * Al terminar cada estatuto las pilas deben quedar vacías
     */
    private void visitStatements(List<BabyDuckParser.StatementContext> statements) {
        boolean returned = false;
        boolean reported = false;
        for (BabyDuckParser.StatementContext statement : statements) {
            // Regla de código inalcanzable (sin número de punto): un estatuto después de uno que
            // siempre regresa. En main, return ya es error; no se reporta además código inalcanzable
            if (returned && !reported && currentFunction != null) {
                error(statement.getStart(), "código inalcanzable: hay un return antes de este estatuto");
                reported = true;
            }
            visit(statement);
            if (!generator.stacksEmpty()) {
                throw new IllegalStateException("Las pilas no quedaron vacías después del estatuto de la línea "
                        + statement.getStart().getLine());
            }
            returned = returned || alwaysReturns(statement);
        }
    }

    // Análisis de caminos: solo inspecciona el árbol, no reporta errores

    /** ¿Algún estatuto del cuerpo siempre regresa? */
    private static boolean bodyAlwaysReturns(BabyDuckParser.BodyContext body) {
        return listAlwaysReturns(body.statement());
    }

    private static boolean listAlwaysReturns(List<BabyDuckParser.StatementContext> statements) {
        for (BabyDuckParser.StatementContext statement : statements) {
            if (alwaysReturns(statement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return → sí; if con else → si ambos cuerpos regresan; if sin else y while → no
     * (pueden no ejecutarse); bloque [ ] → si alguno de sus estatutos regresa; los demás → no
     */
    private static boolean alwaysReturns(BabyDuckParser.StatementContext statement) {
        if (statement.returnStmt() != null) {
            return true;
        }
        if (statement.condition() != null) {
            BabyDuckParser.ConditionContext condition = statement.condition();
            return condition.ELSE() != null
                    && bodyAlwaysReturns(condition.body(0))
                    && bodyAlwaysReturns(condition.body(1));
        }
        if (statement.LBRACKET() != null) {
            return listAlwaysReturns(statement.statement());
        }
        return false;
    }

    private static String describe(NameKind kind) {
        return switch (kind) {
            case PROGRAM -> "nombre del programa";
            case GLOBAL_VARIABLE -> "variable global";
            case FUNCTION -> "función";
        };
    }

    private void error(Token token, String message) {
        errors.semantic(token.getLine(), token.getCharPositionInLine(), message);
    }
}
