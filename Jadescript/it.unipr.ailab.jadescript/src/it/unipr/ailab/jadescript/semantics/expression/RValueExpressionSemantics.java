package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TernaryConditional;
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
 * Created on 27/12/16.
 */
@Singleton
public class RValueExpressionSemantics extends ExpressionSemantics<RValueExpression> {



    public RValueExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<RValueExpression> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(Maybe<RValueExpression> input, CompilationOutputAcceptor acceptor) {
        return "";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<RValueExpression> input) {
        return module.get(TypeHelper.class).ANY;
    }


    @Override
    protected boolean mustTraverse(Maybe<RValueExpression> input) {
        return input.isInstanceOf(SyntheticExpression.class) || input.isInstanceOf(TernaryConditional.class);
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<RValueExpression> input) {
        if (input.isInstanceOf(SyntheticExpression.class)) {
            return Optional.of(
                    new SemanticsBoundToExpression<>(
                            module.get(SyntheticExpressionSemantics.class),
                            input.__((i -> (SyntheticExpression) i))
                    ));
        }
        if (input.isInstanceOf(TernaryConditional.class)) {
            return Optional.of(
                    new SemanticsBoundToExpression<>(
                            module.get(TernaryConditionalExpressionSemantics.class),
                            input.__((i -> (TernaryConditional) i))
                    ));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<RValueExpression> input) {
        return true;
    }


    @Override
    protected boolean validateInternal(Maybe<RValueExpression> input, ValidationMessageAcceptor acceptor) {
        return VALID;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<RValueExpression> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<RValueExpression> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<RValueExpression, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<RValueExpression> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<RValueExpression, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<RValueExpression> input) {
        return true;
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<RValueExpression> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<RValueExpression> input) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<RValueExpression> input) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<RValueExpression> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<RValueExpression> input) {
        return false;
    }
}
