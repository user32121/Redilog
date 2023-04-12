package redilog.synthesis;

import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

public class Token {
    public static class Builder {
        private String value = "";
        private final TypeHint type;
        private final int line;
        private final int column;

        public Builder(TypeHint type, int line, int column) {
            this.type = type;
            this.line = line;
            this.column = column;
        }

        public Builder addChar(char c) {
            value += c;
            return this;
        }

        public Token build() {
            return new Token(value, type, line, column);
        }

        public String getValue() {
            return value;
        }

        public TypeHint getType() {
            return type;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }
    }

    enum Type {
        KEYWORD,
        VARIABLE,
        NUMBER,
        SYMBOL,
        EOF,
    }

    enum TypeHint {
        LETTERS_DIGITS,
        SYMBOL,
        EOF,
    }

    public static final Set<String> KEYWORDS = Set.of("input", "output", "assign");

    public static Token EOF(int line, int column) {
        return new Token("<EOF>", TypeHint.EOF, line, column);
    }

    private final String value;
    private final Type type;
    private final int line;
    private final int column;

    public Token(String value, TypeHint hint, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
        switch (hint) {
            case LETTERS_DIGITS:
                if (Character.isLetter(value.charAt(0))) {
                    if (KEYWORDS.contains(value)) {
                        type = Type.KEYWORD;
                    } else {
                        type = Type.VARIABLE;
                    }
                } else {
                    type = Type.NUMBER;
                }
                break;
            case SYMBOL:
                type = Type.SYMBOL;
                break;
            case EOF:
                type = Type.EOF;
                break;
            default:
                throw new NotImplementedException(String.format("%s handling not implemented", hint));
        }
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    //TODO pass custom message as string
    public String getValue(Token.Type requiredType) throws RedilogParsingException {
        if (type != requiredType) {
            throw new RedilogParsingException(
                    String.format("Expected token of type %s but got %s", requiredType, this));
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
        String v = getValue(requiredType);
        for (String s : expected) {
            if (v.equals(s)) {
                return;
            }
        }
        throw new RedilogParsingException(
                String.format("Expected one of [\"%s\"] but got %s", String.join("\",\"", expected), this));
    }

    /**
     * Attempts to parse the token as an integer.
     * Accepts binary (prefixed with 0b), octal (0o), decimal (no prefix), and hexadecimal (0x)
     * @throws RedilogParsingException
     */
    public int parseAsInt() throws RedilogParsingException {
        getValue(Type.NUMBER);
        try {
            //TODO handle radix
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RedilogParsingException("Could not parse integer", e);
        }
    }

    @Override
    public String toString() {
        return String.format("(%s \"%s\" line:%d col:%d)", type, value, line, column);
    }
}
