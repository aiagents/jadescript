package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.StringLiteralSimple;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class StringLiteralSemantics
    extends AssignableExpressionSemantics<StringLiteralSimple> {

    public StringLiteralSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    private Maybe<String> adaptStringConstant(
        Maybe<String> constant
    ) {
        return constant.__(stringSafe -> {
            if (stringSafe.startsWith("\"") && stringSafe.endsWith("\"")) {
                // case Simple string: "..."
                // replace escaping
                return stringSafe.replaceAll(
                    Pattern.quote("\\$"),
                    Matcher.quoteReplacement("$")
                );
            } else if (stringSafe.startsWith("'") && stringSafe.endsWith("'")) {
                // case Simple string: '...'
                // replace escaping, removing suffix/prefix ' , adding "
                // delimiters
                return "\"" + removePrefixIfPresent(
                    "'",
                    removeSuffixIfPresent(
                        "'",
                        stringSafe.replaceAll(
                            Pattern.quote("\\$"),
                            Matcher.quoteReplacement("$")
                        )
                    )
                ) + "\"";
            } else return "\"" + stringSafe + "\"";
        });
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return state;
    }


    @SuppressWarnings("SameParameterValue")
    private String removePrefixIfPresent(String prefix, String target) {
        if (target.startsWith(prefix)) {
            return target.substring(prefix.length());
        } else {
            return target;
        }
    }


    @SuppressWarnings("SameParameterValue")
    private String removeSuffixIfPresent(String suffix, String target) {
        if (target.endsWith(suffix)) {
            return target.substring(0, target.length() - suffix.length());
        } else {
            return target;
        }
    }


    private boolean validateEscapes(
        String string,
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    ) {
        boolean result = VALID;
        if (string.length() > 2) {
            String text = string.substring(1, string.length() - 1);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\\') {
                    i++;
                    if (i < text.length()) {
                        char nextC = text.charAt(i);
                        switch (nextC) {
                            case 't':
                            case '\'':
                            case '"':
                            case 'r':
                            case '\\':
                            case 'n':
                            case 'f':
                            case 'b':
                            case '$':
                                //ok
                                break;
                            default:
                                int finalI = i;
                                Util.extractEObject(input)
                                    .safeDo(inputSafe -> acceptor.acceptError(
                                        "Invalid escape sequence '" + c +
                                            nextC + "'.",
                                        inputSafe,
                                        Util.getLocationForEObject(inputSafe)
                                            .getOffset() + finalI,
                                        2,
                                        "InvalidTextEscape"
                                    ));
                                result = INVALID;
                        }
                    }
                }
            }
        }
        return result;
    }


    @Override
    protected boolean validateInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        //simply validate all parts
        return input.__(StringLiteralSimple::getValue).__(valueSafe ->
            validateEscapes(valueSafe, input, acceptor)
        ).orElse(VALID);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<StringLiteralSimple> input
    ) {
        return Stream.empty();
    }


    @Override
    protected String compileInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        return adaptStringConstant(input.__(StringLiteralSimple::getValue))
            .orElse("");
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return module.get(TypeHelper.class).TEXT;
    }


    @Override
    protected boolean mustTraverse(Maybe<StringLiteralSimple> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(Maybe<StringLiteralSimple> input) {
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<StringLiteralSimple> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<StringLiteralSimple> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<StringLiteralSimple> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<StringLiteralSimple> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<StringLiteralSimple> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<StringLiteralSimple> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<StringLiteralSimple> input) {
        return false;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<StringLiteralSimple> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<StringLiteralSimple> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {

    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<StringLiteralSimple> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<StringLiteralSimple> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<StringLiteralSimple> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }

}
