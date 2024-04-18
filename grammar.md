program -> definition program2 .
program2 -> program .
program2 -> .

definition -> fun IDENTIFIER op parameters cp definition2 .
definition -> var IDENTIFIER eq initializers .
definition2 -> .
definition2 -> assign statements .

parameters -> IDENTIFIER parameters2 .
parameters -> .
parameters2 -> comma IDENTIFIER parameters2 .
parameters2 -> .

statements -> statement statements2 .
statements2 -> comma statements .
statements2 -> .
statement -> expression statement2 .
statement -> if expression then statements statementIfElse .
statement -> while expression do statements end .
statement -> let statementDef in statements end .
statement2 -> eq expression .
statement2 -> .
statementIfElse -> else statements end .
statementIfElse -> end .
statementDef -> definition statementDef2 .
statementDef2 -> statementDef .
statementDef2 -> .

expression -> INTCONST expression3 .
expression -> CHARCONST expression3 .
expression -> STRINGONST expression3 .
expression -> IDENTIFIER expression2 expression3 .
expression -> prefixOperator expression .
expression -> op expression cp expression3 .
expression2 -> op arguments cp .
expression2 -> .
expression3 -> postfixOperator .
expression3 -> binaryOperator expression .
expression3 -> .

arguments -> expression arguments2 .
arguments -> .
arguments2 -> comma expression arguments2 .
arguments2 -> .

initializers -> initializer initializers2 .
initializers -> .
initializers2 -> comma initializer initializers2 .
initializers2 -> .

initializer -> INTCONST initializer2 .
initializer -> CHARCONST .
initializer -> STRINGCONST .
initializer2 -> asterisk initializer3 .
initializer2 -> .

initializer3 -> INTCONST .
initializer3 -> CHARCONST .
initializer3 -> STRINGCONST .
