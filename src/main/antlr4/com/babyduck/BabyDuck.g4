grammar BabyDuck;

// =====================================================
//  PARSER (temporal)
//  Regla provisional para probar solo el lexer.
//  Se reemplazará por la gramática completa.
// =====================================================
start : .*? EOF ;

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

// --- Ignorados ---
WS : [ \t\r\n]+ -> skip ;
