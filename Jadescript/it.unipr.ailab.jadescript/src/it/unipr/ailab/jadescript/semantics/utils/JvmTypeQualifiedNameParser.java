package it.unipr.ailab.jadescript.semantics.utils;

import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JvmTypeQualifiedNameParser {
    private JvmTypeQualifiedNameParser() {
    } //Do not instantiate

    public static class GenericType {
        private final String type;
        private final boolean isGeneric;
        private final List<GenericType> arguments = new ArrayList<>();

        public GenericType(String type, boolean isGeneric) {
            this.type = type;
            this.isGeneric = isGeneric;
        }

        public boolean isGeneric() {
            return isGeneric;
        }

        public String getType() {
            return type;
        }

        public List<GenericType> getArguments() {
            return arguments;
        }

        public JvmTypeReference convertToTypeRef(
                Function<String, JvmTypeReference> typeRefFactory,
                BiFunction<String, JvmTypeReference[], JvmTypeReference> genericTypeRefFactory
        ){
            if(isGeneric){
                final JvmTypeReference[] args = arguments.stream()
                        .map(gt -> gt.convertToTypeRef(typeRefFactory, genericTypeRefFactory))
                        .toArray(JvmTypeReference[]::new);
                return genericTypeRefFactory.apply(type, args);
            }else{
                return typeRefFactory.apply(type);
            }
        }
    }

    private enum JvmGenericTokenType {
        OPEN, CLOSE, COMMA, OTHER, EOF
    }

    private static class Token {
        final JvmGenericTokenType type;
        final String lexeme;
        final int line;

        Token(JvmGenericTokenType type, String lexeme, int line) {
            this.type = type;
            this.lexeme = lexeme;
            this.line = line;
        }

        public String toString() {
            return "Token(" + type + " " + lexeme + ")";
        }
    }

    private static class Scanner {
        private final String source;
        private final List<Token> tokens = new ArrayList<>();
        private int start = 0;
        private int current = 0;
        private int line = 1;

        Scanner(String source) {
            this.source = source;
        }

        private List<Token> scanTokens() {
            while (!isAtEnd()) {
                // We are at the beginning of the next lexeme.
                start = current;
                scanToken();
            }

            tokens.add(new Token(JvmGenericTokenType.EOF, "", line));
            return tokens;
        }

        private void scanToken() {
            char c = advance();
            switch (c) {
                case '<':
                    addToken(JvmGenericTokenType.OPEN);
                    break;
                case '>':
                    addToken(JvmGenericTokenType.CLOSE);
                    break;
                case ',':
                    addToken(JvmGenericTokenType.COMMA);
                    break;
                case '\n':
                    line++;
                    break;
                default:
                    other();
            }
        }

        private boolean match(char expected) {
            if (isAtEnd()) return false;
            if (source.charAt(current) != expected) return false;

            current++;
            return true;
        }

        private void other() {
            while (!isAtEnd() && isOther(peek())) advance();

            addToken(JvmGenericTokenType.OTHER);
        }

        private boolean isOther(char x) {
            return x != '<' && x != '>';
        }

        private boolean isAtEnd() {
            return current >= source.length();
        }

        private char advance() {
            return source.charAt(current++);
        }

        private char peek() {
            if (isAtEnd()) return '\0';
            return source.charAt(current);
        }

        private void addToken(JvmGenericTokenType type) {
            String text = source.substring(start, current);
            tokens.add(new Token(type, text, line));
        }


    }

    @SuppressWarnings("serial")
	public static class ParseError extends RuntimeException {

        private final Token token;

        public ParseError(Token token, String message) {
            super("Syntax error: " + message);
            this.token = token;
        }

        public Token getToken() {
            return token;
        }
    }


    private static class Parser {
        private final List<Token> tokens;
        private int current = 0;

        public Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        public GenericType type() {
            Token other = consume(JvmGenericTokenType.OTHER, "Expecting Type Name");
            boolean isGeneric = false;
            List<GenericType> arguments = new ArrayList<>();
            if (match(JvmGenericTokenType.OPEN)) {
                isGeneric = true;
                if (!check(JvmGenericTokenType.CLOSE)) {
                    do {
                        arguments.add(type());
                    } while (match(JvmGenericTokenType.COMMA));
                }
                consume(JvmGenericTokenType.CLOSE, "Missing closing '>'");
            }
            final GenericType genericType = new GenericType(other.lexeme, isGeneric);
            genericType.getArguments().addAll(arguments);
            return genericType;
        }

        private boolean match(JvmGenericTokenType... types) {
            for (JvmGenericTokenType type : types) {
                if (check(type)) {
                    advance();
                    return true;
                }
            }

            return false;
        }

        private boolean check(JvmGenericTokenType type) {
            if (isAtEnd()) return false;
            return peek().type == type;
        }

        private Token advance() {
            if (!isAtEnd()) current++;
            return previous();
        }

        private boolean isAtEnd() {
            return peek().type == JvmGenericTokenType.EOF;
        }

        private Token peek() {
            return tokens.get(current);
        }

        private Token previous() {
            return tokens.get(current - 1);
        }

        private Token consume(JvmGenericTokenType type, String message) {
            if (check(type)) return advance();

            throw error(peek(), message);
        }

        private ParseError error(Token token, String message) {
            return new ParseError(token, message);
        }

        private void synchronize() {
            advance();

            while (!isAtEnd()) {
                if (previous().type == JvmGenericTokenType.CLOSE
                        || peek().type == JvmGenericTokenType.OPEN) {
                    return;
                }
                advance();
            }
        }
    }


    public static GenericType parseJvmGenerics(String input) {
        Scanner sc = new Scanner(input);
        final List<Token> tokens = sc.scanTokens();
        Parser p = new Parser(tokens);
        try{
            return p.type();
        }catch (ParseError pe){
            p.synchronize();
            return null;
        }
    }


}
