package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
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



    private Maybe<MethodCall> generateMethodCall() {
        return MethodCall.methodCall(
            input,
            identifier,
            simpleArgs,
            namedArgs,
            false
        );
    }


    @Override
    public AssignableExpressionSemantics.SemanticsBoundToAssignableExpression<?>
    resolveChain(ReversedTrailerChain withoutFirst) {
        return new AssignableExpressionSemantics
            .SemanticsBoundToAssignableExpression<>(
            subSemantics, generateMethodCall()
        );
    }
}
