package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created on 28/12/16.
 */
@Singleton
public class LValueExpressionSemantics extends AssignableExpressionSemantics<LValueExpression> {


    public LValueExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<LValueExpression> input) {
        Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
        if (traversed.isPresent()) {
            return traversed.get().getSemantics().getSubExpressions((Maybe) traversed.get().getInput());
        }

        return Collections.emptyList();
    }

    @Override
    protected String compileInternal(Maybe<LValueExpression> input, CompilationOutputAcceptor acceptor) {
        return module.get(OfNotationExpressionSemantics.class).compile(input.__(i -> (OfNotation) i), acceptor);
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).inferType(input.__(i -> (OfNotation) i));
    }


    @Override
    protected List<String> propertyChainInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).propertyChain(input.__(i -> (OfNotation) i));
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).computeKB(input.__(i -> (OfNotation) i));
    }

    @Override
    protected boolean mustTraverse(Maybe<LValueExpression> input) {
        return true;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<LValueExpression> input) {
        return Optional.of(new SemanticsBoundToExpression<>(
                module.get(OfNotationExpressionSemantics.class),
                input.__(i -> (OfNotation) i)
        ));
    }

    @Override
    protected boolean validateInternal(Maybe<LValueExpression> input, ValidationMessageAcceptor acceptor) {
        return module.get(OfNotationExpressionSemantics.class).validate(input.__(i -> (OfNotation) i), acceptor);
    }

    @Override
    public void compileAssignmentInternal(
            Maybe<LValueExpression> input,
            String expression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        module.get(OfNotationExpressionSemantics.class)
                .compileAssignment(input.__(i -> (OfNotation) i), expression, exprType, acceptor);
    }

    @Override
    public boolean validateAssignmentInternal(
            Maybe<LValueExpression> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        return module.get(OfNotationExpressionSemantics.class).validateAssignment(
                input.__(i -> (OfNotation) i),
                expression,
                acceptor
        );
    }

    @Override
    public boolean syntacticValidateLValueInternal(
            Maybe<LValueExpression> input,
            ValidationMessageAcceptor acceptor
    ) {
        return module.get(OfNotationExpressionSemantics.class)
                .syntacticValidateLValue(input.__(i -> (OfNotation) i), acceptor);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).isValidLExpr(input.__(i -> (OfNotation) i));
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).isPatternEvaluationPure(input.__(i -> (OfNotation) i));
    }

    @Override
    protected boolean isHoledInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).isHoled(input.__(i -> (OfNotation) i));
    }

    @Override
    protected boolean isUnboundInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).isUnbound(input.__(i -> (OfNotation) i));
    }


    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<LValueExpression, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return module.get(OfNotationExpressionSemantics.class).compilePatternMatchInternal(
                input.mapPattern(lve -> (OfNotation) lve),
                acceptor
        );
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).inferPatternTypeInternal(
                input.__(lve -> (OfNotation) lve));
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<LValueExpression, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return module.get(OfNotationExpressionSemantics.class).validatePatternMatchInternal(
                input.mapPattern(lve -> (OfNotation) lve),
                acceptor
        );
    }


}
