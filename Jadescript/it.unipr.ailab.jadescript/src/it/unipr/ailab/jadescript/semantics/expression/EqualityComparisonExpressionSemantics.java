package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.EqualityComparison;
import it.unipr.ailab.jadescript.jadescript.TypeComparison;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Created on 28/12/16.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class EqualityComparisonExpressionSemantics extends ExpressionSemantics<EqualityComparison> {

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";

    public EqualityComparisonExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<EqualityComparison> input) {
        if(mustTraverse(input)){
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Arrays.asList(
                input.__(EqualityComparison::getLeft)
                        .extract(x->new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(TypeComparisonExpressionSemantics.class), x)),
                input.__(EqualityComparison::getRight)
                        .extract(x->new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(TypeComparisonExpressionSemantics.class), x))
        );

    }


    @Override
    public Maybe<String> compile(Maybe<EqualityComparison> input) {
        StringBuilder sb = new StringBuilder();
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        sb.append(module.get(TypeComparisonExpressionSemantics.class).compile(left).orElse(""));

        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);

        String equalityOp = input.__(EqualityComparison::getEqualityOp).orElse("");
        if(equalityOp.equals("≠")){
            equalityOp = "!=";
        }
        switch (equalityOp) {
            case EQUALS_OPERATOR: {
                String stringSoFar = sb.toString();
                sb = new StringBuilder();
                sb.append("java.util.Objects.equals(")
                        .append(stringSoFar)
                        .append(", ")
                        .append(module.get(TypeComparisonExpressionSemantics.class).compile(right).orElse(""))
                        .append(")");
            }
            break;
            case NOT_EQUALS_OPERATOR: {
                String stringSoFar = sb.toString();
                sb = new StringBuilder();
                sb.append("!java.util.Objects.equals(")
                        .append(stringSoFar)
                        .append(", ")
                        .append(module.get(TypeComparisonExpressionSemantics.class).compile(right).orElse(""))
                        .append(")");
            }
            break;
        }


        return Maybe.of(sb.toString());
    }

    @Override
    public IJadescriptType inferType(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);

        if (right.isNothing()) {
            return module.get(TypeComparisonExpressionSemantics.class).inferType(left);
        } else {
            return module.get(TypeHelper.class).BOOLEAN;
        }
    }


    @Override
    public boolean mustTraverse(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        return right.isNothing();
    }

    @Override
    public Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<EqualityComparison> input) {
        if(mustTraverse(input)){
            return Optional.ofNullable(input.__(EqualityComparison::getLeft))
                    .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(TypeComparisonExpressionSemantics.class), x));
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void validate(Maybe<EqualityComparison> input, ValidationMessageAcceptor acceptor) {
        if(input == null) return;
        module.get(TypeComparisonExpressionSemantics.class).validate(input.__(EqualityComparison::getLeft), acceptor);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        if(right.isPresent()) {
            module.get(TypeComparisonExpressionSemantics.class).validate(right, acceptor);
        }

    }

}
