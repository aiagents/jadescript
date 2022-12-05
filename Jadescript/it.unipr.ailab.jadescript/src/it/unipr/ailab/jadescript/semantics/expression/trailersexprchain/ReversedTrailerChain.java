package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.Trailer;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
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
public class ReversedTrailerChain {
    private final List<Maybe<TrailersExpressionChainElement>> elements = new ArrayList<>();
    private final SemanticsModule module;

    public ReversedTrailerChain(SemanticsModule module) {
        this.module = module;
    }


    public ReversedTrailerChain withoutFirst() {
        ReversedTrailerChain result = new ReversedTrailerChain(module);

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
            ReversedTrailerChain withoutFirst = withoutFirst();
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

    public boolean isHoled() {
        if (elements.isEmpty()) return false;
        return elements.get(0).__(el -> el.isHoled(withoutFirst())).extract(Maybe.nullAsFalse);
    }

    public boolean isUnbounded() {
        if (elements.isEmpty()) return false;
        return elements.get(0).__(el -> el.isUnbounded(withoutFirst())).extract(Maybe.nullAsFalse);
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input
    ) {
        if (elements.isEmpty()) return null;//TODO add empty output generator method
        return elements.get(0).__(el -> el.compilePatternMatchInternal(input, withoutFirst()))
                //TODO add empty output generator method:
                .toNullable();
    }

    public PatternType inferPatternTypeInternal(PatternMatchInput<AtomExpr, ?, ?> input) {
        if (elements.isEmpty()) return PatternType.simple(module.get(TypeHelper.class).NOTHING);
        return elements.get(0).__(el -> el.inferPatternTypeInternal(input, withoutFirst())).orElseGet(() ->
                PatternType.simple(module.get(TypeHelper.class).NOTHING));
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        if (elements.isEmpty()) return null;//TODO add empty output generator method
        return elements.get(0).__(el -> el.validatePatternMatchInternal(input, withoutFirst(), acceptor))
                //TODO add empty output generator method:
                .toNullable();
    }

    public boolean isTypelyHoled() {
        if(elements.isEmpty()) return false;
        return elements.get(0).__(el -> el.isTypelyHoled(withoutFirst())).extract(Maybe.nullAsFalse);
    }
}
