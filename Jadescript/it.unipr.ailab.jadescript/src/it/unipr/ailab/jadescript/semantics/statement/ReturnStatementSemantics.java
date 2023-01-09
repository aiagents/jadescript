package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.ReturnStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ReturnExpectedContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created on 26/04/18.
 */
@Singleton
public class ReturnStatementSemantics
    extends StatementSemantics<ReturnStatement> {

    public ReturnStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<ReturnStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        Maybe<RValueExpression> expr = input.__(ReturnStatement::getExpr);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        String compiledExpression = rves.compile(expr, state, acceptor);

        StaticState afterExprState = state;
        if (expr.isPresent()) {
            final Optional<IJadescriptType> expectedReturn =
                module.get(ContextManager.class)
                    .currentContext()
                    .searchAs(ReturnExpectedContext.class, Stream::of)
                    .findFirst()
                    .map(ReturnExpectedContext::expectedReturnType);
            if (expectedReturn.isPresent()) {
                IJadescriptType returnExprType =
                    rves.inferType(expr, state);
                compiledExpression = module.get(TypeHelper.class)
                    .compileWithEventualImplicitConversions(
                        compiledExpression,
                        returnExprType,
                        expectedReturn.get()
                    );
            }

            afterExprState = rves.advance(expr, state);
        }

        acceptor.accept(w.returnStmnt(w.expr(compiledExpression)));

        return afterExprState.invalidateUntilExitOperation();
    }


    @Override
    public StaticState validateStatement(
        Maybe<ReturnStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<RValueExpression> expr = input.__(ReturnStatement::getExpr);

        boolean hasExpr = expr.isPresent();
        boolean exprCheck = true;

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (hasExpr) {
            exprCheck = rves.validate(expr, state, acceptor);
        }

        if (exprCheck == INVALID) {
            return state;
        }

        //check if void/not void, and if not void, that the return type is valid
        final Optional<IJadescriptType> expectedReturn =
            module.get(ContextManager.class).currentContext()
                .searchAs(ReturnExpectedContext.class, Stream::of)
                .findFirst()
                .map(ReturnExpectedContext::expectedReturnType);


        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);


        if (expectedReturn.isPresent() && module.get(TypeHelper.class).VOID
            .typeEquals(expectedReturn.get())
            || expectedReturn.isEmpty()) {
            //just check that has not expr
            validationHelper.asserting(
                !hasExpr,
                "InvalidReturnStatement",
                "Cannot return values from a procedure",
                input,
                JadescriptPackage.eINSTANCE.getReturnStatement_Expr(),
                acceptor
            );
            return state.invalidateUntilExitOperation();
        }

        if (!hasExpr) {
            validationHelper.emitError(
                "InvalidReturnStatement",
                "Expected returned value of type: " +
                    expectedReturn.get(),
                input,
                acceptor
            );
            return state;
        }

        IJadescriptType actualType = rves.inferType(expr, state);
        validationHelper.asserting(
            expectedReturn.get().isAssignableFrom(actualType),
            "InvalidReturnStatement",
            "Expected returned value type: " + expectedReturn.get()
                + "; found: " + actualType,
            input,
            JadescriptPackage.eINSTANCE.getReturnStatement_Expr(),
            acceptor
        );
        StaticState afterExpr = rves.advance(expr, state);
        return afterExpr.invalidateUntilExitOperation();

    }


    @Override
    public Stream<SemanticsBoundToExpression<?>> includedExpressions(
        Maybe<ReturnStatement> input
    ) {
        return Util.buildStream(() -> new SemanticsBoundToExpression<>(
            module.get(RValueExpressionSemantics.class),
            input.__(ReturnStatement::getExpr)
        ));
    }


    @Override
    public List<Effect> computeEffectsInternal(
        Maybe<ReturnStatement> input,
        StaticState state
    ) {
        return Collections.singletonList(
            Effect.JumpsAwayFromOperation.INSTANCE
        );
    }

}
