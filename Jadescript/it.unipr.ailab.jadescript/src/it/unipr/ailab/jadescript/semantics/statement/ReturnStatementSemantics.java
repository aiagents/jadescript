package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.ReturnStatement;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ReturnExpectedContext;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
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
public class ReturnStatementSemantics extends StatementSemantics<ReturnStatement> {

    public ReturnStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void validate(Maybe<ReturnStatement> input, ValidationMessageAcceptor acceptor) {
        //the check for unreachable statements is done in BlockSemantics


        Maybe<RValueExpression> expr = input.__(ReturnStatement::getExpr);

        boolean hasExpr = expr.isPresent();
        boolean isExprValid = true;
        if (hasExpr) {
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(RValueExpressionSemantics.class).validate(expr, interceptAcceptor);
            isExprValid = !interceptAcceptor.thereAreErrors();
        }

        //check if void/not void, and if not void, that the return type is valid
        if (isExprValid) {
            final Optional<IJadescriptType> expectedReturn = module.get(ContextManager.class)
                    .currentContext()
                    .searchAs(ReturnExpectedContext.class, Stream::of)
                    .findFirst()
                    .map(ReturnExpectedContext::expectedReturnType);


            if (expectedReturn.isPresent()) {
                if (module.get(TypeHelper.class).VOID.typeEquals(expectedReturn.get())) {
                    module.get(ValidationHelper.class).assertion(!hasExpr,
                            "InvalidReturnStatement",
                            "Cannot return a value from a procedure",
                            input,
                            JadescriptPackage.eINSTANCE.getReturnStatement_Expr(),
                            acceptor);
                } else {
                    if (hasExpr) {
                        IJadescriptType actualType = module.get(RValueExpressionSemantics.class).inferType(expr);
                        module.get(ValidationHelper.class).assertion(expectedReturn.get().isAssignableFrom(actualType),
                                "InvalidReturnStatement",
                                "Expected returned value type: " + expectedReturn.get()
                                        + "; found: " + actualType,
                                input,
                                JadescriptPackage.eINSTANCE.getReturnStatement_Expr(),
                                acceptor);
                    } else {
                        input.safeDo(inputSafe -> {
                            acceptor.acceptError("Expected returned value of type: " + expectedReturn.get(),
                                    inputSafe,
                                    null,
                                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                                    "InvalidReturnStatement");
                        });
                    }
                }
            } else {
                //just check that has not expr
                module.get(ValidationHelper.class).assertion(!hasExpr,
                        "InvalidReturnStatement",
                        "Cannot return values from a procedure",
                        input,
                        JadescriptPackage.eINSTANCE.getReturnStatement_Expr(),
                        acceptor);
            }
        }

    }

    @Override
    public void compileStatement(Maybe<ReturnStatement> input, CompilationOutputAcceptor acceptor) {
        Maybe<RValueExpression> expr = input.__(ReturnStatement::getExpr);

        String compiledExpression = module.get(RValueExpressionSemantics.class).compile(expr, acceptor).toString();
        if(expr.isPresent()){
            final Optional<IJadescriptType> expectedReturn = module.get(ContextManager.class)
                    .currentContext()
                    .searchAs(ReturnExpectedContext.class, Stream::of)
                    .findFirst()
                    .map(ReturnExpectedContext::expectedReturnType);
            if(expectedReturn.isPresent()){
                IJadescriptType returnExprType = module.get(RValueExpressionSemantics.class).inferType(expr);
                if(module.get(TypeHelper.class).implicitConversionCanOccur(returnExprType, expectedReturn.get())){

                    compiledExpression = module.get(TypeHelper.class).compileImplicitConversion(
                            compiledExpression,
                            returnExprType,
                            expectedReturn.get()
                    );
                }
            }
        }

        acceptor.accept(w.returnStmnt(w.expr(compiledExpression)));
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<ReturnStatement> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(ReturnStatement::getExpr)
        ));
    }

    @Override
    public List<Effect> computeEffectsInternal(Maybe<ReturnStatement> input) {
        return Collections.singletonList(Effect.JumpsAwayFromOperation.INSTANCE);
    }
}
