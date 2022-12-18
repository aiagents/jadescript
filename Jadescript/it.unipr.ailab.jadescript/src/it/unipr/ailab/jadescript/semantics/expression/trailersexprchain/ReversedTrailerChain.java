package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.Trailer;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
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


    public ExpressionCompilationResult compile(StatementCompilationOutputAcceptor acceptor) {
        if (elements.isEmpty()) return ExpressionCompilationResult.empty();
        return elements.get(0)
                .__(e -> e.compile(withoutFirst(), acceptor)).orElseGet(ExpressionCompilationResult::empty);
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
                rValueExpression,
                typeOfRExpr,
                acceptor)
        );
    }

    public void compileAssignment(
            String compiledExpression,
            IJadescriptType exprType,
            StatementCompilationOutputAcceptor acceptor
    ) {
        if (elements.isEmpty()) return;
        elements.get(0).safeDo(safeElement-> {
            safeElement.compileAssignment(withoutFirst(), compiledExpression, exprType, acceptor);
        });
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

    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            StatementCompilationOutputAcceptor acceptor
    ) {
        if (elements.isEmpty()) return input.createEmptyCompileOutput();
        final Maybe<? extends PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>>
                patternMatchOutputMaybe = elements.get(0)
                .__(el -> el.compilePatternMatchInternal(input, withoutFirst(), acceptor));
        if(patternMatchOutputMaybe.isPresent()){
            return patternMatchOutputMaybe.toNullable();
        }else{
            return input.createEmptyCompileOutput();
        }
    }

    public PatternType inferPatternTypeInternal(
            Maybe<AtomExpr> input
    ) {
        if (elements.isEmpty()) return PatternType.simple(module.get(TypeHelper.class).NOTHING);
        return elements.get(0).__(el -> el.inferPatternTypeInternal(input, withoutFirst())).orElseGet(() ->
                PatternType.simple(module.get(TypeHelper.class).NOTHING));
    }

    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        if (elements.isEmpty()) return input.createEmptyValidationOutput();
        final Maybe<? extends PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>>
                patternMatchOutputMaybe = elements.get(0)
                .__(el -> el.validatePatternMatchInternal(input, withoutFirst(), acceptor));

        if(patternMatchOutputMaybe.isPresent()){
            return patternMatchOutputMaybe.toNullable();
        }else{
            return input.createEmptyValidationOutput();
        }
    }

    public boolean isTypelyHoled() {
        if(elements.isEmpty()) return false;
        return elements.get(0).__(el -> el.isTypelyHoled(withoutFirst())).extract(Maybe.nullAsFalse);
    }

    public boolean isValidLExpr() {
        if(elements.isEmpty()) return false;
        return elements.get(0).__(el -> el.isValidLexpr(withoutFirst())).extract(Maybe.nullAsFalse);
    }

    public boolean isPatternEvaluationPure() {
        if(elements.isEmpty()) return true;
        return elements.get(0).__(el -> el.isPatternEvaluationPure(withoutFirst())).extract(Maybe.nullAsTrue);
    }
}
