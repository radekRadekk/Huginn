grammar Huginn;

prog
    : base_statement*
    ;

base_statement
    : statement
    | function_def
    ;

statement
    : 'DECLARE' (INTEGER_NAME | REAL_NAME | BOOL_NAME) ID (INTEGER | REAL | BOOL) ';'   #declaration
    | 'ASSIGN' ID (INTEGER | REAL | BOOL | ID | expression) ';'                         #assignment
    | 'SELECT' (INTEGER | REAL | BOOL | ID) ';'                                         #print
    | 'READ_TO' ID ';'                                                                  #read
    | 'IF' (ID | BOOL) block ';'                                                        #if
    | 'CALL' ID '(' parameter* ')' ';'                                                  #function_call
    ;

parameter
    : ID '|'
    ;

function_def
    : ID '(' parameter_def* ')' block ';'
    ;

parameter_def
    : (INTEGER | REAL | BOOL) ID '|'
    ;

block
    : '[' statement* ']'
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

BOOL
    : ('true' | 'false')
    ;

INTEGER_NAME
    : 'INTEGER'
    ;

REAL_NAME
    : 'REAL'
    ;

BOOL_NAME
    : 'BOOL'
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