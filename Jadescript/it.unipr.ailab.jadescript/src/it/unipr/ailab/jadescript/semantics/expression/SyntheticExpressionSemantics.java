package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 01/11/2018.
 */
@Singleton
public class SyntheticExpressionSemantics extends ExpressionSemantics<SyntheticExpression> {

    public SyntheticExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<SyntheticExpression> input) {
        return Collections.emptyList();
    }

    @Override
    public Maybe<String> compile(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.compile();
        }
        return nothing();
    }

    @Override
    public IJadescriptType inferType(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.inferType(module.get(TypeHelper.class));
        }
        return module.get(TypeHelper.class).ANY;
    }

    @Override
    public boolean mustTraverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.mustTraverse();
        }
        return false;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.traverse();
        }
        return Optional.empty();
    }

    @Override
    public void validate(Maybe<SyntheticExpression> input, ValidationMessageAcceptor acceptor) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            customSemantics.validate(acceptor);
        }
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<SyntheticExpression, ?, ?> input) {
        return input.createEmptyCompileOutput();
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<SyntheticExpression, ?, ?> input) {
        return PatternType.empty(module);
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<SyntheticExpression, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }
}
