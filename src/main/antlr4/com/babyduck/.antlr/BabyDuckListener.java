// Generated from /Users/mariamartinez/Documents/Resource/Semestre9/Desarrollo/Java/compilador/src/main/antlr4/com/babyduck/BabyDuck.g4 by ANTLR 4.13.1
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link BabyDuckParser}.
 */
public interface BabyDuckListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(BabyDuckParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(BabyDuckParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(BabyDuckParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(BabyDuckParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#relOp}.
	 * @param ctx the parse tree
	 */
	void enterRelOp(BabyDuckParser.RelOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#relOp}.
	 * @param ctx the parse tree
	 */
	void exitRelOp(BabyDuckParser.RelOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterExp(BabyDuckParser.ExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitExp(BabyDuckParser.ExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#term}.
	 * @param ctx the parse tree
	 */
	void enterTerm(BabyDuckParser.TermContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#term}.
	 * @param ctx the parse tree
	 */
	void exitTerm(BabyDuckParser.TermContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#factor}.
	 * @param ctx the parse tree
	 */
	void enterFactor(BabyDuckParser.FactorContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#factor}.
	 * @param ctx the parse tree
	 */
	void exitFactor(BabyDuckParser.FactorContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#cte}.
	 * @param ctx the parse tree
	 */
	void enterCte(BabyDuckParser.CteContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#cte}.
	 * @param ctx the parse tree
	 */
	void exitCte(BabyDuckParser.CteContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#funcCall}.
	 * @param ctx the parse tree
	 */
	void enterFuncCall(BabyDuckParser.FuncCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#funcCall}.
	 * @param ctx the parse tree
	 */
	void exitFuncCall(BabyDuckParser.FuncCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#body}.
	 * @param ctx the parse tree
	 */
	void enterBody(BabyDuckParser.BodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#body}.
	 * @param ctx the parse tree
	 */
	void exitBody(BabyDuckParser.BodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(BabyDuckParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(BabyDuckParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#assign}.
	 * @param ctx the parse tree
	 */
	void enterAssign(BabyDuckParser.AssignContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#assign}.
	 * @param ctx the parse tree
	 */
	void exitAssign(BabyDuckParser.AssignContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(BabyDuckParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(BabyDuckParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#cycle}.
	 * @param ctx the parse tree
	 */
	void enterCycle(BabyDuckParser.CycleContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#cycle}.
	 * @param ctx the parse tree
	 */
	void exitCycle(BabyDuckParser.CycleContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#print}.
	 * @param ctx the parse tree
	 */
	void enterPrint(BabyDuckParser.PrintContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#print}.
	 * @param ctx the parse tree
	 */
	void exitPrint(BabyDuckParser.PrintContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#printItem}.
	 * @param ctx the parse tree
	 */
	void enterPrintItem(BabyDuckParser.PrintItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#printItem}.
	 * @param ctx the parse tree
	 */
	void exitPrintItem(BabyDuckParser.PrintItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#read}.
	 * @param ctx the parse tree
	 */
	void enterRead(BabyDuckParser.ReadContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#read}.
	 * @param ctx the parse tree
	 */
	void exitRead(BabyDuckParser.ReadContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#returnStmt}.
	 * @param ctx the parse tree
	 */
	void enterReturnStmt(BabyDuckParser.ReturnStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#returnStmt}.
	 * @param ctx the parse tree
	 */
	void exitReturnStmt(BabyDuckParser.ReturnStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#vars}.
	 * @param ctx the parse tree
	 */
	void enterVars(BabyDuckParser.VarsContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#vars}.
	 * @param ctx the parse tree
	 */
	void exitVars(BabyDuckParser.VarsContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#varDecl}.
	 * @param ctx the parse tree
	 */
	void enterVarDecl(BabyDuckParser.VarDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#varDecl}.
	 * @param ctx the parse tree
	 */
	void exitVarDecl(BabyDuckParser.VarDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(BabyDuckParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(BabyDuckParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#funcDecl}.
	 * @param ctx the parse tree
	 */
	void enterFuncDecl(BabyDuckParser.FuncDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#funcDecl}.
	 * @param ctx the parse tree
	 */
	void exitFuncDecl(BabyDuckParser.FuncDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#funcType}.
	 * @param ctx the parse tree
	 */
	void enterFuncType(BabyDuckParser.FuncTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#funcType}.
	 * @param ctx the parse tree
	 */
	void exitFuncType(BabyDuckParser.FuncTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#params}.
	 * @param ctx the parse tree
	 */
	void enterParams(BabyDuckParser.ParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#params}.
	 * @param ctx the parse tree
	 */
	void exitParams(BabyDuckParser.ParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link BabyDuckParser#param}.
	 * @param ctx the parse tree
	 */
	void enterParam(BabyDuckParser.ParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link BabyDuckParser#param}.
	 * @param ctx the parse tree
	 */
	void exitParam(BabyDuckParser.ParamContext ctx);
}