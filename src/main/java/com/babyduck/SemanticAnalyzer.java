package com.babyduck;

import com.babyduck.intermediate.Operand;
import com.babyduck.intermediate.Outcome;
import com.babyduck.intermediate.QuadrupleGenerator;
import com.babyduck.intermediate.QuadrupleQueue;
import com.babyduck.intermediate.TypedOperand;
import com.babyduck.semantic.FunctionDirectory;
import com.babyduck.semantic.FunctionDirectory.NameKind;
import com.babyduck.semantic.FunctionInfo;
import com.babyduck.semantic.Op;
import com.babyduck.semantic.SemanticCube;
import com.babyduck.semantic.Type;
import com.babyduck.semantic.VariableInfo;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

/**
 * Recorrido semántico del árbol sintáctico (Visitor), en una sola pasada
 *
 * Llena el directorio de funciones y las tablas de variables, y detecta
 * declaraciones repetidas. También valida el uso: variables y funciones
 * no declaradas, llamadas, asignaciones, condiciones, expresiones y return
 *
 * Además genera los cuádruplos de las expresiones y los estatutos lineales con
 * {@link QuadrupleGenerator}. Las expresiones no devuelven su tipo: dejan su resultado
 * (operando y tipo) en las pilas del generador, y quien las usa lo saca de ahí.
 * Si una parte de una expresión ya es ERROR, ese error ya se reportó y no se reporta otro
 *
 * Se usa un Visitor, aunque ya no devuelva tipos, porque permite acciones a mitad de
 * una regla (necesarias para los saltos de if y while en la siguiente entrega)
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

    // Programa

    @Override
    public Void visitProgram(BabyDuckParser.ProgramContext ctx) {
        directory.setProgramName(ctx.ID().getText());
        boolean hasFunctions = !ctx.funcDecl().isEmpty();
        if (hasFunctions) {
            generator.note("aquí irá Goto a main");
        }
        if (ctx.vars() != null) {
            visit(ctx.vars());                                           // globales
        }
        for (BabyDuckParser.FuncDeclContext function : ctx.funcDecl()) {
            visit(function);
        }
        currentFunction = null;                                          // main solo ve globales
        if (hasFunctions) {
            generator.note("inicio de main: aquí se rellenará el Goto a main");
        }
        visit(ctx.body());
        generator.emitEnd();                                             // fin del programa
        return null;                                                     // las tablas se conservan
    }

    // Variables

    /** {@code _A_, _B_ : int;} — el tipo ya está en el mismo nodo que los nombres */
    @Override
    public Void visitVarDecl(BabyDuckParser.VarDeclContext ctx) {
        Type type = Type.fromKeyword(ctx.type().getText());
        for (TerminalNode id : ctx.ID()) {
            declareVariable(id.getSymbol(), type, false);
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

        // Una función con valor debe regresar en todos los caminos. Se revisa antes del
        // cuerpo porque solo inspecciona el árbol, y así el error sale en orden de línea
        if (returnType != Type.VOID && !bodyAlwaysReturns(ctx.body())) {
            error(nameToken, "la función " + name + " no regresa un valor en todos los caminos");
        }

        currentFunction = function;
        if (ctx.params() != null) {
            visit(ctx.params());
        }
        if (ctx.vars() != null) {
            visit(ctx.vars());                                           // variables locales
        }
        generator.note("inicio de la función " + name);
        visit(ctx.body());
        generator.note("fin de la función " + name + ": aquí irá ENDFUNC");
        currentFunction = null;                                          // la tabla se conserva
        return null;
    }

    /** {@code _X_ : int} dentro de los paréntesis de una función */
    @Override
    public Void visitParam(BabyDuckParser.ParamContext ctx) {
        Type type = Type.fromKeyword(ctx.type().getText());
        declareVariable(ctx.ID().getSymbol(), type, true);
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
        return visitChildren(ctx);
    }

    /**
     * {@code _X_ = expresión;} — destino ← valor
     * El destino no entra a las pilas (no es operando de una expresión): se valida y se
     * conserva; después de la expresión se saca el valor, se revisa con el cubo y se genera =
     */
    @Override
    public Void visitAssign(BabyDuckParser.AssignContext ctx) {
        Token target = ctx.ID().getSymbol();
        Operand destination = variableOperand(target);
        visit(ctx.expression());
        Token assign = ctx.ASSIGN().getSymbol();
        Outcome outcome = generator.generateAssign(destination, assign.getLine(), assign.getCharPositionInLine());
        if (outcome.failed()) {
            error(assign, "no se puede asignar " + outcome.right().display()
                    + " a " + target.getText() + ", que es " + outcome.left().display());
        }
        return null;
    }

    /** Los saltos llegan en la siguiente entrega; por ahora se marcan con notas */
    @Override
    public Void visitCondition(BabyDuckParser.ConditionContext ctx) {
        TypedOperand condition = checkCondition(ctx.expression(), "if");
        generator.note("if (línea " + ctx.IF().getSymbol().getLine() + "): aquí irá GotoF "
                + condition.operand());
        visit(ctx.body(0));
        if (ctx.ELSE() != null) {
            generator.note("else: aquí irá Goto al final del if y se rellenará el GotoF");
            visit(ctx.body(1));
            generator.note("fin del if: aquí se rellenará el Goto");
        } else {
            generator.note("fin del if: aquí se rellenará el GotoF");
        }
        return null;
    }

    @Override
    public Void visitCycle(BabyDuckParser.CycleContext ctx) {
        generator.note("inicio del while (línea " + ctx.WHILE().getSymbol().getLine()
                + "): destino del Goto de regreso");
        TypedOperand condition = checkCondition(ctx.expression(), "while");
        generator.note("aquí irá GotoF " + condition.operand());
        visit(ctx.body());
        generator.note("fin del while: aquí irá Goto al inicio y se rellenará el GotoF");
        return null;
    }

    /** {@code read(_X_);} — solo puede leer en una variable declarada */
    @Override
    public Void visitRead(BabyDuckParser.ReadContext ctx) {
        generator.emitRead(variableOperand(ctx.ID().getSymbol()));
        return null;
    }

    /** {@code print(e1, e2, ...);} — un PRINT por elemento, en orden, y un PRINTLN al final */
    @Override
    public Void visitPrint(BabyDuckParser.PrintContext ctx) {
        for (BabyDuckParser.PrintItemContext item : ctx.printItem()) {
            if (item.CTE_STRING() != null) {
                generator.emitPrint(generator.string(item.CTE_STRING().getText()));
            } else {
                // acepta BOOL; una función void dentro de print ya la reporta checkCall
                generator.emitPrint(evaluate(item.expression()).operand());
            }
        }
        generator.emitPrintln();
        return null;
    }

    /** Llamada usada como estatuto ({@code _F_(2);}); si regresa un valor, se descarta */
    @Override
    public Void visitFuncCall(BabyDuckParser.FuncCallContext ctx) {
        checkCall(ctx, false);
        return null;
    }

    /** Reglas de return; el cuádruplo RETURN llega con las funciones */
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
            generator.note("return: aquí irá el regreso de " + name);
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
        }
        generator.note("return: aquí irá RETURN " + result.operand());
        return null;
    }

    // Expresiones: dejan su resultado en las pilas. Después de cada operando se revisa
    // el tope de la pila de operadores para decidir si se genera el cuádruplo

    /** {@code exp (relOp exp)?} */
    @Override
    public Void visitExpression(BabyDuckParser.ExpressionContext ctx) {
        visit(ctx.exp(0));
        if (ctx.relOp() != null) {
            pushOperator(ctx.relOp().getStart());
            visit(ctx.exp(1));
            if (generator.topIs(Op.GT, Op.LT, Op.EQ, Op.NEQ)) {
                report(generator.generateBinary());
            }
        }
        return null;
    }

    /** {@code term ((+|-) term)*}, de izquierda a derecha */
    @Override
    public Void visitExp(BabyDuckParser.ExpContext ctx) {
        for (int i = 0; i < ctx.term().size(); i++) {
            if (i > 0) {
                pushOperator(operatorBefore(ctx, i));
            }
            visit(ctx.term(i));
            if (generator.topIs(Op.PLUS, Op.MINUS)) {
                report(generator.generateBinary());
            }
        }
        return null;
    }

    /** {@code factor ((*|/) factor)*}, de izquierda a derecha */
    @Override
    public Void visitTerm(BabyDuckParser.TermContext ctx) {
        for (int i = 0; i < ctx.factor().size(); i++) {
            if (i > 0) {
                pushOperator(operatorBefore(ctx, i));
            }
            visit(ctx.factor(i));
            if (generator.topIs(Op.MULT, Op.DIV)) {
                report(generator.generateBinary());
            }
        }
        return null;
    }

    /** {@code (+|-)? ( (expresión) | llamada | _Id_ | constante )} */
    @Override
    public Void visitFactor(BabyDuckParser.FactorContext ctx) {
        TerminalNode sign = (ctx.MINUS() != null) ? ctx.MINUS() : ctx.PLUS();

        if (ctx.cte() != null) {
            pushConstant(ctx.cte(), sign);                               // el signo va con la constante
            return null;
        }
        if (ctx.expression() != null) {
            generator.pushFakeBottom();                                  // (
            visit(ctx.expression());
            generator.popFakeBottom();                                   // )
        } else if (ctx.funcCall() != null) {
            checkCall(ctx.funcCall(), true);
        } else {
            generator.pushOperand(variableOperand(ctx.ID().getSymbol()));
        }

        if (sign != null) {
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
        VariableInfo variable = new VariableInfo(name, type);

        if (currentFunction == null) {                                   // global
            NameKind use = directory.globalUse(name);
            if (use == NameKind.GLOBAL_VARIABLE) {
                error(token, "la variable " + name + " ya fue declarada");
            } else if (use != null) {
                error(token, name + " ya se usa como " + describe(use));
            } else {
                directory.globals().add(variable);
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
        if (isParam) {
            currentFunction.addParam(variable);
        } else {
            currentFunction.addLocal(variable);
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
        generator.pushFakeBottom();
        visit(argument);
        generator.popFakeBottom();
        return generator.popOperand();
    }

    /**
     * Revisa una llamada. Los errores salen en orden: primero los del nombre,
     * después los de cada argumento. Los cuádruplos de la llamada (ERA, PARAM, GOSUB)
     * llegan con las funciones; por ahora se marcan con notas
     *
     * @param inExpression {@code true} si la llamada está dentro de una expresión: entonces
     *                     deja en las pilas un temporal con el tipo de retorno, o ERROR
     */
    private void checkCall(BabyDuckParser.FuncCallContext ctx, boolean inExpression) {
        Token nameToken = ctx.ID().getSymbol();
        String name = nameToken.getText();
        List<BabyDuckParser.ExpressionContext> args = ctx.expression();
        FunctionInfo function = directory.getFunction(name);

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

        generator.note("llamada a " + name + " (línea " + nameToken.getLine() + "): aquí irá ERA " + name);
        int expected = function.paramCount();
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
                generator.note("aquí irá PARAM " + arg.operand() + " (argumento " + (i + 1) + ")");
            }
        }

        if (!inExpression) {
            generator.note("aquí irá GOSUB " + name);
        } else if (voidValue) {
            generator.pushOperand(Operand.ERROR);
        } else {
            Operand result = generator.newTemp(function.returnType());
            generator.note("aquí irá GOSUB " + name + "; " + result + " recibirá su valor de retorno");
            generator.pushOperand(result);
        }
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
            // En main, return ya es error; no se reporta además código inalcanzable
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
