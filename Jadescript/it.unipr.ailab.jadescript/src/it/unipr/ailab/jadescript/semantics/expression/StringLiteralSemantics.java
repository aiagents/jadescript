package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private String removePrefixIfPresent(String prefix, String target) {
        if (target.startsWith(prefix)) {
            return target.substring(prefix.length());
        } else {
            return target;
        }
    }

    private String removeSuffixIfPresent(String suffix, String target) {
        if (target.endsWith(suffix)) {
            return target.substring(0, target.length() - suffix.length());
        } else {
            return target;
        }
    }

    private void validateEscapes(
            String string,
            Maybe<? extends EObject> input,
            ValidationMessageAcceptor acceptor
    ) {
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
                                input.safeDo(inputSafe -> {
                                    acceptor.acceptError(
                                            "Invalid escape sequence '" + c + nextC + "'.",
                                            inputSafe,
                                            Util.getLocationForEObject(inputSafe).getOffset() + finalI,
                                            2,
                                            "InvalidTextEscape"

                                    );
                                });
                        }
                    }
                }
            }
        }
    }

    @Override
    public void validate(Maybe<StringLiteralSimple> input, ValidationMessageAcceptor acceptor) {
        //simply validate all parts
        input.__(StringLiteralSimple::getValue).safeDo(valueSafe -> {
            validateEscapes(valueSafe, input, acceptor);
        });
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<StringLiteralSimple> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public Maybe<String> compile(Maybe<StringLiteralSimple> input) {
        return adaptStringConstant(input.__(StringLiteralSimple::getValue));
    }

    @Override
    public IJadescriptType inferType(Maybe<StringLiteralSimple> input) {
        return module.get(TypeHelper.class).TEXT;
    }

    @Override
    public boolean mustTraverse(Maybe<StringLiteralSimple> input) {
        return false;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<StringLiteralSimple> input) {
        return Optional.empty();
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<StringLiteralSimple, ?, ?> input) {
        return input.createEmptyCompileOutput();
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<StringLiteralSimple, ?, ?> input) {
        return PatternType.empty(module);
    }

    @Override
    protected PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(PatternMatchInput<StringLiteralSimple, ?, ?> input, ValidationMessageAcceptor acceptor) {
        return input.createEmptyValidationOutput();
    }


}
