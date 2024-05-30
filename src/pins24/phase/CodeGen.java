package pins24.phase;

import java.util.*;
import pins24.common.*;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {
			private int varCount = 0;

			private AST.Expr leftValue = null;
			private boolean isMemAddr = false;

			private List<List<AST.Def>> varDefsByFun = new Vector<>();
			private int funCount = 0;

			Map<String, Integer> funNameCount = new HashMap<>();

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.AssignStmt assignStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();
				code.addAll(assignStmt.srcExpr.accept(this, arg));
				leftValue = assignStmt.dstExpr;
				code.addAll(assignStmt.dstExpr.accept(this, arg));
				code.addLast(new PDM.SAVE(attrAST.attrLoc.get(assignStmt)));
				leftValue = null;
				attrAST.attrCode.put(assignStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.IfStmt ifStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();

				int counter = labelCounter++;

				code.addAll(ifStmt.cond.accept(this, arg));
				code.addLast(new PDM.NAME("else:" + counter, attrAST.attrLoc.get(ifStmt)));
				code.addLast(new PDM.NAME("then:" + counter, attrAST.attrLoc.get(ifStmt)));
				code.addLast(new PDM.CJMP(attrAST.attrLoc.get(ifStmt)));

				code.addLast(new PDM.LABEL("then:" + counter, attrAST.attrLoc.get(ifStmt)));
				code.addAll(ifStmt.thenStmts.accept(this, arg));
				code.addLast(new PDM.NAME("end:" + counter, attrAST.attrLoc.get(ifStmt)));
				code.addLast(new PDM.UJMP(attrAST.attrLoc.get(ifStmt)));

				code.addLast(new PDM.LABEL("else:" + counter, attrAST.attrLoc.get(ifStmt)));
				code.addAll(ifStmt.elseStmts.accept(this, arg));

				code.addLast(new PDM.LABEL("end:" + counter, attrAST.attrLoc.get(ifStmt)));
				attrAST.attrCode.put(ifStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.WhileStmt whileStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();

				int counter = labelCounter++;

				code.addLast(new PDM.LABEL("while:" + counter, attrAST.attrLoc.get(whileStmt)));
				code.addAll(whileStmt.cond.accept(this, arg));

				code.addLast(new PDM.NAME("end:" + counter, attrAST.attrLoc.get(whileStmt)));
				code.addLast(new PDM.NAME("do:" + counter, attrAST.attrLoc.get(whileStmt)));
				code.addLast(new PDM.CJMP(attrAST.attrLoc.get(whileStmt)));

				code.addLast(new PDM.LABEL("do:" + counter, attrAST.attrLoc.get(whileStmt)));
				code.addAll(whileStmt.stmts.accept(this, arg));
				code.addLast(new PDM.NAME("while:" + counter, attrAST.attrLoc.get(whileStmt)));
				code.addLast(new PDM.UJMP(attrAST.attrLoc.get(whileStmt)));

				code.addLast(new PDM.LABEL("end:" + counter, attrAST.attrLoc.get(whileStmt)));

				labelCounter++;
				attrAST.attrCode.put(whileStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.LetStmt letStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();
				for (AST.Node def : letStmt.defs)
					if (!(def instanceof AST.FunDef))
						code.addAll(def.accept(this, arg));

				code.addAll(letStmt.stmts.accept(this, arg));

				for (AST.Node def : letStmt.defs)
					if (def instanceof AST.FunDef)
						def.accept(this, arg);

				attrAST.attrCode.put(letStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.UnExpr unExpr, final Mem.Frame arg) {
				// todo: hopefully only MEMADDR needs work
				List<PDM.CodeInstr> code = new Vector<>();
				PDM.CodeInstr lastInstr = null;
				switch (unExpr.oper) {
					case NOT -> lastInstr = new PDM.OPER(PDM.OPER.Oper.NOT, attrAST.attrLoc.get(unExpr));
					case SUB -> lastInstr = new PDM.OPER(PDM.OPER.Oper.NEG, attrAST.attrLoc.get(unExpr));
					case VALUEAT -> {
						if (leftValue != unExpr)
							lastInstr = new PDM.LOAD(attrAST.attrLoc.get(unExpr));
					}
					case MEMADDR -> isMemAddr = true;
					case ADD -> { /* do nothing */ }
				}
				code.addAll(unExpr.expr.accept(this, arg));
				if (lastInstr != null)
					code.addLast(lastInstr);
				attrAST.attrCode.put(unExpr, code);

				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.BinExpr binExpr, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();

				code.addAll(binExpr.fstExpr.accept(this, arg));
				code.addAll(binExpr.sndExpr.accept(this, arg));

				PDM.OPER.Oper oper = switch (binExpr.oper) {
					case ADD -> PDM.OPER.Oper.ADD;
					case SUB -> PDM.OPER.Oper.SUB;
					case MUL -> PDM.OPER.Oper.MUL;
					case DIV -> PDM.OPER.Oper.DIV;
					case MOD -> PDM.OPER.Oper.MOD;
					case AND -> PDM.OPER.Oper.AND;
					case OR -> PDM.OPER.Oper.OR;
					case EQU -> PDM.OPER.Oper.NEQ;
					case NEQ -> PDM.OPER.Oper.EQU;
					case LTH -> PDM.OPER.Oper.GEQ;
					case GTH -> PDM.OPER.Oper.LEQ;
					case LEQ -> PDM.OPER.Oper.GTH;
					case GEQ -> PDM.OPER.Oper.LTH;
				};
				code.addLast(new PDM.OPER(oper, attrAST.attrLoc.get(binExpr)));
				attrAST.attrCode.put(binExpr, code);

				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.VarExpr varExpr, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();

				AST.Def varDef = attrAST.attrDef.get(varExpr);

				// todo: what if variable is string? <- send value of first character

				if (attrAST.attrVarAccess.get(varDef) instanceof Mem.AbsAccess) {
					// if variable is absolute
					code.addLast(new PDM.NAME(varExpr.name, attrAST.attrLoc.get(varExpr)));
				} else {
					// if variable is relative or parameter
					code.addLast(new PDM.REGN(PDM.REGN.Reg.FP, attrAST.attrLoc.get(varExpr)));

					if (!varDefsByFun.isEmpty()) {
						out: for (int i = funCount - 1; i >= 0; i--) {
							for (AST.Def var : varDefsByFun.get(i)) {
								if (var == varDef)
									break out;
							}
							code.addLast(new PDM.LOAD(attrAST.attrLoc.get(varExpr)));
						}
					}

                    if (attrAST.attrVarAccess.get(varDef) instanceof Mem.RelAccess)
						code.addLast(new PDM.PUSH(((Mem.RelAccess) attrAST.attrVarAccess.get(varDef)).offset, attrAST.attrLoc.get(varExpr)));
					else
						code.addLast(new PDM.PUSH((attrAST.attrParAccess.get(varDef)).offset, attrAST.attrLoc.get(varExpr)));

					code.addLast(new PDM.OPER(PDM.OPER.Oper.ADD, attrAST.attrLoc.get(varExpr)));
				}

				// Load value of variable if we are not in assign
				if (leftValue != varExpr) {
					if (isMemAddr)
						isMemAddr = false;
					else
						code.addLast(new PDM.LOAD(attrAST.attrLoc.get(varExpr)));
				}

				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.CallExpr callExpr, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();
				AST.Def funDef = attrAST.attrDef.get(callExpr);

				String funName = funDef.name;
				if (funNameCount.getOrDefault(funName, 0) > 0)
					funName += "_" + funNameCount.get(funName);

				// accept arguments from right to left
				for (int i = callExpr.args.size() - 1; i >= 0; i--)
					code.addAll(callExpr.args.get(i).accept(this, arg));
				code.addLast(new PDM.REGN(PDM.REGN.Reg.FP, attrAST.attrLoc.get(callExpr)));
				code.addLast(new PDM.NAME(funName, attrAST.attrLoc.get(callExpr)));
				code.addLast(new PDM.CALL(attrAST.attrFrame.get(funDef), attrAST.attrLoc.get(callExpr)));
				attrAST.attrCode.put(callExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.FunDef funDef, final Mem.Frame arg) {
				Vector<PDM.CodeInstr> code = new Vector<>();
				Report.Locatable funDefLoc = attrAST.attrLoc.get(funDef);
				Mem.Frame funFrame = attrAST.attrFrame.get(funDef);

				String funName = funDef.name;
				if (funNameCount.getOrDefault(funName, 0) > 0)
					funName += "_" + funNameCount.get(funName);
				funNameCount.put(funDef.name, funNameCount.getOrDefault(funDef.name, 0) + 1);

				varDefsByFun.addLast(new Vector<>());
				funCount++;

				// accept parameters
				funDef.pars.accept(this, arg);

				// setup function
				code.addLast(new PDM.LABEL(funName, funDefLoc));
				// push size of variables without frame pointer and return address
				code.addLast(new PDM.PUSH(8 - funFrame.varsSize, null));
				code.addLast(new PDM.POPN(funDefLoc));

				// accept statements
				code.addAll(funDef.stmts.accept(this, arg));

				// finish function
				// push size of parameters without stack pointer
				code.addLast(new PDM.PUSH(funFrame.parsSize - 4, funDefLoc));
				code.addLast(new PDM.RETN(funFrame, funDefLoc));

				attrAST.attrCode.put(funDef, code);

				funCount--;
				varDefsByFun.remove(funCount);

				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.VarDef varDef, final Mem.Frame arg) {
				if (attrAST.attrData.get(varDef) != null)
					return attrAST.attrCode.get(varDef);

				List<PDM.CodeInstr> code = new Vector<>();
				List<PDM.DataInstr> data = new Vector<>();
				Report.Locatable varDefLoc = attrAST.attrLoc.get(varDef);

				if (attrAST.attrVarAccess.get(varDef) instanceof Mem.AbsAccess) {
					// absolute access data
					data.addLast(new PDM.LABEL(varDef.name, varDefLoc));
					data.addLast(new PDM.SIZE(attrAST.attrVarAccess.get(varDef).size, varDefLoc));

					// absolute access code
					code.addLast(new PDM.NAME(varDef.name, varDefLoc));
				} else {
					// relative access code
					code.addLast(new PDM.REGN(PDM.REGN.Reg.FP, varDefLoc));
					code.addLast(new PDM.PUSH(((Mem.RelAccess) attrAST.attrVarAccess.get(varDef)).offset, varDefLoc));
					code.addLast(new PDM.OPER(PDM.OPER.Oper.ADD, varDefLoc));
					varDefsByFun.get(funCount - 1).add(varDef);
				}

				code.addLast(new PDM.NAME(":" + varCount, varDefLoc));
				code.addLast(new PDM.INIT(varDefLoc));

				// data for inits
				data.addLast(new PDM.LABEL(":" + varCount, varDefLoc));
				for (Integer i : attrAST.attrVarAccess.get(varDef).inits) {
					data.addLast(new PDM.DATA(i, varDefLoc));
				}

				attrAST.attrCode.put(varDef, code);
				attrAST.attrData.put(varDef, data);
				varCount++;

				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.ParDef parDef, final Mem.Frame arg) {
				varDefsByFun.get(funCount - 1).add(parDef);
				return null;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.AtomExpr atomExpr, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();
				List<PDM.DataInstr> data = new Vector<>();
				if (atomExpr.type == AST.AtomExpr.Type.STRCONST) {
					data.addLast(new PDM.LABEL(":" + varCount, attrAST.attrLoc.get(atomExpr)));
					for (Integer i : Memory.decodeStrConst(atomExpr, attrAST.attrLoc.get(atomExpr))) {
						data.addLast(new PDM.DATA(i, attrAST.attrLoc.get(atomExpr)));
					}
					attrAST.attrData.put(atomExpr, data);
					code.addLast(new PDM.NAME(":" + varCount, attrAST.attrLoc.get(atomExpr)));
					varCount++;
				} else {
					code.addLast(new PDM.PUSH(Integer.valueOf(atomExpr.value), attrAST.attrLoc.get(atomExpr)));
				}
				attrAST.attrCode.put(atomExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.ExprStmt exprStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = exprStmt.expr.accept(this, arg);
				attrAST.attrCode.put(exprStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.Nodes<? extends AST.Node> nodes, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new Vector<>();
				for (AST.Node node : nodes) {
					List<PDM.CodeInstr> nodeCode = node.accept(this, arg);
					if (nodeCode != null)
						code.addAll(nodeCode);
				}
				return code;
			}
		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
					codeInitSegment.addAll(code);
					break;
				}
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (code generation):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
				// generiranje kode:
				final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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
