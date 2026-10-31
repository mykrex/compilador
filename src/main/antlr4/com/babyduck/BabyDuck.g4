grammar BabyDuck;

// ======================
//  PARSER
// ======================

// ---------- Programa ----------
program    : PROGRAM ID SEMI vars? funcDecl* MAIN body END EOF ;

// ---------- Expresiones ----------
expression : exp (relOp exp)? ;
relOp      : GT | LT | EQ | NEQ ;

exp        : term ((PLUS | MINUS) term)* ;

term       : factor ((MULT | DIV) factor)* ;

factor : (PLUS | MINUS)? ( LPAREN expression RPAREN
                         | funcCall
                         | ID
                         | cte ) ;

cte        : CTE_INT | CTE_FLOAT ;

funcCall   : ID LPAREN (expression (COMMA expression)*)? RPAREN ;

// ---------- Estatutos ----------
body       : LBRACE statement* RBRACE ;

statement  : assign
           | condition
           | cycle
           | funcCall SEMI
           | print
           | read
           | returnStmt
           | LBRACKET statement* RBRACKET ;

assign     : ID ASSIGN expression SEMI ;

condition  : IF LPAREN expression RPAREN body (ELSE body)? SEMI ;

cycle      : WHILE LPAREN expression RPAREN DO body SEMI ;

print      : PRINT LPAREN printItem (COMMA printItem)* RPAREN SEMI ;
printItem  : expression | CTE_STRING ;

read       : READ LPAREN ID RPAREN SEMI ;

returnStmt : RETURN expression? SEMI ;

// ---------- Declarar Variables ----------
vars       : VAR varDecl+ ;
varDecl    : ID (COMMA ID)* COLON type SEMI ;

// ---------- Tipos de Datos ----------
type       : INT | FLOAT ;

// ---------- Funciones ----------
funcDecl   : funcType ID LPAREN params? RPAREN LBRACKET vars? body RBRACKET SEMI ;
funcType   : VOID | type ;
params     : param (COMMA param)* ;
param      : ID COLON type ;


// ======================
//  LEXER
// ======================

// Palabras reservadas
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

// Operadores
PLUS   : '+' ;
MINUS  : '-' ;
MULT   : '*' ;
DIV    : '/' ;
ASSIGN : '=' ;
EQ     : '==' ;
NEQ    : '!=' ;
GT     : '>' ;
LT     : '<' ;

// Caracteres especiales
SEMI     : ';' ;
COMMA    : ',' ;
COLON    : ':' ;
LBRACE   : '{' ;
RBRACE   : '}' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;

// Identificadores y constantes
ID         : '_' [A-Z] [a-zA-Z0-9_]* '_' ;
CTE_FLOAT  : [0-9]+ '.' [0-9]+ ;
CTE_INT    : [0-9]+ ;
CTE_STRING : '"' (~["\\\r\n] | '\\' .)* '"' ;

// Comentarios
COMMENT : '//' ~[\r\n]* -> skip ;

// Ignorados
WS : [ \t\r\n]+ -> skip ;
