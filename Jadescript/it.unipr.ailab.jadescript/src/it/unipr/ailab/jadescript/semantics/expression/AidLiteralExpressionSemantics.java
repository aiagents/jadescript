package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.AidLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeCast;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.nullAsTrue;

public class AidLiteralExpressionSemantics extends AssignableExpressionSemantics<AidLiteral> {
    public AidLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }

    @Override
    public void validate(Maybe<AidLiteral> input, ValidationMessageAcceptor acceptor) {
        module.get(TypeCastExpressionSemantics.class).validate(input.__(AidLiteral::getTypeCast), acceptor);
        if(input.__(AidLiteral::getHap).isPresent()){
            module.get(TypeCastExpressionSemantics.class).validate(input.__(AidLiteral::getHap), acceptor);
        }
    }

    @Override
    public Maybe<String> compileAssignment(
            Maybe<AidLiteral> input,
            String compiledExpression,
            IJadescriptType exprType
    ) {
        return module.get(TypeCastExpressionSemantics.class)
                .compileAssignment(input.__(AidLiteral::getTypeCast), compiledExpression, exprType);
    }

    @Override
    public void validateAssignment(
            Maybe<AidLiteral> input,
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        module.get(TypeCastExpressionSemantics.class)
                .validateAssignment(input.__(AidLiteral::getTypeCast), assignmentOperator, expression, acceptor);
    }

    @Override
    public void syntacticValidateLValue(Maybe<AidLiteral> input, ValidationMessageAcceptor acceptor) {
        if(input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)){
            errorNotLvalue(input, acceptor);
        }else{
            module.get(TypeCastExpressionSemantics.class).syntacticValidateLValue(
                    input.__(AidLiteral::getTypeCast),
                    acceptor
            );
        }
    }

    @Override
    public boolean isHoled(Maybe<AidLiteral> input) {
        final Maybe<TypeCast> typeCast = input.__(AidLiteral::getTypeCast);
        final Maybe<TypeCast> hap = input.__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        return tces.isHoled(typeCast) || tces.isHoled(hap);
    }

    @Override
    public boolean isUnbounded(Maybe<AidLiteral> input) {
        final Maybe<TypeCast> typeCast = input.__(AidLiteral::getTypeCast);
        final Maybe<TypeCast> hap = input.__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        return tces.isUnbounded(typeCast) || tces.isUnbounded(hap);
    }




    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<AidLiteral> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        List<SemanticsBoundToExpression<?>> result = new ArrayList<>();
        result.add(input.__(AidLiteral::getTypeCast).extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(TypeCastExpressionSemantics.class), x
        )));
        if (input.__(AidLiteral::getHap).isPresent()) {
            result.add(input.__(AidLiteral::getHap).extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                    module.get(TypeCastExpressionSemantics.class), x
            )));
        }
        return result;
    }

    @Override
    public Maybe<String> compile(Maybe<AidLiteral> input) {
        String result;
        final Maybe<String> leftCompiled = module.get(TypeCastExpressionSemantics.class)
                .compile(input.__(AidLiteral::getTypeCast));
        if (input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)) {
            String argString = "java.lang.String.valueOf(" + leftCompiled.orElse("") + ")";
            String isGuid = "false";
            if (input.__(AidLiteral::getHap).isPresent()) {
                isGuid = "true";
                final Maybe<String> rightCompiled = module.get(TypeCastExpressionSemantics.class)
                        .compile(input.__(AidLiteral::getHap));
                argString += " + \"@\" + " +
                        "java.lang.String.valueOf(" + rightCompiled.orElse("") + ")";
            }
            result = "new jade.core.AID(" + argString + ", " + isGuid + ")";
            return Maybe.of(result);
        } else {
            return leftCompiled;
        }

    }

    @Override
    public IJadescriptType inferType(Maybe<AidLiteral> input) {
        return mustTraverse(input) ?
                module.get(TypeCastExpressionSemantics.class).inferType(input.__(AidLiteral::getTypeCast))
                :
                module.get(TypeHelper.class).AID;
    }

    @Override
    public boolean mustTraverse(Maybe<AidLiteral> input) {
        return !input.__(AidLiteral::isIsAidExpr).extract(nullAsTrue);
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<AidLiteral> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(input.__(AidLiteral::getTypeCast))
                    .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                            module.get(TypeCastExpressionSemantics.class), x));
        } else {
            return Optional.empty();
        }

    }
}
