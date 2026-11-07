package com.babyduck;

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
 * Los métodos de expresiones devuelven su tipo; los de declaraciones y estatutos devuelven null
 * Si una parte de una expresión ya es ERROR, ese error ya se reportó y no se reporta otro
 */
public class SemanticAnalyzer extends BabyDuckBaseVisitor<Type> {

    private final ErrorCollector errors;
    private final FunctionDirectory directory = new FunctionDirectory();
    private final SemanticCube cube = new SemanticCube();

    /** Función cuyo cuerpo se está analizando; {@code null} = alcance global / main */
    private FunctionInfo currentFunction;

    public SemanticAnalyzer(ErrorCollector errors) {
        this.errors = errors;
    }

    public FunctionDirectory directory() {
        return directory;
    }

    // Programa

    @Override
    public Type visitProgram(BabyDuckParser.ProgramContext ctx) {
        directory.setProgramName(ctx.ID().getText());
        if (ctx.vars() != null) {
            visit(ctx.vars());                                           // globales
        }
        for (BabyDuckParser.FuncDeclContext function : ctx.funcDecl()) {
            visit(function);
        }
        currentFunction = null;                                          // main solo ve globales
        visit(ctx.body());
        return null;                                                     // las tablas se conservan
    }

    // Variables

    /** {@code _A_, _B_ : int;} — el tipo ya está en el mismo nodo que los nombres */
    @Override
    public Type visitVarDecl(BabyDuckParser.VarDeclContext ctx) {
        Type type = Type.fromKeyword(ctx.type().getText());
        for (TerminalNode id : ctx.ID()) {
            declareVariable(id.getSymbol(), type, false);
        }
        return null;
    }

    // Funciones

    @Override
    public Type visitFuncDecl(BabyDuckParser.FuncDeclContext ctx) {
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
        visit(ctx.body());
        currentFunction = null;                                          // la tabla se conserva
        return null;
    }

    /** {@code _X_ : int} dentro de los paréntesis de una función */
    @Override
    public Type visitParam(BabyDuckParser.ParamContext ctx) {
        Type type = Type.fromKeyword(ctx.type().getText());
        declareVariable(ctx.ID().getSymbol(), type, true);
        return null;
    }

    // Estatutos

    /** {@code { estatutos }}: cuerpo de una función o de main, if, else y while */
    @Override
    public Type visitBody(BabyDuckParser.BodyContext ctx) {
        visitStatements(ctx.statement());
        return null;
    }

    /** El bloque {@code [ estatutos ]} se revisa igual que un cuerpo; el resto, por su propio método */
    @Override
    public Type visitStatement(BabyDuckParser.StatementContext ctx) {
        if (ctx.LBRACKET() != null) {
            visitStatements(ctx.statement());
            return null;
        }
        return visitChildren(ctx);
    }

    /** {@code _X_ = expresión;} — destino ← valor */
    @Override
    public Type visitAssign(BabyDuckParser.AssignContext ctx) {
        Token target = ctx.ID().getSymbol();
        Type targetType = variableType(target);
        Type valueType = visit(ctx.expression());
        if (targetType == Type.ERROR || valueType == Type.ERROR) {
            return null;                                                 // ya se reportó
        }
        if (!cube.canAssign(targetType, valueType)) {
            error(ctx.ASSIGN().getSymbol(), "no se puede asignar " + valueType.display()
                    + " a " + target.getText() + ", que es " + targetType.display());
        }
        return null;
    }

    @Override
    public Type visitCondition(BabyDuckParser.ConditionContext ctx) {
        checkCondition(ctx.expression(), "if");
        visit(ctx.body(0));
        if (ctx.ELSE() != null) {
            visit(ctx.body(1));
        }
        return null;
    }

    @Override
    public Type visitCycle(BabyDuckParser.CycleContext ctx) {
        checkCondition(ctx.expression(), "while");
        visit(ctx.body());
        return null;
    }

    /** {@code read(_X_);} — solo puede leer en una variable declarada */
    @Override
    public Type visitRead(BabyDuckParser.ReadContext ctx) {
        variableType(ctx.ID().getSymbol());
        return null;
    }

    /** Llamada usada como estatuto ({@code _F_(2);}); si regresa un valor, se descarta */
    @Override
    public Type visitFuncCall(BabyDuckParser.FuncCallContext ctx) {
        checkCall(ctx, false);
        return null;
    }

    /** Reglas de return */
    @Override
    public Type visitReturnStmt(BabyDuckParser.ReturnStmtContext ctx) {
        Token keyword = ctx.RETURN().getSymbol();
        BabyDuckParser.ExpressionContext value = ctx.expression();

        if (currentFunction == null) {
            error(keyword, "return solo puede usarse dentro de una función");
            if (value != null) visit(value);                             // errores dentro del valor
            return null;
        }

        String name = currentFunction.name();
        Type returnType = currentFunction.returnType();
        if (returnType == Type.VOID) {
            if (value != null) {
                error(keyword, "la función " + name + " es void, no puede regresar un valor");
                visit(value);
            }
            return null;                                                 // return; en void es válido
        }
        if (value == null) {
            error(keyword, "la función " + name + " debe regresar un valor " + returnType.display());
            return null;
        }
        Type valueType = visit(value);
        if (valueType != Type.ERROR && !cube.canAssign(returnType, valueType)) {
            error(value.getStart(), "la función " + name + " regresa " + returnType.display()
                    + ", no puede regresar " + valueType.display());
        }
        return null;
    }

    // print no necesita método propio: sus expresiones se revisan solas (visitChildren),
    // acepta BOOL y una función void dentro de print ya la reporta checkCall

    // Expresiones: cada método devuelve el tipo de lo que analizó

    /** {@code exp (relOp exp)?} */
    @Override
    public Type visitExpression(BabyDuckParser.ExpressionContext ctx) {
        Type left = visit(ctx.exp(0));
        if (ctx.relOp() == null) {
            return left;
        }
        Type right = visit(ctx.exp(1));
        Token op = ctx.relOp().getStart();
        return binary(left, Op.fromSymbol(op.getText()), right, op);
    }

    /** {@code term ((+|-) term)*}, de izquierda a derecha */
    @Override
    public Type visitExp(BabyDuckParser.ExpContext ctx) {
        Type result = visit(ctx.term(0));
        for (int i = 1; i < ctx.term().size(); i++) {
            Token op = operatorBefore(ctx, i);
            Type right = visit(ctx.term(i));
            result = binary(result, Op.fromSymbol(op.getText()), right, op);
        }
        return result;
    }

    /** {@code factor ((*|/) factor)*}, de izquierda a derecha */
    @Override
    public Type visitTerm(BabyDuckParser.TermContext ctx) {
        Type result = visit(ctx.factor(0));
        for (int i = 1; i < ctx.factor().size(); i++) {
            Token op = operatorBefore(ctx, i);
            Type right = visit(ctx.factor(i));
            result = binary(result, Op.fromSymbol(op.getText()), right, op);
        }
        return result;
    }

    /** {@code (+|-)? ( (expresión) | llamada | _Id_ | constante )} */
    @Override
    public Type visitFactor(BabyDuckParser.FactorContext ctx) {
        Type type;
        if (ctx.expression() != null) {
            type = visit(ctx.expression());
        } else if (ctx.funcCall() != null) {
            type = checkCall(ctx.funcCall(), true);
        } else if (ctx.ID() != null) {
            type = variableType(ctx.ID().getSymbol());
        } else {
            type = (ctx.cte().CTE_INT() != null) ? Type.INT : Type.FLOAT;
        }

        TerminalNode sign = (ctx.MINUS() != null) ? ctx.MINUS() : ctx.PLUS();
        if (sign == null || type == Type.ERROR) {
            return type;
        }
        Type result = cube.unaryResult(Op.fromSymbol(sign.getText()), type);
        if (result == Type.ERROR) {
            error(sign.getSymbol(), "no se puede aplicar el signo " + sign.getText()
                    + " a " + type.display());
        }
        return result;
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
     * Tipo de un nombre usado como variable. Si no es una variable visible,
     * reporta qué es en realidad y devuelve ERROR
     */
    private Type variableType(Token token) {
        String name = token.getText();
        VariableInfo variable = directory.lookupVariable(name, currentFunction);
        if (variable != null) {
            return variable.type();
        }
        NameKind use = directory.globalUse(name);
        if (use == NameKind.FUNCTION) {
            error(token, name + " es una función, no una variable");
        } else if (use == NameKind.PROGRAM) {
            error(token, name + " es el nombre del programa");
        } else {
            error(token, "la variable " + name + " no está declarada");
        }
        return Type.ERROR;
    }

    /**
     * Revisa una llamada. Los errores salen en orden: primero los del nombre,
     * después los de cada argumento.
     *
     * @param inExpression {@code true} si la llamada está dentro de una expresión (su valor se usa)
     * @return el tipo de retorno de la función, o ERROR si la llamada no puede dar un valor
     */
    private Type checkCall(BabyDuckParser.FuncCallContext ctx, boolean inExpression) {
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
            args.forEach(this::visit);                                   // errores dentro de los argumentos
            return Type.ERROR;
        }

        Type result = function.returnType();
        if (inExpression && result == Type.VOID) {
            error(nameToken, "la función " + name + " es void y no devuelve un valor");
            result = Type.ERROR;
        }

        int expected = function.paramCount();
        if (args.size() != expected) {
            // Un solo error por llamada: con otro número de argumentos no se comparan los tipos
            error(nameToken, name + " espera " + expected
                    + (expected == 1 ? " argumento" : " argumentos") + ", recibió " + args.size());
            args.forEach(this::visit);
            return result;
        }

        for (int i = 0; i < expected; i++) {
            Type argType = visit(args.get(i));
            Type paramType = function.params().get(i).type();
            if (argType != Type.ERROR && !cube.canAssign(paramType, argType)) {
                error(args.get(i).getStart(), "argumento " + (i + 1) + " de " + name
                        + ": no se puede pasar " + argType.display() + " a " + paramType.display());
            }
        }
        return result;
    }

    /** La condición de if/while debe ser BOOL */
    private void checkCondition(BabyDuckParser.ExpressionContext condition, String statement) {
        Type type = visit(condition);
        if (type != Type.ERROR && type != Type.BOOL) {
            error(condition.getStart(), "la condición del " + statement
                    + " debe ser una comparación (bool), se obtuvo " + type.display());
        }
    }

    /** Consulta el cubo para {@code left op right}; si un operando ya es ERROR no reporta otra vez */
    private Type binary(Type left, Op op, Type right, Token opToken) {
        if (left == Type.ERROR || right == Type.ERROR) {
            return Type.ERROR;
        }
        Type result = cube.result(left, op, right);
        if (result == Type.ERROR) {
            // "y" cambia a "e" antes de una palabra que suena a "i" (bool e int)
            String and = right.display().startsWith("i") ? " e " : " y ";
            error(opToken, "no se puede aplicar " + op.symbol() + " entre "
                    + left.display() + and + right.display());
        }
        return result;
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
     * código inalcanzable: se reporta una vez y se siguen revisando todos
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