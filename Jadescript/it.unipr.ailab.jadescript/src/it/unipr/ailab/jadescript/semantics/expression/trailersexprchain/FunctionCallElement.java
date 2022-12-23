package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.INVALID;

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
    public String compile(ReversedTrailerChain rest, CompilationOutputAcceptor acceptor) {
        //rest should be empty, so it's ignored
        return subSemantics.compile(generateMethodCall(), acceptor);
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
    public boolean validate(ReversedTrailerChain rest, ValidationMessageAcceptor acceptor) {
        //rest should be empty, so it's ignored

        return subSemantics.validate(generateMethodCall(), acceptor);
    }


    @Override
    public boolean validateAssignment(
            ReversedTrailerChain rest,
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
        return INVALID;
    }


    @Override
    public boolean syntacticValidateLValue(ValidationMessageAcceptor acceptor) {
        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "this is not a valid l-value expression",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidLValueExpression"
            );
        });
        return INVALID;
    }

    @Override
    public void compileAssignment(
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        acceptor.accept(w.commentStmt("CANNOT ASSIGN TO A METHOD CALL"));
    }

    @Override
    public boolean isAlwaysPure(ReversedTrailerChain rest) {
        // if one day there will be declared (or inferred) purity of functions, this implementation will change
        return false;
    }

    @Override
    public Stream<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(ReversedTrailerChain rest) {
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
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            CompilationOutputAcceptor acceptor
    ) {
        //rest should be empty, so it's ignored
        return subSemantics.compilePatternMatchInternal(input.replacePattern(generateMethodCall()), acceptor);
    }

    @Override
    public PatternType inferPatternTypeInternal(
            Maybe<AtomExpr> input,
            ReversedTrailerChain rest
    ) {
        //rest should be empty, so it's ignored
        return subSemantics.inferPatternTypeInternal(input.__(__ -> generateMethodCall().toNullable()));
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    ) {
        //rest should be empty, so it's ignored
        return subSemantics.validatePatternMatchInternal(
                input.replacePattern(generateMethodCall()),
                acceptor
        );
    }

    @Override
    public boolean isTypelyHoled(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return subSemantics.isTypelyHoled(generateMethodCall());
    }

    @Override
    public boolean isValidLexpr(ReversedTrailerChain rest) {
        return subSemantics.isValidLexpr(input.__(__ -> generateMethodCall().toNullable()));
    }

    @Override
    public boolean isPatternEvaluationPure(ReversedTrailerChain rest) {
        return subSemantics.isPatternEvaluationPure(input.__(__ -> generateMethodCall().toNullable()));
    }

    @Override
    public boolean canBeHoled(ReversedTrailerChain withoutFirst) {
        return subSemantics.canBeHoled(generateMethodCall());
    }

    @Override
    public boolean containsNotHoledAssignableParts(ReversedTrailerChain withoutFirst) {
        return subSemantics.containsNotHoledAssignableParts(generateMethodCall());
    }

}
