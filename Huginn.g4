grammar Huginn;

prog
    : statement* EOF
    ;

statement
    : declaration | assignment | print | read
    ;

declaration
    : DECLARE (INTEGER_NAME | REAL_NAME) assignment
    ;

assignment
    : ID ASSIGN (INTEGER | REAL) SEMICOLON
    ;

print
    : SELECT ID SEMICOLON
    ;

read
    : READ_TO ID SEMICOLON
    ;

fragment DIGIT
    : ('0' .. '9')
    ;

fragment NON_ZERO_DIGIT
    : ('1' .. '9')
    ;

fragment ZERO
    : '0'
    ;

fragment LETTER
    : ('a' .. 'z') | ('A' .. 'Z')
    ;

fragment MINUS
    : '-'
    ;

INTEGER
    : ZERO | MINUS? NON_ZERO_DIGIT DIGIT*
    ;

ID
    : LETTER+
    ;

REAL
    : ( ZERO | NON_ZERO_DIGIT DIGIT* ) '.' DIGIT+
    ;

INTEGER_NAME
    : 'INTEGER' | 'integer'
    ;

REAL_NAME
    : 'REAL' | 'real'
    ;

DECLARE
    : 'DECLARE' | 'declare'
    ;

ASSIGN
    : 'ASSIGN' | 'assign'
    ;

ADD
    : 'ADD' | 'add'
    ;

SUB
    : 'SUB' | 'sub'
    ;

MUL
    : 'MUL' | 'mul'
    ;

DIV
    : 'DIV' | 'div'
    ;

MOD
    : 'MOD' | 'mod'
    ;

SELECT
    : 'SELECT' | 'select'
    ;

READ_TO
    : 'READ_TO' | 'read_to'
    ;

SEMICOLON
    : ';'
    ;

COMMENT
    : '//' ~ [\r\n]* -> skip
    ;

WS
    : [ \t\r\n] -> skip
    ;

