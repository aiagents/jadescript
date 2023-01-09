package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Assignment;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

/**
 * Created on 26/04/18.
 */
@Singleton
public class AssignmentSemantics extends StatementSemantics<Assignment> {


    public AssignmentSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<Assignment> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final Maybe<LValueExpression> left = input.__(Assignment::getLexpr);
        final Maybe<RValueExpression> right = input.__(Assignment::getRexpr);


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final String rightCompiled = rves.compile(right, state, acceptor);
        final IJadescriptType rightType = rves.inferType(right, state);
        final StaticState afterRight = rves.advance(right, state);

        final LValueExpressionSemantics lves =
            module.get(LValueExpressionSemantics.class);

        if (lves.canBeHoled(left) && lves.isHoled(left, afterRight)) {
            final PatternMatchHelper patternMatchHelper =
                module.get(PatternMatchHelper.class);
            final PatternMatcher patternMatcher = patternMatchHelper
                .compileAssignmentDeconstructionPatternMatching(
                    rightType,
                    left,
                    afterRight,
                    acceptor
                );
            acceptor.accept(w.simpleStmt(
                patternMatcher.operationInvocationText(
                    rightCompiled
                )
            ));
            return patternMatchHelper
                .advanceAssignmentDeconstructionPatternMatching(
                    rightType,
                    left,
                    afterRight
                );
        }

        if (lves.isValidLExpr(left)) {
            lves.compileAssignment(
                left,
                rightCompiled,
                rightType,
                afterRight,
                acceptor
            );
            return lves.advanceAssignment(left, rightType, afterRight);
        }

        acceptor.accept(w.commentStmt(
            "Cannot detect assignment semantics."
        ));
        String leftCompiled = lves.compile(left, afterRight, acceptor);
        acceptor.accept(w.assign(leftCompiled, w.expr(rightCompiled)));
        return lves.advanceAssignment(left, rightType, afterRight);
    }


    @Override
    public Stream<SemanticsBoundToExpression<?>>
    includedExpressions(Maybe<Assignment> input) {

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        return Stream.of(input.__(Assignment::getRexpr))
            .filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(rves, i));
    }


    @Override
    public StaticState validateStatement(
        Maybe<Assignment> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        final Maybe<LValueExpression> left = input.__(Assignment::getLexpr);
        final Maybe<RValueExpression> right = input.__(Assignment::getRexpr);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final boolean rightCheck = rves.validate(right, state, acceptor);
        if (rightCheck == INVALID) {
            return state;
        }

        final IJadescriptType rightType = rves.inferType(right, state);
        final StaticState afterRight = rves.advance(right, state);

        final LValueExpressionSemantics lves =
            module.get(LValueExpressionSemantics.class);

        if (lves.canBeHoled(left) && lves.isHoled(left, afterRight)) {
            final PatternMatchHelper patternMatchHelper =
                module.get(PatternMatchHelper.class);
            patternMatchHelper
                .validateAssignmentDeconstructionPatternMatching(
                    rightType,
                    left,
                    afterRight,
                    acceptor
                );

            return patternMatchHelper
                .advanceAssignmentDeconstructionPatternMatching(
                    rightType,
                    left,
                    afterRight
                );
        }

        boolean syntacticSubValidation = lves.syntacticValidateLValue(
            input.__(Assignment::getLexpr),
            acceptor
        );
        if (syntacticSubValidation == INVALID) {
            return afterRight;
        }

        if (lves.isValidLExpr(left)) {

            lves.validateAssignment(
                left,
                right,
                afterRight,
                acceptor
            );
            return lves.advanceAssignment(left, rightType, afterRight);
        }


        return afterRight;

    }


}
