package pins24.phase;

import pins24.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

    /** Leksikalni analizator. */
    private final LexAn lexAn;

    /**
     * Ustvari nov sintaksni analizator.
     *
     * @param srcFileName ime izvorne datoteke.
     */
    public SynAn(final String srcFileName) {
        this.lexAn = new LexAn(srcFileName);
    }

    @Override
    public void close() {
        lexAn.close();
    }

    /**
     * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
     * je prave vrste.
     *
     * @param symbol Pricakovana vrsta leksikalnega simbola.
     * @return Prevzeti leksikalni simbol.
     */
    public Token check(Token.Symbol symbol) {
        final Token token = lexAn.takeToken();
        if (token.symbol() != symbol)
            throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
        return token;
    }

    /**
     * Opravi sintaksno analizo.
     */
    public void parse() {
        parseProgram();
        if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
            throw new Report.Error(lexAn.peekToken(), "Unexpected text '" + lexAn.peekToken().lexeme() + "...'.");
    }

    /**
     * Opravi sintaksno analizo celega programa.
     */
    private void parseProgram() {
        switch (lexAn.peekToken().symbol()) {
            case FUN:
            case VAR:
                parseDefinition();
                parseProgram2();
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "A definition expected.");
        }
    }

    private void parseProgram2() {
        switch (lexAn.peekToken().symbol()) {
            case FUN:
            case VAR:
                parseProgram();
            default:
                // epsilon
        }
    }

    private void parseDefinition() {
        switch (lexAn.peekToken().symbol()) {
            case FUN:
                check(Token.Symbol.FUN);
                check(Token.Symbol.IDENTIFIER);
                check(Token.Symbol.LPAREN);
                parseParameters();
                check(Token.Symbol.RPAREN);
                parseDefinition2();
                return;
            case VAR:
                check(Token.Symbol.VAR);
                check(Token.Symbol.IDENTIFIER);
                check(Token.Symbol.ASSIGN);
                parseInitializers();
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "A definition expected.");
        }
    }

    private void parseDefinition2() {
        switch (lexAn.peekToken().symbol()) {
            case ASSIGN:
                check(Token.Symbol.ASSIGN);
                parseStatements();
                return;
            default:
                // epsilon
        }
    }

    private void parseParameters() {
        switch (lexAn.peekToken().symbol()) {
            case IDENTIFIER:
                check(Token.Symbol.IDENTIFIER);
                parseParameters2();
                return;
            default:
                // epsilon
        }
    }

    private void parseParameters2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA:
                check(Token.Symbol.COMMA);
                check(Token.Symbol.IDENTIFIER);
                parseParameters2();
                return;
            default:
                // epsilon
        }
    }

    private void parseStatements() {
        switch (lexAn.peekToken().symbol()) {
            // statement
            case IF:
            case WHILE:
            case LET:
            // expression
            case IDENTIFIER:
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
            case LPAREN:
            // expression prefix: [!, +, -, ^]
            case NOT:
            case ADD:
            case SUB:
            case PTR:
                parseStatement();
                parseStatements2();
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "Statement expected.");
        }
    }

    private void parseStatements2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA:
                check(Token.Symbol.COMMA);
                parseStatements();
                return;
            default:
                // epsilon
        }
    }

    private void parseStatement() {
        switch (lexAn.peekToken().symbol()) {
            // expression
            case IDENTIFIER:
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
            case LPAREN:
            case NOT:
            case ADD:
            case SUB:
            case PTR:
                parseExpression();
                parseStatement2();
                return;
            case IF:
                check(Token.Symbol.IF);
                parseExpression();
                check(Token.Symbol.THEN);
                parseStatements();
                parseStatementsIfElse();
                return;
            case WHILE:
                check(Token.Symbol.WHILE);
                parseExpression();
                check(Token.Symbol.DO);
                parseStatements();
                check(Token.Symbol.END);
                return;
            case LET:
                check(Token.Symbol.LET);
                parseStatementDef();
                check(Token.Symbol.IN);
                parseStatements();
                check(Token.Symbol.END);
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "A statement expected.");
        }
    }

    private void parseStatement2() {
        switch (lexAn.peekToken().symbol()) {
            case ASSIGN:
                check(Token.Symbol.ASSIGN);
                parseExpression();
                return;
            default:
                // epsilon
        }
    }

    private void parseStatementsIfElse() {
        switch (lexAn.peekToken().symbol()) {
            case ELSE:
                check(Token.Symbol.ELSE);
                parseStatements();
                check(Token.Symbol.END);
                return;
            case END:
                check(Token.Symbol.END);
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "An else or end expected.");
        }
    }

    private void parseStatementDef() {
        switch (lexAn.peekToken().symbol()) {
            case FUN:
            case VAR:
                parseDefinition();
                parseStatementDef2();
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "A variable or function definition expected.");
        }
    }
    private void parseStatementDef2() {
        switch (lexAn.peekToken().symbol()) {
            case FUN:
            case VAR:
                parseStatementDef();
                return;
            default:
                // epsilon
        }
    }

    private void parseExpression() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
                check(lexAn.peekToken().symbol());
                parseExpression3();
                return;
            case IDENTIFIER:
                check(Token.Symbol.IDENTIFIER);
                parseExpression2();
                parseExpression3();
                return;
            case LPAREN:
                check(Token.Symbol.LPAREN);
                parseExpression();
                check(Token.Symbol.RPAREN);
                parseExpression3();
                return;
            // prefix operator [!, +, -, ^]
            case NOT:
            case ADD:
            case SUB:
            case PTR:
                check(lexAn.peekToken().symbol());
                parseExpression();
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "An expression expected.");
        }
    }

    private void parseNonAssociativeExpression() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
                check(lexAn.peekToken().symbol());
                return;
            case IDENTIFIER:
                check(Token.Symbol.IDENTIFIER);
                parseExpression2();
                return;
            case LPAREN:
                check(Token.Symbol.LPAREN);
                parseExpression();
                check(Token.Symbol.RPAREN);
                return;
            // prefix operator [!, +, -, ^]
            case NOT:
            case ADD:
            case SUB:
            case PTR:
                check(lexAn.peekToken().symbol());
                parseExpression();
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "An expression expected.");
        }
    }

    private void parseExpression2() {
        switch (lexAn.peekToken().symbol()) {
            case LPAREN:
                check(Token.Symbol.LPAREN);
                parseArguments();
                check(Token.Symbol.RPAREN);
                return;
            default:
                // epsilon
        }
    }
    private void parseExpression3() {
        switch (lexAn.peekToken().symbol()) {
            // binary operator
            // associative operators [&&, ||, +, -, *, /, %]
            case AND:
            case OR:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
                check(lexAn.peekToken().symbol());
                parseExpression();
                return;
            // non-associative operators [==, !=, >, <, >=, <=]
            case EQU:
            case NEQ:
            case GTH:
            case LTH:
            case GEQ:
            case LEQ:
                check(lexAn.peekToken().symbol());
                parseNonAssociativeExpression();
                return;
            // postfix operator [^]
            case PTR:
                check(Token.Symbol.PTR);
                return;
            default:
                // epsilon
        }
    }
    private void parseArguments() {
        switch (lexAn.peekToken().symbol()) {
            // expression
            case IDENTIFIER:
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
            case LPAREN:
            case NOT:
            case ADD:
            case SUB:
            case PTR:
                parseExpression();
                parseArguments2();
                return;
            default:
                // epsilon
        }
    }

    private void parseArguments2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA:
                check(Token.Symbol.COMMA);
                parseExpression();
                parseArguments2();
                return;
            default:
                // epsilon
        }
    }

    private void parseInitializers() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
                parseInitializer();
                parseInitializers2();
                return;
            default:
                // epsilon
        }
    }

    private void parseInitializers2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA:
                check(Token.Symbol.COMMA);
                parseInitializer();
                parseInitializers2();
                return;
            default:
                // epsilon
        }
    }

    private void parseInitializer() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
                check(lexAn.peekToken().symbol());
                parseInitializer2();
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "An initializer expected.");
        }
    }

    private void parseInitializer2() {
        switch (lexAn.peekToken().symbol()) {
            case MUL:
                check(Token.Symbol.MUL);
                parseInitializer3();
                return;
            default:
                // epsilon
        }
    }

    private void parseInitializer3() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST:
            case CHARCONST:
            case STRINGCONST:
                check(lexAn.peekToken().symbol());
                return;
            default:
                throw new Report.Error(lexAn.peekToken(), "An initializer expected.");
        }
    }

    // --- ZAGON ---

    /**
     * Zagon sintaksnega analizatorja kot samostojnega programa.
     *
     * @param cmdLineArgs Argumenti v ukazni vrstici.
     */
    public static void main(final String[] cmdLineArgs) {
        System.out.println("This is PINS'24 compiler (syntax analysis):");

        try {
            if (cmdLineArgs.length == 0)
                throw new Report.Error("No source file specified in the command line.");
            if (cmdLineArgs.length > 1)
                Report.warning("Unused arguments in the command line.");

            try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
                synAn.parse();
            }

            // Upajmo, da kdaj pridemo to te tocke.
            // A zavedajmo se sledecega:
            // 1. Prevod je zaradi napak v programu lahko napacen :-o
            // 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
            Report.info("Done.");
        } catch (Report.Error error) {
            // Izpis opisa napake.
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

}
