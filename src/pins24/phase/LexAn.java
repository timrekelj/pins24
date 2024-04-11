package pins24.phase;

import java.io.*;
import pins24.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = '\n';

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 8 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * 
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private void nextToken() {
		while (true) {
			int column = buffCharColumn;
			int line = buffCharLine;

			// Števila
			if (buffChar >= '0' && buffChar <= '9') {
				final StringBuilder num = new StringBuilder();
				while (buffChar >= '0' && buffChar <= '9') {
					num.append((char) buffChar);
					nextChar();
				}
				buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.INTCONST, num.toString());
				return;
			}

			// Imena in ključne besede
			if (
				(buffChar >= 'a' && buffChar <= 'z') ||
				(buffChar >= 'A' && buffChar <= 'Z') ||
				buffChar == '_'
			) {
				final StringBuilder name = new StringBuilder();
				while (
					(buffChar >= 'a' && buffChar <= 'z') ||
					(buffChar >= 'A' && buffChar <= 'Z') ||
					(buffChar >= '0' && buffChar <= '9') ||
					buffChar == '_'
				) {
					name.append((char) buffChar);
					nextChar();
				}
				switch (name.toString()) {
					case "fun":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.FUN, "fun");
						break;
					case "var":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.VAR, "var");
						break;
					case "if":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.IF, "if");
						break;
					case "then":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.THEN, "then");
						break;
					case "else":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.ELSE, "else");
						break;
					case "while":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.WHILE, "while");
						break;
					case "do":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.DO, "do");
						break;
					case "let":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.LET, "let");
						break;
					case "in":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.IN, "in");
						break;
					case "end":
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.END, "end");
						break;
					default:
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn - 1), Token.Symbol.IDENTIFIER, name.toString());
				}
				return;
			}


			switch (buffChar) {
				case '=':
					nextChar();
					if (buffChar == '=') {
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.EQU, "==");
						nextChar();
					} else {
						buffToken = new Token(new Report.Location(line, column), Token.Symbol.ASSIGN, "=");
					}
					return;
				case ',':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.COMMA, ",");
					nextChar();
					return;
				case '&':
					nextChar();
					if (buffChar == '&') {
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.AND, "&&");
						nextChar();
					} else {
						throw new Report.Error(new Report.Location(line, column), "Nepričakovan znak '" + (char) buffChar + "'.");
					}
					return;
				case '|':
					nextChar();
					if (buffChar == '|') {
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.OR, "||");
						nextChar();
					} else {
						throw new Report.Error(new Report.Location(line, column), "Nepričakovan znak '" + (char) buffChar + "'.");
					}
					return;
				case '!':
					nextChar();
					if (buffChar == '=') {
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.NEQ, "!=");
						nextChar();
					} else {
						buffToken = new Token(new Report.Location(line, column), Token.Symbol.NOT, "!");
					}
					return;
				case '>':
					nextChar();
					if (buffChar == '=') {
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.GEQ, ">=");
						nextChar();
					} else {
						buffToken = new Token(new Report.Location(line, column), Token.Symbol.GTH, ">");
					}
					return;
				case '<':
					nextChar();
					if (buffChar == '=') {
						buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.LEQ, "<=");
						nextChar();
					} else {
						buffToken = new Token(new Report.Location(line, column), Token.Symbol.LTH, "<");
					}
					return;
				case '%':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.MOD, "%");
					nextChar();
					return;
				case '(':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.LPAREN, "(");
					nextChar();
					return;
				case ')':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.RPAREN, ")");
					nextChar();
					return;
				case '^':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.PTR, "^");
					nextChar();
					return;
				case '+':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.ADD, "+");
					nextChar();
					return;
				case '-':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.SUB, "-");
					nextChar();
					return;
				case '*':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.MUL, "*");
					nextChar();
					return;
				case '/':
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.DIV, "/");
					nextChar();
					return;
				case '#':
					while (buffChar != '\n' && buffChar != -1)
						nextChar();
					break;
				case '"':
					final StringBuilder str = new StringBuilder();
					while (true) {
						nextChar();
						if (buffChar == '\\') {
							str.append(parseEscape(true));
							continue;
						}
						if (buffChar == '"') {
							buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.STRINGCONST, str.toString());
							nextChar();
							break;
						}
						if (buffChar == -1) {
							throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nedokončan niz.");
						}
						str.append((char) buffChar);
					}
					return;
				case '\'':
					nextChar();
					if (buffChar == -1) {
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nedokončan znak.");
					}
					char ch = (char) buffChar;
					if (ch < 32 || ch > 126) {
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljaven znak.");
					}
					if (ch == '\\') {
						ch = parseEscape(false);
					}
					nextChar();
					if (buffChar != '\'') {
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nedokončan znak.");
					}
					buffToken = new Token(new Report.Location(line, column, buffCharLine, buffCharColumn), Token.Symbol.CHARCONST, Character.toString(ch));
					nextChar();
					return;
				case ' ': case '\t': case '\n':
					nextChar();
					break;
				case -1:
					buffToken = null;
					return;
				default:
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nepričakovan znak: '" + (char) buffChar + "'.");
			}
		}
	}

	private char parseEscape(boolean isString) {
		// TODO: finish this
		nextChar();
		char ch;
		if (buffChar == -1) {
			throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nedokončan znak.");
		}
		if ((buffChar >= '0' && buffChar <= '9') || (buffChar >= 'A' && buffChar <= 'F')) {
			String hexChar = String.valueOf((char)buffChar);
			nextChar();
			if (buffChar == -1) {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nedokončan znak.");
			}
			if ((buffChar >= '0' && buffChar <= '9') || (buffChar >= 'A' && buffChar <= 'F')) {
				hexChar += (char)buffChar;
				ch = (char) Integer.parseInt(hexChar, 16);
			} else {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljaven escape znak.");
			}
		} else {
			if (
				isString && buffChar == '\"' || // če je escape v stringu in naslednji znak "
				!isString && buffChar == '\''   // če je escape v char-u in naslednji znak '
			) {
				ch = (char) buffChar;
			} else {
				switch (buffChar) {
					case '\\':
						ch = '\\';
						break;
					case 'n':
						ch = '\n';
						break;
					default:
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljaven escape znak.");
				}
			}
		}

		return ch;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken() != null)
					System.out.println(lexAn.takeToken());
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
