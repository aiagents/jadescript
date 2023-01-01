package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class StringLiteralSemantics extends ExpressionSemantics<StringLiteralSimple> {
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
                // replace escaping, removing suffix/prefix ' , adding " delimiters
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
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<StringLiteralSimple> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<StringLiteralSimple> input,
                                          StaticState state) {
        return ExpressionTypeKB.empty();
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
                                Util.extractEObject(input).safeDo(inputSafe -> {
                                    acceptor.acceptError(
                                            "Invalid escape sequence '" + c + nextC + "'.",
                                            inputSafe,
                                            Util.getLocationForEObject(inputSafe).getOffset() + finalI,
                                            2,
                                            "InvalidTextEscape"

                                    );
                                });
                                result = INVALID;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected boolean validateInternal(Maybe<StringLiteralSimple> input, StaticState state, ValidationMessageAcceptor acceptor) {
        //simply validate all parts
        return input.__(StringLiteralSimple::getValue).__(valueSafe ->
            validateEscapes(valueSafe, input, acceptor)
        ).orElse(VALID);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<StringLiteralSimple> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(
        Maybe<StringLiteralSimple> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        return adaptStringConstant(input.__(StringLiteralSimple::getValue)).orElse("");
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<StringLiteralSimple> input, StaticState state) {
        return module.get(TypeHelper.class).TEXT;
    }

    @Override
    protected boolean mustTraverse(Maybe<StringLiteralSimple> input) {
        return false;
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<StringLiteralSimple> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(PatternMatchInput<StringLiteralSimple> input, StaticState state) {
        return true;
    }

    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<StringLiteralSimple> input, StaticState state, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<StringLiteralSimple> input, StaticState state) {
        return PatternType.empty(module);
    }

    @Override
    public boolean validatePatternMatchInternal(PatternMatchInput<StringLiteralSimple> input, StaticState state, ValidationMessageAcceptor acceptor) {
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<StringLiteralSimple> input,
                                           StaticState state) {
        return true;
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<StringLiteralSimple> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<StringLiteralSimple> input,
                                      StaticState state) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<StringLiteralSimple> input,
                                            StaticState state) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<StringLiteralSimple> input,
                                        StaticState state) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<StringLiteralSimple> input) {
        return false;
    }
}
