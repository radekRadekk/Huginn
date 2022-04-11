grammar Huginn;

prog
    : statement*
    ;

statement
    : 'DECLARE' (INTEGER_NAME | REAL_NAME) ID (INTEGER | REAL) ';'      #declaration
    | 'ASSIGN' ID (INTEGER | REAL | ID) ';'                             #assignment
    | 'SELECT' (INTEGER | REAL | ID) ';'                                #print
    | 'READ_TO' ID ';'                                                  #read
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
    : ('a'..'z' | 'A'..'Z')+
    ;

COMMENT
    : '//' ~ [\r\n]* -> skip
    ;

WS
    : [ \t\r\n] -> skip
    ;