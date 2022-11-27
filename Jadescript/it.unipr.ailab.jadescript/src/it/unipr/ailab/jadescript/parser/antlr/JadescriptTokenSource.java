/*
 * generated by Xtext 2.12.0
 */
package it.unipr.ailab.jadescript.parser.antlr;

import it.unipr.ailab.jadescript.parser.antlr.internal.InternalJadescriptParser;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.eclipse.xtext.parser.antlr.AbstractIndentationTokenSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * JadescriptTokenSource class.
 *
 * 
 * <p>
 * This class is used to insert into the token stream some synthetic tokens
 * required by the Jadescript grammar. Specifically these are: NEWLINE, INDENT
 * and DEDENT (these last two sometimes are referred as "begin" and "end" respectively
 * in this code).
 * This is done by using a series of simple checks on the sequences of tokens and an
 * internal state.
 * The internal state is distributed among this class and its superclasses (mostly
 * in {@link AbstractIndentationTokenSource} and
 * {@link org.eclipse.xtext.parser.antlr.AbstractSplittingTokenSource}) and is composed
 * especially by:
 * - the current indentation level;
 * - a stack of the widths of each indentation level;
 * - three variables containing the last three tokens encountered in the pushed stream;
 * - an {@code int} identifying a type of delimiter of the "ignore context" the code
 * is currently in;
 * - a number used to balance out the open and close delimiters of the "ignore context".
 * <p>
 * Each time the parser asks for the next token, this implementation of the lexer does this:
 * IF the lexer is not in an "ignore context" (see later)
 * AND
 * a whitespace token is encountered, and it contains a newline character
 * THEN
 * compute the indentation level (by counting blanks and tabs since the last
 * newline character, inside the whitespace token).
 * IF the current indentation level is GREATER THAN the previous indentation
 * THEN
 * IF the whitespace token is:
 * preceded by the token 'do'
 * OR
 * preceded by the ('send', -whitespace-, 'message') token sequence
 * OR
 * followed by one of these tokens: (
 * 'concept', 'proposition', 'predicate', 'action',
 * 'function', 'procedure', 'on', 'property',
 * 'cyclic', 'one', 'oneshot')
 * THEN
 * push an INDENT token after the whitespace token in the stream.
 * update the indentation stack by pushing the width of the found
 * indentation level.
 * ELSE
 * do not push anything else than the whitespace token.
 * <p>
 * ELSE IF the current indentation level is EQUAL THAN the previous indentation
 * THEN
 * push a NEWLINE token after the whitespace token in the stream.
 * ELSE IF the current indentation level is LESS THAN the previous indentation
 * THEN
 * push N DEDENT tokens after the whitespace token in the stream,
 * where N is calculated by popping N levels of indentation from the stack to
 * match the current indentation.
 * ELSE IF an EOF token is encountered
 * THEN
 * push N DEDENT tokens after the whitespace token in the stream,
 * where N is calculated by popping all the N remaining levels of indentation
 * from the stack.
 * END IF
 * END IF
 * <p>
 * The "ignore context" is activated when the code is inside any of these parentheses:
 * - '(' ')'
 * - '[' ']'
 * - '\{' '\}'
 * and the variables {@code ignoreDelimiterCounter} and {@code ignoreDelimiterType}:
 * are used to exit from the context when the proper parentheses is closed.
 */
public class JadescriptTokenSource implements TokenSource {
    /*
     * Put this to true if you want to have a log of all the token pushed
     * in the stream (slows editor performances).
     */
    private static final boolean DEBUG_TOKEN_SOURCE = false;

    private final TokenSource delegate;

    private final List<Token> allTokens = new ArrayList<>();

    private final List<Token> resultTokens = new ArrayList<>();


    private boolean firstTokenRequest = true;


    public static final int INDENT_TOKEN_TYPE = InternalJadescriptParser.RULE_INDENT;
    public static final int DEDENT_TOKEN_TYPE = InternalJadescriptParser.RULE_DEDENT;
    public static final int NEWLINE_TOKEN_TYPE = InternalJadescriptParser.RULE_NEWLINE;

    public JadescriptTokenSource(TokenSource delegate) {
        this.delegate = delegate;

    }


    private static class TokenGroup {
        final List<Token> tokens;
        private boolean isWS_or_COMMENT = false;

        public TokenGroup(Token... tokens) {
            this.tokens = new ArrayList<>(Arrays.asList(tokens));
        }

        public boolean isWS_or_COMMENT() {
            return isWS_or_COMMENT;
        }

        public void setWS_or_COMMENT(boolean b) {
            isWS_or_COMMENT = b;
        }

        public void addToken(Token t) {
            tokens.add(t);
        }

        public List<Token> getTokens() {
            return tokens;
        }

        public Token getLast() {
            return JadescriptTokenSource.getLast(tokens);
        }

        public boolean hasNoNewline() {
            for (int i = tokens.size() - 1; i >= 0; i--) {
                Token token = tokens.get(i);
                if (token.getType() == InternalJadescriptParser.RULE_WS) {
                    for (int j = token.getText().length() - 1; j >= 0; j--) {
                        char c = token.getText().charAt(j);
                        if (c == '\n' || c == '\r') {
                            return false;
                        }
                    }
                } else if (token.getType() == InternalJadescriptParser.RULE_SL_COMMENT
                        || token.getType() == InternalJadescriptParser.RULE_ML_COMMENT) {
                    return false;
                }
            }
            return true;
        }


        public int computeWSIndentation() {
            if (hasNoNewline()) {
                return -1;
            }

            if (getLast().getType() == InternalJadescriptParser.RULE_SL_COMMENT
                    || getLast().getType() == InternalJadescriptParser.RULE_ML_COMMENT) {
                return 0;
            } else if (getLast().getType() == InternalJadescriptParser.RULE_WS) {
                String text = getLast().getText();
                int result = 0;
                for (int i = text.length() - 1; i >= 0; i--) {
                    char c = text.charAt(i);
                    if (c == '\n' || c == '\r') {
                        return result;
                    }
                    if (c == '\t') {
                        result += tabWidth();
                    } else {
                        result++;
                    }
                }
                return result;
            } else {
                return -1;
            }

        }

        public void print() {
            System.out.println("Token Group" + (isWS_or_COMMENT() ? "[WS/COMM]" : "") + ": ");
            for (Token token : tokens) {
                printToken(token);
            }
        }

        public boolean isModule() {
            return getLast().getType() == InternalJadescriptParser.Module;
        }

    }


    @SuppressWarnings("SameReturnValue")
    protected static char tabWidth() {
        return 4;
    }

    private int nextTokenInvocationCount = 0;

    @Override
    public Token nextToken() {
        ++nextTokenInvocationCount;
        if (DEBUG_TOKEN_SOURCE) System.out.println("nextToken() invoked: " + nextTokenInvocationCount);
        if (firstTokenRequest) {
            if (DEBUG_TOKEN_SOURCE) System.out.println("populating sequence for analysis...");
            populateTokenList();
            if (DEBUG_TOKEN_SOURCE) System.out.println("populated sequence size: " + allTokens.size());
            if (DEBUG_TOKEN_SOURCE) System.out.println();
            if (DEBUG_TOKEN_SOURCE) System.out.println("Performing synthetic token injections");
            doSplits();
            firstTokenRequest = false;
        }

        if (resultTokens.isEmpty() || resultTokens.get(0) == null) {
            if (DEBUG_TOKEN_SOURCE) System.out.println("returning EOF!");
            return Token.EOF_TOKEN;
        }

        Token toBeSent = resultTokens.remove(0);
        if (DEBUG_TOKEN_SOURCE) {
            System.out.println("Returning token.");
            printToken(toBeSent);
            System.out.println();
        }

        return toBeSent;
    }

    /*
    if (result instanceof CommonToken) {
			nextOffset = ((CommonToken) result).getStopIndex() + 1;
		} else {
			throw new IllegalArgumentException(String.valueOf(result));
		}
     */


    protected void populateTokenList() {
        do {
            allTokens.add(delegate.nextToken());
        } while (allTokens.get(allTokens.size() - 1) != null &&
                allTokens.get(allTokens.size() - 1).getType() != Token.EOF);
    }

    @Override
    public String getSourceName() {
        return "Custom Jadescript Token Source [delegate:" + delegate.getSourceName() + "]";
    }

    protected void doSplits() {
        final List<TokenGroup> tokenGroups = new ArrayList<>();

        for (Token token : allTokens) {
            //aggregates all ws/comments in the same groups
            TokenGroup last = getLast(tokenGroups);
            if (last != null
                    && last.isWS_or_COMMENT()
                    && isWS_or_COMMENT(token)) {
                last.addToken(token);
            } else {
                TokenGroup ntg = new TokenGroup(token);
                ntg.setWS_or_COMMENT(isWS_or_COMMENT(token));
                tokenGroups.add(ntg);
            }

        }

        int ignoreDelimiterCounter = 0;
        int ignoreDelimiterType = -1;

        int currentIndentation = 0;

        Stack<Integer> indentationStack = new Stack<>();
        indentationStack.push(0);


        for (int i = 0; i < tokenGroups.size(); i++) {


            TokenGroup tokenGroup = tokenGroups.get(i);
            if (DEBUG_TOKEN_SOURCE) tokenGroup.print();

            Token lastTokenOfGroup = tokenGroup.getLast();

            if (lastTokenOfGroup.getType() == Token.EOF) {
                if (DEBUG_TOKEN_SOURCE) System.out.println("FOUND EOF!");
                while (indentationStack.size() > 1) {
                    indentationStack.pop();
                    accept(createEndTokenGroup(getNextOffset()));
                }
                accept(tokenGroup);
                break;//return;
            }

            if (tokenGroup.isModule() && i > 0 && tokenGroups.get(i - 1).isWS_or_COMMENT()) {
                int indentation;
                if (tokenGroups.get(i - 1).hasNoNewline()) {
                    indentation = tokenGroups.get(i - 1).getLast().getText().length();
                } else {
                    indentation = tokenGroups.get(i - 1).computeWSIndentation();
                }
                if (indentation > 0) {
                    indentationStack.push(indentation);
                    currentIndentation = indentation;
                    accept(createBeginTokenGroup(getNextOffset()));
                    if (DEBUG_TOKEN_SOURCE) System.out.println("added BEGIN at MODULE");
                    if (DEBUG_TOKEN_SOURCE) System.out.println();
                }
                accept(tokenGroup);
            } else if (tokenGroup.isWS_or_COMMENT()) { //if this is a WS or COMMENT RULE
                if (!mustIgnore(ignoreDelimiterCounter)) {
                    //doSplitTokenImpl(token);
                    int indentation = tokenGroup.computeWSIndentation();
                    if (indentation == -1) { //we are in the same line
                        accept(tokenGroup);
                    } else if (indentation > currentIndentation) { //indentation increased
                        accept(tokenGroup);
                        if (((tokenGroups.size() > (i + 1)) && isAfterEnabler(tokenGroups.get(i + 1).getLast()))
                                || canActivateBegin(tokenGroups, i - 1)) {
                            // push INDENT only if (i+1) isAfterEnabler or (i-1) canActivateBegin
                            indentationStack.push(indentation);
                            currentIndentation = indentation;
                            accept(createBeginTokenGroup(getNextOffset()));
                            if (DEBUG_TOKEN_SOURCE) System.out.println("added BEGIN");
                            if (DEBUG_TOKEN_SOURCE) System.out.println();
                        }
                    } else if (indentation == currentIndentation) { //indentation is the same
                        accept(tokenGroup);
                        accept(createNewLineTokenGroup(getNextOffset()));
                        if (DEBUG_TOKEN_SOURCE) System.out.println("added NEWLINE");
                        if (DEBUG_TOKEN_SOURCE) System.out.println();
                    } else { // indentation < currentIndentation -> indentation decreased
                        while (indentation < currentIndentation) {
                            indentationStack.pop();
                            currentIndentation = indentationStack.peek();
                            accept(createEndTokenGroup(getNextOffset()));
                            if (DEBUG_TOKEN_SOURCE) System.out.println("added END");
                            if (DEBUG_TOKEN_SOURCE) System.out.println();

                        }
                        accept(createNewLineTokenGroup(getNextOffset()));
                        if (DEBUG_TOKEN_SOURCE) System.out.println("added NEWLINE");
                        if (DEBUG_TOKEN_SOURCE) System.out.println();
                        if (indentation > currentIndentation) {
                            accept(tokenGroup);
                            indentationStack.push(indentation);
                            currentIndentation = indentation;
                            accept(createBeginTokenGroup(getNextOffset()));
                            if (DEBUG_TOKEN_SOURCE) System.out.println("added BEGIN (after end)");
                            if (DEBUG_TOKEN_SOURCE) System.out.println();
                            continue;//return;
                        }
                        accept(tokenGroup);
                    }
                } else {
                    accept(tokenGroup);
                }
            } else {
                if (!mustIgnore(ignoreDelimiterCounter)) {
                    if (startIgnoreIndentation(lastTokenOfGroup.getType()) != 0) {
                        ignoreDelimiterType = startIgnoreIndentation(lastTokenOfGroup.getType());
                        ignoreDelimiterCounter++;
                    }
                } else {
                    if (startIgnoreIndentation(lastTokenOfGroup.getType()) == ignoreDelimiterType) {
                        ignoreDelimiterCounter++;
                    } else if (stopIgnoreIndentation(lastTokenOfGroup.getType()) == ignoreDelimiterType) {
                        ignoreDelimiterCounter--;
                    }
                }
                accept(tokenGroup);
            }


        }


    }

    private boolean canActivateBegin(List<TokenGroup> tokenGroups, int pos) {
        return ((pos >= 0)
                && (tokenGroups.get(pos).getLast().getType() == InternalJadescriptParser.Do))
                ||
                ((pos - 2) >= 0
                        && (tokenGroups.get(pos - 2).getLast().getType() == InternalJadescriptParser.Send)
                        && (tokenGroups.get(pos - 1).isWS_or_COMMENT())
                        && (tokenGroups.get(pos).getLast().getType() == InternalJadescriptParser.Message));

    }


    private int getNextOffset() {
        if (!resultTokens.isEmpty()) {
            Token last = getLast(resultTokens);
            if (last instanceof CommonToken) {
                return ((CommonToken) last).getStopIndex() + 1;
            }
            throw new IllegalArgumentException(String.valueOf(last));
        }
        return 0;
    }

    private TokenGroup newTokenGroup(int offset, int tokenType) {
        CommonToken result = new CommonToken(tokenType);
        result.setText("");
        result.setChannel(Token.DEFAULT_CHANNEL);
        result.setStartIndex(offset);
        result.setStopIndex(offset - 1);

        return new TokenGroup(result);
    }


    protected TokenGroup createNewLineTokenGroup(int offset) {
        return newTokenGroup(offset, NEWLINE_TOKEN_TYPE);
    }

    protected TokenGroup createBeginTokenGroup(int offset) {
        return newTokenGroup(offset, INDENT_TOKEN_TYPE);
    }

    protected TokenGroup createEndTokenGroup(int offset) {
        return newTokenGroup(offset, DEDENT_TOKEN_TYPE);
    }


    private void accept(TokenGroup tokenGroup) {
        resultTokens.addAll(tokenGroup.getTokens());
    }

    private static <T> T getLast(List<T> l) {
        if (!l.isEmpty()) {
            return l.get(l.size() - 1);
        }
        return null;
    }

    public static boolean isWS_or_COMMENT(Token t) {
        return t.getType() == InternalJadescriptParser.RULE_SL_COMMENT
                || t.getType() == InternalJadescriptParser.RULE_ML_COMMENT
                || t.getType() == InternalJadescriptParser.RULE_WS;
    }

    public static void printToken(Token token) {
        System.out.println("\tToken, type: " + token.getType());
        System.out.println("\tToken, text: '" + token.getText() + "'");
        if (token instanceof CommonToken) {
            System.out.println("\tToken, start: " + ((CommonToken) token).getStartIndex());
            System.out.println("\tToken, stop : " + ((CommonToken) token).getStopIndex());

        }
        System.out.println();
    }


    private boolean isAfterEnabler(Token token) {

        if (token == null) return false;
        int type = token.getType();
        switch (type) {
            case InternalJadescriptParser.Concept:
            case InternalJadescriptParser.Native:
            case InternalJadescriptParser.Proposition:
            case InternalJadescriptParser.Action:
            case InternalJadescriptParser.Predicate:
            case InternalJadescriptParser.Function:
            case InternalJadescriptParser.Procedure:
            case InternalJadescriptParser.On:
            case InternalJadescriptParser.Property:
            case InternalJadescriptParser.Cyclic:
            case InternalJadescriptParser.One:
            case InternalJadescriptParser.Oneshot:
                return true;
            default:
                return false;
        }
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean mustIgnore(int ignoreDelimiterCounter) {
        return ignoreDelimiterCounter > 0;
    }

    protected int startIgnoreIndentation(int tokenType) {
        switch (tokenType) {
            case InternalJadescriptParser.LeftParenthesis:
                return 1;
            case InternalJadescriptParser.LeftSquareBracket:
                return 2;
            case InternalJadescriptParser.LeftCurlyBracket:
                return 3;
            default:
                return 0;
        }

    }

    protected int stopIgnoreIndentation(int tokenType) {
        switch (tokenType) {
            case InternalJadescriptParser.RightParenthesis:
                return 1;
            case InternalJadescriptParser.RightSquareBracket:
                return 2;
            case InternalJadescriptParser.RightCurlyBracket:
                return 3;
            default:
                return 0;
        }

    }


}
