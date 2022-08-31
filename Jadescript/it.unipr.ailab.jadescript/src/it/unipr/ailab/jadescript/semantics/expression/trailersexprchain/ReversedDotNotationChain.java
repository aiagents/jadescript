package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.Trailer;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 26/08/18.
 *
 */
public class ReversedDotNotationChain {
    private final List<Maybe<TrailersExpressionChainElement>> elements = new ArrayList<>();
    private final SemanticsModule module;

    public ReversedDotNotationChain(SemanticsModule module) {
        this.module = module;
    }


    public ReversedDotNotationChain withoutFirst() {
        ReversedDotNotationChain result = new ReversedDotNotationChain(module);

        result.elements.addAll(this.elements.subList(1, this.elements.size()));

        return result;
    }


    public Maybe<String> compile() {
        if (elements.isEmpty()) return Maybe.of("");
        return elements.get(0)
                .__(TrailersExpressionChainElement::compile, withoutFirst());
    }

    public IJadescriptType inferType() {
        if (elements.isEmpty()) return module.get(TypeHelper.class).ANY;
        return elements.get(0)
                .__(TrailersExpressionChainElement::inferType, withoutFirst())
                .orElse(module.get(TypeHelper.class).ANY);
    }

    public void validate(ValidationMessageAcceptor acceptor) {
        if (elements.isEmpty()) return;
        elements.get(0).safeDo(e->e.validate(withoutFirst(), acceptor));
    }

    public void validateAssignment(
            String assignmentOperator,
            Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr,
            ValidationMessageAcceptor acceptor
    ) {
        if (elements.isEmpty()) return;

        InterceptAcceptor syntaxLValueSubValidation = new InterceptAcceptor(acceptor);

        elements.get(elements.size() - 1).safeDo(
                TrailersExpressionChainElement::syntacticValidateLValue,
                syntaxLValueSubValidation
        );

        if (syntaxLValueSubValidation.thereAreErrors()) {
            return;
        }

        elements.get(0).safeDo(e->e.validateAssignment(
                withoutFirst(),
                assignmentOperator,
                rValueExpression,
                typeOfRExpr,
                acceptor)
        );
    }

    public Maybe<String> compileAssignment(String compiledExpression, IJadescriptType exprType) {
        if (elements.isEmpty()) return Maybe.of("");
        return elements.get(0).__(e->e.compileAssignment(withoutFirst(), compiledExpression, exprType));
    }

    public void addPrimary(Maybe<Primary> atom) {
        elements.add(Maybe.of(new PrimaryChainElement(module, atom)));
    }

    public void addSubscription(Maybe<Trailer> currentTrailer) {
        elements.add(Maybe.of(new SubscriptionElement(module, currentTrailer.__(Trailer::getKey))));
    }

    public void addGlobalMethodCall(Maybe<Primary> atom, Maybe<Trailer> parentheses) {
        elements.add(Maybe.of(new FunctionCallElement(
                module,
                atom.__(Primary::getIdentifier),
                parentheses.__(Trailer::getSimpleArgs),
                parentheses.__(Trailer::getNamedArgs),
                atom
        )));
    }




    public List<Maybe<TrailersExpressionChainElement>> getElements() {
        return elements;
    }

    public boolean isAlwaysPure() {
        if (elements.isEmpty()) return true;
        return elements.get(0).__(TrailersExpressionChainElement::isAlwaysPure, withoutFirst()).extract(Maybe.nullAsTrue);
    }

    public List<SemanticsBoundToExpression<?>> getSubExpressions() {
        List<SemanticsBoundToExpression<?>> result = new ArrayList<>();
        if(!elements.isEmpty()) {
            ReversedDotNotationChain withoutFirst = withoutFirst();
            for (Maybe<TrailersExpressionChainElement> element : elements) {
                element.safeDo(elementSafe -> {
                    result.addAll(elementSafe.getSubExpressions(withoutFirst));
                });
            }

            return result;
        }else{
            return Collections.emptyList();
        }
    }
}
