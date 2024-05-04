package pins24.phase;

import pins24.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

    private HashMap<AST.Node, Report.Locatable> attrLoc;

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
    public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
        this.attrLoc = attrLoc;
        final AST.Nodes<AST.MainDef> defs = parseProgram();
        if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
            throw new Report.Error(lexAn.peekToken(), "Unexpected text '" + lexAn.peekToken().lexeme() + "...'.");
        return defs;
    }



    /**
     * Opravi sintaksno analizo celega programa.
     *
     * @return
     */
    private AST.Nodes<AST.MainDef> parseProgram() {
        switch (lexAn.peekToken().symbol()) {
            case FUN, VAR -> {
                List<AST.MainDef> defs = new ArrayList<>();
                defs.add(parseDefinition());
                defs.addAll(parseProgram2());
                return new AST.Nodes<>(defs);
            }
            default -> throw new Report.Error(lexAn.peekToken(), "A definition expected.");
        }
    }

    private List<AST.MainDef> parseProgram2() {
        switch (lexAn.peekToken().symbol()) {
            case FUN, VAR -> { return parseProgram().getAll(); }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private AST.MainDef parseDefinition() {
        switch (lexAn.peekToken().symbol()) {
            case FUN -> {
                Token fun = check(Token.Symbol.FUN);
                Token funName = check(Token.Symbol.IDENTIFIER);
                check(Token.Symbol.LPAREN);
                List<AST.ParDef> pars = parseParameters();
                Token rp = check(Token.Symbol.RPAREN);
                List<AST.Stmt> stmts = parseDefinition2();
                AST.FunDef funDef = new AST.FunDef(funName.lexeme(), pars, stmts);
                if (!stmts.isEmpty()) {
                    attrLoc.put(
                            funDef,
                            new Report.Location(
                                    fun.location().begLine(),
                                    fun.location().begColumn(),
                                    attrLoc.get(stmts.getLast()).location().endLine(),
                                    attrLoc.get(stmts.getLast()).location().endColumn()
                            )
                    );
                } else {
                    attrLoc.put(
                        funDef,
                        new Report.Location(
                            fun.location().begLine(),
                            fun.location().begColumn(),
                            rp.location().endLine(),
                            rp.location().endColumn()
                        )
                    );
                }
                return funDef;
            }
            case VAR -> {
                Token var = check(Token.Symbol.VAR);
                Token varName = check(Token.Symbol.IDENTIFIER);
                Token assign = check(Token.Symbol.ASSIGN);
                List<AST.Init> inits = parseInitializers();
                Report.Location endLocation = assign.location();

                if (!inits.isEmpty())
                    endLocation = attrLoc.get(inits.getLast()).location();
                else {
                    AST.AtomExpr atomExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "0");
                    attrLoc.put(atomExpr, new Report.Location(0, 0));
                    AST.Init init = new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), atomExpr);
                    attrLoc.put(init, new Report.Location(0, 0));
                    inits.add(init);
                }

                AST.VarDef varDef = new AST.VarDef(varName.lexeme(), inits);
                attrLoc.put(
                    varDef,
                    new Report.Location(
                        var.location().begLine(),
                        var.location().begColumn(),
                        endLocation.endLine(),
                        endLocation.endColumn()
                    )
                );
                return varDef;
            }
            default -> throw new Report.Error(lexAn.peekToken(), "A definition expected.");
        }
    }

    private List<AST.Stmt> parseDefinition2() {
        switch (lexAn.peekToken().symbol()) {
            case ASSIGN -> {
                check(Token.Symbol.ASSIGN);
                return parseStatements();
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private List<AST.ParDef> parseParameters() {
        switch (lexAn.peekToken().symbol()) {
            case IDENTIFIER -> {
                Token parToken = check(Token.Symbol.IDENTIFIER);
                AST.ParDef parDef = new AST.ParDef(parToken.lexeme());
                attrLoc.put(parDef, parToken.location());

                List<AST.ParDef> pars = new ArrayList<>();
                pars.add(parDef);
                List<AST.ParDef> morePrams = parseParameters2();
                pars.addAll(morePrams);

                return pars;
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private List<AST.ParDef> parseParameters2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA -> {
                check(Token.Symbol.COMMA);
                Token parToken = check(Token.Symbol.IDENTIFIER);
                AST.ParDef parDef = new AST.ParDef(parToken.lexeme());
                attrLoc.put(parDef, parToken.location());

                List<AST.ParDef> pars = new ArrayList<>();
                pars.add(parDef);
                List<AST.ParDef> morePrams = parseParameters2();
                pars.addAll(morePrams);

                return pars;
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private List<AST.Stmt> parseStatements() {
        switch (lexAn.peekToken().symbol()) {
            //  | statement     | expression                                          | expression prefix: [!, +, -, ^]
            case IF, WHILE, LET, IDENTIFIER, INTCONST, CHARCONST, STRINGCONST, LPAREN, NOT, ADD, SUB, PTR -> {
                List<AST.Stmt> stmts = new ArrayList<>();
                stmts.add(parseStatement());
                List<AST.Stmt> moreStmts = parseStatements2();
                stmts.addAll(moreStmts);
                return stmts;
            }
            default -> throw new Report.Error(lexAn.peekToken(), "Statement expected.");
        }
    }

    private List<AST.Stmt> parseStatements2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA -> {
                check(Token.Symbol.COMMA);
                return parseStatements();
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private AST.Stmt parseStatement() {
        switch (lexAn.peekToken().symbol()) {
            // expression
            case IDENTIFIER, INTCONST, CHARCONST, STRINGCONST, LPAREN, NOT, ADD, SUB, PTR -> {
                AST.Expr dstExpr = parseExpression();
                AST.Expr srcExpr = parseStatement2();
                AST.Stmt stmt;
                if (srcExpr == null) {
                    stmt = new AST.ExprStmt(dstExpr);
                    attrLoc.put(stmt, attrLoc.get(dstExpr));
                } else {
                    stmt = new AST.AssignStmt(dstExpr, srcExpr);
                    attrLoc.put(
                        stmt,
                        new Report.Location(
                            attrLoc.get(dstExpr).location().begLine(),
                            attrLoc.get(dstExpr).location().begColumn(),
                            attrLoc.get(srcExpr).location().endLine(),
                            attrLoc.get(srcExpr).location().endColumn()
                        )
                    );
                }
                return stmt;
            }
            case IF -> {
                Token ifSymbol = check(Token.Symbol.IF);
                AST.Expr ifExpr = parseExpression();
                check(Token.Symbol.THEN);
                List<AST.Stmt> ifStmts = parseStatements();
                List<AST.Stmt> elseStmts = parseStatementsIfElse();
                Token ifEnd = check(Token.Symbol.END);
                AST.IfStmt ifStmt = new AST.IfStmt(ifExpr, ifStmts, elseStmts);
                attrLoc.put(
                    ifStmt,
                    new Report.Location(
                        ifSymbol.location().begLine(),
                        ifSymbol.location().begColumn(),
                        ifEnd.location().endLine(),
                        ifEnd.location().endColumn()
                    )
                );
                return ifStmt;
            }
            case WHILE -> {
                Token whileSymbol = check(Token.Symbol.WHILE);
                AST.Expr whileExpr = parseExpression();
                check(Token.Symbol.DO);
                List<AST.Stmt> whileStmts = parseStatements();
                Token whileEnd = check(Token.Symbol.END);
                AST.WhileStmt whileStmt = new AST.WhileStmt(whileExpr, whileStmts);
                attrLoc.put(
                    whileStmt,
                    new Report.Location(
                        whileSymbol.location().begLine(),
                        whileSymbol.location().begColumn(),
                        whileEnd.location().endLine(),
                        whileEnd.location().endColumn()
                    )
                );
                return whileStmt;
            }
            case LET -> {
                Token letSymbol = check(Token.Symbol.LET);
                List<AST.MainDef> stmtDef = parseStatementDef();
                check(Token.Symbol.IN);
                List<AST.Stmt> letStmts = parseStatements();
                Token letEnd = check(Token.Symbol.END);
                AST.LetStmt letStmt = new AST.LetStmt(stmtDef, letStmts);
                attrLoc.put(
                    letStmt,
                    new Report.Location(
                        letSymbol.location().begLine(),
                        letSymbol.location().begColumn(),
                        letEnd.location().endLine(),
                        letEnd.location().endColumn()
                    )
                );
                return letStmt;
            }
            default -> throw new Report.Error(lexAn.peekToken(), "A statement expected.");
        }
    }

    private AST.Expr parseStatement2() {
        switch (lexAn.peekToken().symbol()) {
            case ASSIGN -> {
                check(Token.Symbol.ASSIGN);
                return parseExpression();
            }
            // epsilon
            default -> { return null; }
        }
    }

    private List<AST.Stmt> parseStatementsIfElse() {
        switch (lexAn.peekToken().symbol()) {
            case ELSE -> {
                check(Token.Symbol.ELSE);
                return parseStatements();
            }
            case END -> { return new ArrayList<>(); }
            default -> throw new Report.Error(lexAn.peekToken(), "An else or end expected.");
        }
    }

    private List<AST.MainDef> parseStatementDef() {
        switch (lexAn.peekToken().symbol()) {
            case FUN, VAR -> {
                List<AST.MainDef> defs = new ArrayList<>();
                defs.add(parseDefinition());
                defs.addAll(parseStatementDef2());
                return defs;
            }
            default -> throw new Report.Error(lexAn.peekToken(), "A variable or function definition expected.");
        }
    }
    private List<AST.MainDef> parseStatementDef2() {
        switch (lexAn.peekToken().symbol()) {
            case FUN, VAR -> {
                return parseStatementDef();
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private AST.Expr parseExpression() {
        return parseOrExpr();
    }

    private AST.Expr parseOrExpr() {
        return parseOrExpr2(parseAndExpr());
    }

    private AST.Expr parseOrExpr2(AST.Expr leftExpr) {
        switch (lexAn.peekToken().symbol()) {
            case OR -> {
                check(Token.Symbol.OR);
                AST.Expr rightExpr = parseAndExpr();
                AST.Expr resExpr = new AST.BinExpr(AST.BinExpr.Oper.OR, leftExpr, rightExpr);
                attrLoc.put(
                    resExpr,
                    new Report.Location(
                        attrLoc.get(leftExpr).location().begLine(),
                        attrLoc.get(leftExpr).location().begColumn(),
                        attrLoc.get(rightExpr).location().endLine(),
                        attrLoc.get(rightExpr).location().endColumn()
                    )
                );
                return parseOrExpr2(resExpr);
            }
            // epsilon
            default -> { return leftExpr; }
        }
    }

    private AST.Expr parseAndExpr() {
        return parseAndExpr2(parseCompareExpr());
    }

    private AST.Expr parseAndExpr2(AST.Expr leftExpr) {
        switch (lexAn.peekToken().symbol()) {
            case AND -> {
                check(Token.Symbol.AND);
                AST.Expr rightExpr = parseCompareExpr();
                AST.Expr resExpr = new AST.BinExpr(AST.BinExpr.Oper.AND, leftExpr, rightExpr);
                attrLoc.put(
                    resExpr,
                    new Report.Location(
                        attrLoc.get(leftExpr).location().begLine(),
                        attrLoc.get(leftExpr).location().begColumn(),
                        attrLoc.get(rightExpr).location().endLine(),
                        attrLoc.get(rightExpr).location().endColumn()
                    )
                );
                return parseAndExpr2(resExpr);
            }
            // epsilon
            default -> { return leftExpr; }
        }
    }

    private AST.Expr parseCompareExpr() {
        return parseCompareExpr2(parseAddExpr());
    }

    private AST.Expr parseCompareExpr2(AST.Expr leftExpr) {
        switch (lexAn.peekToken().symbol()) {
            case EQU, NEQ, LTH, GTH, LEQ, GEQ -> {
                Token compare = check(lexAn.peekToken().symbol());
                AST.Expr rightExpr = parseAddExpr();
                AST.Expr resExpr = new AST.BinExpr(getExpressionOperand(compare), leftExpr, rightExpr);
                attrLoc.put(
                    resExpr,
                    new Report.Location(
                        attrLoc.get(leftExpr).location().begLine(),
                        attrLoc.get(leftExpr).location().begColumn(),
                        attrLoc.get(rightExpr).location().endLine(),
                        attrLoc.get(rightExpr).location().endColumn()
                    )
                );
                return resExpr;
            }
            // epsilon
            default -> { return leftExpr; }
        }
    }

    private AST.Expr parseAddExpr() {
        return parseAddExpr2(parseMulExpr());
    }

    private AST.Expr parseAddExpr2(AST.Expr leftExpr) {
        switch (lexAn.peekToken().symbol()) {
            case ADD, SUB -> {
                Token addSub = check(lexAn.peekToken().symbol());
                AST.Expr rightExpr = parseMulExpr();
                AST.Expr resExpr = new AST.BinExpr(getExpressionOperand(addSub), leftExpr, rightExpr);
                attrLoc.put(
                    resExpr,
                    new Report.Location(
                        attrLoc.get(leftExpr).location().begLine(),
                        attrLoc.get(leftExpr).location().begColumn(),
                        attrLoc.get(rightExpr).location().endLine(),
                        attrLoc.get(rightExpr).location().endColumn()
                    )
                );
                return parseAddExpr2(resExpr);
            }
            // epsilon
            default -> { return leftExpr; }
        }
    }

    private AST.Expr parseMulExpr() {
        return parseMulExpr2(parsePrefixExpr());
    }

    private AST.Expr parseMulExpr2(AST.Expr leftExpr) {
        switch (lexAn.peekToken().symbol()) {
            case MUL, DIV, MOD -> {
                Token mulDivMod = check(lexAn.peekToken().symbol());
                AST.Expr rightExpr = parsePrefixExpr();
                AST.Expr resExpr = new AST.BinExpr(getExpressionOperand(mulDivMod), leftExpr, rightExpr);
                attrLoc.put(
                    resExpr,
                    new Report.Location(
                        attrLoc.get(leftExpr).location().begLine(),
                        attrLoc.get(leftExpr).location().begColumn(),
                        attrLoc.get(rightExpr).location().endLine(),
                        attrLoc.get(rightExpr).location().endColumn()
                    )
                );
                return parseMulExpr2(resExpr);
            }
            // epsilon
            default -> { return leftExpr; }
        }
    }

    private AST.Expr parsePrefixExpr() {
        switch (lexAn.peekToken().symbol()) {
            case NOT, ADD, SUB, PTR -> {
                Token prefix = check(lexAn.peekToken().symbol());
                AST.Expr expr = parsePrefixExpr();
                AST.Expr resExpr = new AST.UnExpr(getPrefixOperand(prefix), expr);
                attrLoc.put(
                    resExpr,
                    new Report.Location(
                        prefix.location().begLine(),
                        prefix.location().begColumn(),
                        attrLoc.get(expr).location().endLine(),
                        attrLoc.get(expr).location().endColumn()
                    )
                );
                return resExpr;
            }
            default -> { return parsePostfixExpr(); }
        }
    }

    private AST.Expr parsePostfixExpr() {
        AST.Expr expr = parsePrimaryExpr();
        for (Token ptr : parsePostfixExpr2()) {
            AST.Expr ptrExpr = new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, expr);
            attrLoc.put(
                ptrExpr,
                new Report.Location(
                    attrLoc.get(expr).location().begLine(),
                    attrLoc.get(expr).location().begColumn(),
                    ptr.location().endLine(),
                    ptr.location().endColumn()
                )
            );
            return ptrExpr;
        }
        return expr;
    }

    private List<Token> parsePostfixExpr2() {
        switch (lexAn.peekToken().symbol()) {
            case PTR -> {
                List<Token> ptrs = new ArrayList<>();
                ptrs.add(check(Token.Symbol.PTR));
                ptrs.addAll(parsePostfixExpr2());
                return ptrs;
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private AST.Expr parsePrimaryExpr() {
        switch (lexAn.peekToken().symbol()) {
            case IDENTIFIER -> {
                Token id = check(Token.Symbol.IDENTIFIER);
                return parsePrimaryExpr2(id);
            }
            case INTCONST, CHARCONST, STRINGCONST -> {
                Token constToken = check(lexAn.peekToken().symbol());
                AST.AtomExpr.Type type = getExpressionType(constToken);
                AST.AtomExpr atomExpr = new AST.AtomExpr(type, constToken.lexeme());
                attrLoc.put(atomExpr, constToken.location());
                return atomExpr;
            }
            case LPAREN -> {
                check(Token.Symbol.LPAREN);
                AST.Expr expr = parseExpression();
                check(Token.Symbol.RPAREN);
                return expr;
            }
            default -> throw new Report.Error(lexAn.peekToken(), "An expression expected.");
        }
    }

    private AST.Expr parsePrimaryExpr2(Token id) {
        switch (lexAn.peekToken().symbol()) {
            // call expression
            case LPAREN -> {
                check(Token.Symbol.LPAREN);
                List<AST.Expr> arguments = parseArguments();
                Token rparen = check(Token.Symbol.RPAREN);
                AST.CallExpr callExpr = new AST.CallExpr(id.lexeme(), arguments);
                attrLoc.put(
                    callExpr,
                    new Report.Location(
                        id.location().begLine(),
                        id.location().begColumn(),
                        rparen.location().endLine(),
                        rparen.location().endColumn()
                    )
                );
                return callExpr;
            }
            // variable expression
            default -> {
                AST.VarExpr varExpr = new AST.VarExpr(id.lexeme());
                attrLoc.put(varExpr, id.location());
                return varExpr;
            }
        }
    }

    private List<AST.Expr> parseArguments() {
        switch (lexAn.peekToken().symbol()) {
            // expression
            case IDENTIFIER, INTCONST, CHARCONST, STRINGCONST, LPAREN, NOT, ADD, SUB, PTR -> {
                List<AST.Expr> arguments = new ArrayList<>();
                arguments.add(parseExpression());
                arguments.addAll(parseArguments2());
                return arguments;
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private List<AST.Expr> parseArguments2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA -> {
                check(Token.Symbol.COMMA);
                List<AST.Expr> arguments = new ArrayList<>();
                arguments.add(parseExpression());
                arguments.addAll(parseArguments2());
                return arguments;
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private List<AST.Init> parseInitializers() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST, CHARCONST, STRINGCONST -> {
                List<AST.Init> inits = new ArrayList<>();
                inits.add(parseInitializer());
                inits.addAll(parseInitializers2());
                return inits;
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private List<AST.Init> parseInitializers2() {
        switch (lexAn.peekToken().symbol()) {
            case COMMA -> {
                check(Token.Symbol.COMMA);
                List<AST.Init> inits = new ArrayList<>();
                inits.add(parseInitializer());
                inits.addAll(parseInitializers2());
                return inits;
            }
            // epsilon
            default -> { return new ArrayList<>(); }
        }
    }

    private AST.Init parseInitializer() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST, CHARCONST, STRINGCONST -> {
                Token init1 = check(lexAn.peekToken().symbol());
                Token init2 = parseInitializer2();
                if (init2 != null) {
                    // 3 * "hello world"
                    if (init1.symbol() != Token.Symbol.INTCONST)
                        throw new Report.Error(init1, "An integer constant expected.");
                    AST.AtomExpr.Type type2 = getExpressionType(init2);
                    AST.AtomExpr initializer = new AST.AtomExpr(type2, init2.lexeme());
                    AST.Init res = new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, init1.lexeme()), initializer);
                    attrLoc.put(initializer, init2.location());
                    attrLoc.put(
                        res,
                        new Report.Location(
                            init1.location().begLine(),
                            init1.location().begColumn(),
                            init2.location().endLine(),
                            init2.location().endColumn()
                        )
                    );
                    return res;
                } else {
                    // "hello world"
                    AST.AtomExpr.Type type1 = getExpressionType(init1);
                    AST.AtomExpr initializer = new AST.AtomExpr(type1, init1.lexeme());
                    AST.Init res = new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), initializer);
                    attrLoc.put(initializer, init1.location());
                    attrLoc.put(res, init1.location());
                    return res;
                }
            }
            default -> throw new Report.Error(lexAn.peekToken(), "An initializer expected.");
        }
    }

    private Token parseInitializer2() {
        switch (lexAn.peekToken().symbol()) {
            case MUL -> {
                check(Token.Symbol.MUL);
                return parseInitializer3();
            }
            // epsilon
            default -> { return null; }
        }
    }

    private Token parseInitializer3() {
        switch (lexAn.peekToken().symbol()) {
            case INTCONST, CHARCONST, STRINGCONST -> {
                return check(lexAn.peekToken().symbol());
            }
            default -> throw new Report.Error(lexAn.peekToken(), "An initializer expected.");
        }
    }

    private AST.AtomExpr.Type getExpressionType(Token token ) {
        return switch (token.symbol()) {
            case INTCONST -> AST.AtomExpr.Type.INTCONST;
            case CHARCONST -> AST.AtomExpr.Type.CHRCONST;
            case STRINGCONST -> AST.AtomExpr.Type.STRCONST;
            default -> throw new Report.Error(lexAn.peekToken(), "An initializer expected.");
        };
    }

    private AST.UnExpr.Oper getPrefixOperand(Token token ) {
        return switch (token.symbol()) {
            case NOT -> AST.UnExpr.Oper.NOT;
            case ADD -> AST.UnExpr.Oper.ADD;
            case SUB -> AST.UnExpr.Oper.SUB;
            case PTR -> AST.UnExpr.Oper.MEMADDR;
            default -> throw new Report.Error(lexAn.peekToken(), "An initializer expected.");
        };
    }

    private AST.BinExpr.Oper getExpressionOperand(Token token ) {
        return switch (token.symbol()) {
            case ADD -> AST.BinExpr.Oper.ADD;
            case SUB -> AST.BinExpr.Oper.SUB;
            case MUL -> AST.BinExpr.Oper.MUL;
            case DIV -> AST.BinExpr.Oper.DIV;
            case MOD -> AST.BinExpr.Oper.MOD;
            case EQU -> AST.BinExpr.Oper.EQU;
            case NEQ -> AST.BinExpr.Oper.NEQ;
            case LTH -> AST.BinExpr.Oper.LTH;
            case GTH -> AST.BinExpr.Oper.GTH;
            case LEQ -> AST.BinExpr.Oper.LEQ;
            case GEQ -> AST.BinExpr.Oper.GEQ;
            default -> throw new Report.Error(lexAn.peekToken(), "An initializer expected.");
        };
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
                synAn.parse(new HashMap<>());
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
