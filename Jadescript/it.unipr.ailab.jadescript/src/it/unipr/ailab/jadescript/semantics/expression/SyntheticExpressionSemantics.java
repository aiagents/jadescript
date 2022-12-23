package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Created on 01/11/2018.
 */
@Singleton
public class SyntheticExpressionSemantics extends ExpressionSemantics<SyntheticExpression> {

    public SyntheticExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<SyntheticExpression> input) {
        return Collections.emptyList();
    }

    @Override
    protected String compileInternal(Maybe<SyntheticExpression> input, CompilationOutputAcceptor acceptor) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.compile();
        }
        return "";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.inferType(module.get(TypeHelper.class));
        }
        return module.get(TypeHelper.class).ANY;
    }

    @Override
    protected boolean mustTraverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.mustTraverse();
        }
        return false;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<SyntheticExpression> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<SyntheticExpression> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.traverse();
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.isPatternEvaluationPure();
        }
        return true;
    }

    @Override
    protected boolean validateInternal(Maybe<SyntheticExpression> input, ValidationMessageAcceptor acceptor) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.validate(acceptor);
        }
        return VALID;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<SyntheticExpression, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<SyntheticExpression> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<SyntheticExpression, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }
}
