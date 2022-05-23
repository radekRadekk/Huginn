grammar Huginn;

prog
    : statement*
    ;

statement
    : 'DECLARE' (INTEGER_NAME | REAL_NAME) ID (INTEGER | REAL) ';'      #declaration
    | 'ASSIGN' ID (INTEGER | REAL | ID | expression) ';'                #assignment
    | 'SELECT' (INTEGER | REAL | ID) ';'                                #print
    | 'READ_TO' ID ';'                                                  #read
    ;

expression
    : '[' expression_base ']'
    ;

expression_base
    : (ID | INTEGER | REAL) ADD (ID | INTEGER | REAL ) #expression_base_add
    | (ID | INTEGER | REAL) MUL (ID | INTEGER | REAL ) #expression_base_mul
    | (ID | INTEGER | REAL) SUB (ID | INTEGER | REAL ) #expression_base_sub
    | (ID | INTEGER | REAL) DIV (ID | INTEGER | REAL ) #expression_base_div
    ;

INTEGER
    : ('0'..'9')+
    ;

REAL
    : ('0'..'9')+'.'('0'..'9')+
    ;

INTEGER_NAME
    : 'INTEGER'
    ;

REAL_NAME
    : 'REAL'
    ;

ID
    : ('a'..'z' | '_')+
    ;

ADD
    : '+'
    ;

SUB
    : '-'
    ;

MUL
    : '*'
    ;

DIV
    : '/'
    ;

EQ
    : '==='
    ;

GT
    : '>'
    ;

LT
    : '<'
    ;

COMMENT
    : '//' ~ [\r\n]* -> skip
    ;

WS
    : [ \t\r\n] -> skip
    ;