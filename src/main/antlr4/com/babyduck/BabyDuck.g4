grammar BabyDuck;

// =====================================================
//  PARSER
// =====================================================

// ---------- Programa ----------
// <Programa> → program id ; <Vars_opt> <Li_funcs> main <Body> end
program    : PROGRAM ID SEMI vars? funcDecl* MAIN body END EOF ;

// ---------- Expresiones ----------
// <Expresion> → <Exp> <Rel_Expr>     <Rel_Expr> → <Opr_rel> <Exp> | ε
expression : exp (relOp exp)? ;
relOp      : GT | LT | EQ | NEQ ;

// <Exp> → <Termino> <Add_term>       <Add_term> → <Opr_add> <Termino> <Add_term> | ε
exp        : term ((PLUS | MINUS) term)* ;

// <Termino> → <Factor> <Add_Fact>    <Add_Fact> → <Opr_mult> <Factor> <Add_Fact> | ε
term       : factor ((MULT | DIV) factor)* ;

// <Factor> → ( <Expresion> ) | <F_call> | <Signo_opt> <valor>
// <Factor>    → <Signo_opt> <valor>    <valor>    → ( <Expresion> ) | <F_call> | id | <CTE>
factor : (PLUS | MINUS)? ( LPAREN expression RPAREN
                         | funcCall
                         | ID
                         | cte ) ;

cte        : CTE_INT | CTE_FLOAT ;

// <F_call> → id ( <Args_opt> )
funcCall   : ID LPAREN (expression (COMMA expression)*)? RPAREN ;

// ---------- Estatutos ----------
// <Body> → { <Li_Statement> }
body       : LBRACE statement* RBRACE ;

statement  : assign
           | condition
           | cycle
           | funcCall SEMI                    // <F_call> ;
           | print
           | read
           | returnStmt
           | LBRACKET statement* RBRACKET ;   // [ <Li_statement> ]

assign     : ID ASSIGN expression SEMI ;

// <Condition> → if ( <Expresion> ) <Statement> <Co_else> ; STATEMENT → BODY
condition  : IF LPAREN expression RPAREN body (ELSE body)? SEMI ;

// <Cycle> → while ( <Expresion> ) do <Statement> ;
cycle      : WHILE LPAREN expression RPAREN DO body SEMI ;

// <Print> → print ( <Li_print> ) ;
print      : PRINT LPAREN printItem (COMMA printItem)* RPAREN SEMI ;
printItem  : expression | CTE_STRING ;

// <Read> → read ( id ) ;
read       : READ LPAREN ID RPAREN SEMI ;

// <Return> → return <Expresion>? ;
returnStmt : RETURN expression? SEMI ;

// ---------- Declarar Variables ----------
// <Vars> → var <Declarar_var> <Decl_mas>
vars       : VAR varDecl+ ;
// <Declarar_var> → id <Mas_id> : <Type> ;
varDecl    : ID (COMMA ID)* COLON type SEMI ;

// ---------- Tipos de Datos ----------
type       : INT | FLOAT ;

// ---------- Funciones ----------
// <Funcs> → <TypeFunc> id ( <Params_opt> ) [ <Vars_opt> <Body> ] ;
funcDecl   : funcType ID LPAREN params? RPAREN LBRACKET vars? body RBRACKET SEMI ;
funcType   : VOID | type ;
params     : param (COMMA param)* ;
param      : ID COLON type ;


// =====================================================
//  LEXER
// =====================================================

// --- Palabras reservadas (antes de ID para evitar que se reconozcan como ID) ---
PROGRAM : 'program' ;
MAIN    : 'main' ;
END     : 'end' ;
VOID    : 'void' ;
VAR     : 'var' ;
PRINT   : 'print' ;
INT     : 'int' ;
FLOAT   : 'float' ;
WHILE   : 'while' ;
DO      : 'do' ;
IF      : 'if' ;
ELSE    : 'else' ;
READ    : 'read' ;
RETURN  : 'return' ;

// --- Operadores ---
PLUS   : '+' ;
MINUS  : '-' ;
MULT   : '*' ;
DIV    : '/' ;
ASSIGN : '=' ;
EQ     : '==' ;
NEQ    : '!=' ;
GT     : '>' ;
LT     : '<' ;

// --- Caracteres especiales ---
SEMI     : ';' ;
COMMA    : ',' ;
COLON    : ':' ;
LBRACE   : '{' ;
RBRACE   : '}' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;

// --- Identificadores y constantes ---
ID         : '_' [A-Z] [a-zA-Z0-9_]* '_' ;
CTE_FLOAT  : [0-9]+ '.' [0-9]+ ;
CTE_INT    : [0-9]+ ;
CTE_STRING : '"' (~["\\\r\n] | '\\' .)* '"' ;

// --- Comentarios ---
COMMENT : '//' ~[\r\n]* -> skip ;

// --- Ignorados ---
WS : [ \t\r\n]+ -> skip ;
