package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

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

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<LValueExpression> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(Maybe<LValueExpression> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        return module.get(OfNotationExpressionSemantics.class).compile(input.__(i -> (OfNotation) i), , acceptor);
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<LValueExpression> input, StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).inferType(input.__(i -> (OfNotation) i), );
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<LValueExpression> input, StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).describeExpression(input.__(i -> (OfNotation) i), );
    }

    @Override
    protected StaticState advanceInternal(Maybe<LValueExpression> input,
                                          StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).advance(input.__(i -> (OfNotation) i), );
    }

    @Override
    protected boolean mustTraverse(Maybe<LValueExpression> input) {
        return true;
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<LValueExpression> input) {
        return Optional.of(new SemanticsBoundToExpression<>(
                module.get(OfNotationExpressionSemantics.class),
                input.__(i -> (OfNotation) i)
        ));
    }

    @Override
    protected boolean validateInternal(Maybe<LValueExpression> input, StaticState state, ValidationMessageAcceptor acceptor) {
        return module.get(OfNotationExpressionSemantics.class).validate(input.__(i -> (OfNotation) i), , acceptor);
    }

    @Override
    public void compileAssignmentInternal(
        Maybe<LValueExpression> input,
        String expression,
        IJadescriptType exprType,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        module.get(OfNotationExpressionSemantics.class)
                .compileAssignment(input.__(i -> (OfNotation) i), expression, exprType, , acceptor);
    }

    @Override
    public boolean validateAssignmentInternal(
        Maybe<LValueExpression> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return module.get(OfNotationExpressionSemantics.class).validateAssignment(
                input.__(i -> (OfNotation) i),
                expression, ,
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
    protected boolean isPatternEvaluationPureInternal(PatternMatchInput<LValueExpression> input, StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).isPatternEvaluationPure(input.__(i -> (OfNotation) i), );
    }

    @Override
    protected boolean isHoledInternal(Maybe<LValueExpression> input,
                                      StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).isHoled(input.__(i -> (OfNotation) i), );
    }

    @Override
    protected boolean isUnboundInternal(Maybe<LValueExpression> input,
                                        StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).isUnbound(input.__(i -> (OfNotation) i), );
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<LValueExpression> input, StaticState state, CompilationOutputAcceptor acceptor) {
        return module.get(OfNotationExpressionSemantics.class).compilePatternMatchInternal(
                input.mapPattern(lve -> (OfNotation) lve), ,
            acceptor
        );
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<LValueExpression> input, StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).inferPatternTypeInternal(
                input.__(lve -> (OfNotation) lve), );
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<LValueExpression> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return module.get(OfNotationExpressionSemantics.class).validatePatternMatchInternal(
                input.mapPattern(lve -> (OfNotation) lve), ,
            acceptor
        );
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<LValueExpression> input,
                                           StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).isAlwaysPureInternal(input.__(lve-> (OfNotation) lve), );
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<LValueExpression> input,
                                            StaticState state) {
        return module.get(OfNotationExpressionSemantics.class).isTypelyHoledInternal(input.__(lve-> (OfNotation) lve), );
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).canBeHoledInternal(input.__(lve-> (OfNotation) lve));
    }


}
