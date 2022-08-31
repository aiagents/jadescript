package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ListLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.iterate;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 31/03/18.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class ListLiteralExpressionSemantics extends ExpressionSemantics<ListLiteral> {


    public ListLiteralExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<ListLiteral> input) {
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Maybe.toListOfMaybes(values).stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    @Override
    public Maybe<String> compile(Maybe<ListLiteral> input) {
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter = input.__(ListLiteral::getTypeParameter);
        if (values.__(List::isEmpty).extract(Maybe.nullAsTrue)) {
            final IJadescriptType elementType = module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(typeParameter);
            return Maybe.of("new java.util.ArrayList<" + elementType.compileToJavaTypeReference() + ">()");
        }

        StringBuilder sb = new StringBuilder("java.util.Arrays.asList(");
        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);
        for (int i = 0; i < valuesList.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(valuesList.get(i)));
        }
        sb.append(")");

        return Maybe.of("new java.util.ArrayList<>("+sb+")");
    }

    @Override
    public IJadescriptType inferType(Maybe<ListLiteral> input) {
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter = input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier = input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);
        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);

        if (hasTypeSpecifier || valuesList.isEmpty()) {
            return module.get(TypeHelper.class).LIST.apply(
                    Collections.singletonList(module.get(TypeExpressionSemantics.class).toJadescriptType(typeParameter))
            );
        }

        IJadescriptType lub = module.get(RValueExpressionSemantics.class).inferType(valuesList.get(0));
        for (int i = 1; i < valuesList.size(); i++) {
            lub = module.get(TypeHelper.class).getLUB(lub, module.get(RValueExpressionSemantics.class).inferType(valuesList.get(i)));
        }
        return module.get(TypeHelper.class).LIST.apply(
                Collections.singletonList(lub)
        );
    }

    @Override
    public boolean mustTraverse(Maybe<ListLiteral> input) {
        return false;
    }

    @Override
    public Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<ListLiteral> input) {
        return Optional.empty();
    }


    @Override
    public void validate(Maybe<ListLiteral> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter = input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier = input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);

        InterceptAcceptor stage1Validation = new InterceptAcceptor(acceptor);
        for (Maybe<RValueExpression> jadescriptRValueExpression : iterate(values)) {
            module.get(RValueExpressionSemantics.class).validate(jadescriptRValueExpression, stage1Validation);
        }

        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);
        module.get(ValidationHelper.class).assertion(
                (!valuesList.isEmpty() && !valuesList.stream().allMatch(Maybe::isNothing))
                        || hasTypeSpecifier,
                "ListLiteralCannotComputeType",
                "Missing type specification for empty list listeral",
                input,
                stage1Validation
        );

        if (!stage1Validation.thereAreErrors()
                && !valuesList.isEmpty() && !valuesList.stream().allMatch(Maybe::isNothing)) {
            IJadescriptType lub = module.get(RValueExpressionSemantics.class).inferType(valuesList.get(0));
            for (int i = 1; i < valuesList.size(); i++) {
                lub = module.get(TypeHelper.class).getLUB(lub, module.get(RValueExpressionSemantics.class).inferType(valuesList.get(i)));
            }

            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(ValidationHelper.class).assertion(
                    !lub.isErroneous(),
                    "ListLiteralCannotComputeType",
                    "Can not find a valid common parent type of the elements in the list literal.",
                    input,
                    interceptAcceptor
            );

            module.get(TypeExpressionSemantics.class).validate(typeParameter, interceptAcceptor);

            if (!interceptAcceptor.thereAreErrors() && hasTypeSpecifier) {
                module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeExpressionSemantics.class).toJadescriptType(typeParameter),
                        lub,
                        "ListLiteralTypeMismatch",
                        input,
                        acceptor
                );
            }
        }
    }
}
