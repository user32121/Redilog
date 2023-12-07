package redilog.parsing;

import java.util.Set;
import java.util.regex.Pattern;

public class Token {
    enum Type {
        KEYWORD,
        VARIABLE,
        NUMBER,
        SYMBOL,
        EOF,
    }

    public static final Set<String> KEYWORDS = Set.of("input", "output", "assign");
    public static final Set<String> MULTICHAR_SYMBOLS = Set.of();

    public static final Pattern WORD = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    public static final Pattern POSITIVE_NUMBER = Pattern.compile("0x[0-9a-fA-F]+|0o[0-8]+|0b[01]+|[0-9]+");
    public static final Pattern NUMBER = Pattern.compile("-?0x[0-9a-fA-F]+|-?0o[0-8]+|-?0b[01]+|-?[0-9]+");

    private final String value;
    private final Type type;
    private final int line;
    private final int column;

    public static Token EOF(int line, int column) {
        return new Token("<EOF>", Type.EOF, line, column);
    }

    public static Token word(String value, int line, int column) {
        return new Token(value, KEYWORDS.contains(value) ? Type.KEYWORD : Type.VARIABLE, line, column);
    }

    public Token(String value, Type type, int line, int column) {
        this.value = value;
        this.type = type;
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getValue() {
        return value;
    }

    public String getValue(Token.Type requiredType) throws RedilogParsingException {
        if (type != requiredType) {
            throw new RedilogParsingException(
                    String.format("Expected %s but got %s", requiredType, this));
        }
        return value;
    }

    public Type getType() {
        return type;
    }

    /**
     * Require a token to have a certain value
     * @param expected any acceptable values
     * @throws RedilogParsingException
     */
    public void require(Type requiredType, String... expected) throws RedilogParsingException {
        if (type == requiredType) {
            for (String s : expected) {
                if (value.equals(s)) {
                    return;
                }
            }
        }
        if (expected.length == 1) {
            throw new RedilogParsingException(
                    String.format("Expected \"%s\" but got %s", expected[0], this));
        } else {
            throw new RedilogParsingException(
                    String.format("Expected one of [\"%s\"] but got %s", String.join("\" \"", expected), this));
        }
    }

    /**
     * Attempts to parse the token as an integer.
     * Accepts binary (prefixed with 0b), octal (0o), decimal (no prefix), and hexadecimal (0x)
     * @throws RedilogParsingException
     */
    public int parseAsInt() throws RedilogParsingException {
        String s = getValue(Type.NUMBER);
        try {
            int radix = 10;
            if (s.startsWith("0b")) {
                radix = 2;
            } else if (s.startsWith("0o")) {
                radix = 8;
            } else if (s.startsWith("0x")) {
                radix = 16;
            }
            if (radix != 10) {
                s = s.substring(2);
            }
            return Integer.parseInt(s, radix);
        } catch (NumberFormatException e) {
            throw new RedilogParsingException(e);
        }
    }

    @Override
    public String toString() {
        return String.format("(%s \"%s\" line:%d col:%d)", type, value, line, column);
    }
}
