package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Assignment;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
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
            final PatternMatchInput<LValueExpression> pmi =
                patternMatchHelper.assignmentDeconstruction(
                    rightType, left
                );

            final PatternMatcher patternMatcher = lves.compilePatternMatch(
                pmi,
                afterRight,
                acceptor
            );
            final String localClassName =
                patternMatchHelper.getPatternMatcherClassName(left);
            final LocalClassStatementWriter localClass =
                w.localClass(localClassName);

            patternMatcher.getWriters().forEach(localClass::addMember);

            final String matcherVariableName =
                patternMatchHelper.getPatternMatcherVariableName(left);

            acceptor.accept(localClass);
            acceptor.accept(w.variable(
                localClassName,
                matcherVariableName,
                w.expr("new " + localClassName + "()")
            ));

            acceptor.accept(w.simpleStmt(
                patternMatcher.operationInvocationText(
                    rightCompiled
                )
            ));

            return lves.advancePattern(
                pmi,
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

            PatternMatchInput.AssignmentDeconstruction<LValueExpression> pmi =
                patternMatchHelper.assignmentDeconstruction(
                    rightType,
                    left
                );

            final boolean patternCheck = lves.validatePatternMatch(
                pmi,
                afterRight,
                acceptor
            );

            if(patternCheck) {
                return lves.advancePattern(pmi, afterRight);
            }else{
                return afterRight;
            }
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
