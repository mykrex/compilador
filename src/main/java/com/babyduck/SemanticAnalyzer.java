package com.babyduck;

import com.babyduck.semantic.FunctionDirectory;
import com.babyduck.semantic.FunctionDirectory.NameKind;
import com.babyduck.semantic.FunctionInfo;
import com.babyduck.semantic.Type;
import com.babyduck.semantic.VariableInfo;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Recorrido semántico del árbol sintáctico (Visitor), en una sola pasada
 *
 * Llena el directorio de funciones y las tablas de variables, y detecta
 * declaraciones repetidas. Las validaciones de uso (variables y funciones
 * no declaradas, llamadas, asignaciones, condiciones y return)
 *
 */
public class SemanticAnalyzer extends BabyDuckBaseVisitor<Type> {

    private final ErrorCollector errors;
    private final FunctionDirectory directory = new FunctionDirectory();   // ①

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