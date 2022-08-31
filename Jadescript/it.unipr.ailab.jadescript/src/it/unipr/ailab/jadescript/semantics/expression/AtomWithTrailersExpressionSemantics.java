package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.Trailer;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.trailersexprchain.ReversedDotNotationChain;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 26/08/18.
 */
@Singleton
public class AtomWithTrailersExpressionSemantics extends AssignableExpressionSemantics<AtomExpr> {


    public AtomWithTrailersExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<AtomExpr> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        return buildChain(input).getSubExpressions();
    }

    @Override
    public Maybe<String> compile(Maybe<AtomExpr> input) {
        return buildChain(input).compile();
    }


    @Override
    public IJadescriptType inferType(Maybe<AtomExpr> input) {
        return buildChain(input).inferType();
    }


    @Override
    public boolean mustTraverse(Maybe<AtomExpr> input) {
        List<Maybe<Trailer>> trailers = toListOfMaybes(input.__(AtomExpr::getTrailers));
        return trailers.isEmpty();
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<AtomExpr> input) {
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(module.get(PrimaryExpressionSemantics.class), input.__(AtomExpr::getAtom)));
        } else {
            return Optional.empty();
        }
    }


    @Override
    public void validate(Maybe<AtomExpr> input, ValidationMessageAcceptor acceptor) {
        buildChain(input).validate(acceptor);
    }

    @Override
    public Maybe<String> compileAssignment(Maybe<AtomExpr> input, String compiledExpression, IJadescriptType exprType) {
        return buildChain(input).compileAssignment(compiledExpression, exprType);
    }

    @Override
    public void validateAssignment(
            Maybe<AtomExpr> input,
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null || expression == null) return;

        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
        module.get(RValueExpressionSemantics.class).validate(expression, subValidation);

        if (!subValidation.thereAreErrors()) {
            IJadescriptType typeOfRExpression = module.get(RValueExpressionSemantics.class).inferType(expression);

            validateArithmeticAssignmentRExpression(assignmentOperator, expression, acceptor, typeOfRExpression);

            buildChain(input).validateAssignment(assignmentOperator, expression, typeOfRExpression, acceptor);

        }

    }

    @Override
    public void syntacticValidateLValue(Maybe<AtomExpr> input, ValidationMessageAcceptor acceptor) {
        if (input.__(AtomExpr::getTrailers).__(List::isEmpty).extract(nullAsTrue)) {

            module.get(PrimaryExpressionSemantics.class).syntacticValidateLValue(input.__(AtomExpr::getAtom), acceptor);
        } else {
            List<Maybe<Trailer>> trailers = toListOfMaybes(input.__(AtomExpr::getTrailers));
            module.get(ValidationHelper.class).assertion(
                    trailers.get(trailers.size() - 1)
                            .__(Trailer::isIsACall)
                            .__(not),
                    "InvalidLValueExpression",
                    "this is not a valid l-value expression",
                    input,
                    acceptor
            );
        }
    }


    private ReversedDotNotationChain buildChain(Maybe<AtomExpr> input) {
        ReversedDotNotationChain chain = new ReversedDotNotationChain(module);
        boolean isAtomEaten = false;
        Maybe<Primary> atom = input.__(AtomExpr::getAtom);
        List<Maybe<Trailer>> trailers = toListOfMaybes(input.__(AtomExpr::getTrailers));
        for (int i = trailers.size() - 1; i >= 0; i--) {
            Maybe<Trailer> currentTrailer = trailers.get(i);
            if (currentTrailer.__(Trailer::isIsACall).extract(nullAsFalse)) {
                i--; //get previous (by eating a trailer)
                chain.addGlobalMethodCall(atom, currentTrailer);
                isAtomEaten = true;
            } else if (currentTrailer.__(Trailer::isIsASubscription).extract(nullAsFalse)) {
                chain.addSubscription(currentTrailer);
            }
        }
        if (!isAtomEaten) {
            chain.addPrimary(atom);
        }
        return chain;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public boolean isAlwaysPure(Maybe<AtomExpr> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return traversed.get().getSemantics().isAlwaysPure((Maybe)traversed.get().getInput());
            }
        }
        return buildChain(input).isAlwaysPure();
    }
}
