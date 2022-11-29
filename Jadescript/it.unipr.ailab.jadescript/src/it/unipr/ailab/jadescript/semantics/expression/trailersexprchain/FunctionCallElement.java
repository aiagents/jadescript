package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;

/**
 * Created on 26/08/18.
 */
public class FunctionCallElement extends TrailersExpressionChainElement {
    private final Maybe<String> identifier;
    private final Maybe<SimpleArgumentList> simpleArgs;
    private final Maybe<NamedArgumentList> namedArgs;
    private final Maybe<? extends EObject> input;
    private final MethodInvocationSemantics subSemantics;

    public FunctionCallElement(
            SemanticsModule module,
            Maybe<String> identifier,
            Maybe<SimpleArgumentList> simpleArgs,
            Maybe<NamedArgumentList> namedArgs,
            Maybe<? extends EObject> input
    ) {
        super(module);
        this.identifier = identifier;
        this.simpleArgs = simpleArgs;
        this.namedArgs = namedArgs;
        this.input = input;
        this.subSemantics = module.get(MethodInvocationSemantics.class);
    }


    @Override
    public String compile(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored

        return subSemantics.compile(generateMethodCall());
    }

    private Maybe<MethodCall> generateMethodCall() {
        return MethodCall.methodCall(input, identifier, simpleArgs, namedArgs, false);
    }


    @Override
    public IJadescriptType inferType(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored

        return subSemantics.inferType(generateMethodCall());
    }

    @Override
    public void validate(ReversedTrailerChain rest, ValidationMessageAcceptor acceptor) {
        //rest should be empty, so it's ignored

        subSemantics.validate(generateMethodCall(), acceptor);
    }


    @Override
    public void validateAssignment(
            ReversedTrailerChain rest,
            String assignmentOperator,
            Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr,
            ValidationMessageAcceptor acceptor
    ) {
        //CANNOT ASSIGN TO A METHOD CALL
        //this is never called because of prior check via syntacticValidateLValue(...)

        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "this is not a valid l-value expression",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidLValueExpression"
            );
        });
    }


    @Override
    public void syntacticValidateLValue(InterceptAcceptor acceptor) {
        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "this is not a valid l-value expression",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidLValueExpression"
            );
        });
    }

    @Override
    public String compileAssignment(
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType
    ) {
        return ""; //CANNOT ASSIGN TO A METHOD CALL
    }

    @Override
    public boolean isAlwaysPure(ReversedTrailerChain rest) {
        // if one day there will be declared (or inferred) purity of functions, this implementation will change
        return false;
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return subSemantics.getSubExpressions(generateMethodCall());
    }

    @Override
    public boolean isHoled(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return subSemantics.isHoled(generateMethodCall());
    }

    @Override
    public boolean isUnbounded(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return subSemantics.isUnbounded(generateMethodCall());
    }

    @Override
    public PatternMatchOutput<PatternMatchOutput.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest
    ) {
        //rest should be empty, so it's ignored
        return subSemantics.compilePatternMatchInternal(input.mapPattern(__ -> generateMethodCall().toNullable()));
    }

    @Override
    public PatternType inferPatternType(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest
    ) {
        //rest should be empty, so it's ignored
        return subSemantics.inferPatternType(input.mapPattern(__ -> generateMethodCall().toNullable()));
    }

    @Override
    public PatternMatchOutput<PatternMatchOutput.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    ) {
        //rest should be empty, so it's ignored
        return subSemantics.validatePatternMatchInternal(
                input.mapPattern(__ -> generateMethodCall().toNullable()),
                acceptor
        );
    }

}
