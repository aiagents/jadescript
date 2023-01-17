package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.Call;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

/**
 * Created on 26/08/18.
 */
public class FunctionCallElement extends TrailersExpressionChainElement {
    private final Maybe<String> identifier;
    private final Maybe<SimpleArgumentList> simpleArgs;
    private final Maybe<NamedArgumentList> namedArgs;
    private final Maybe<? extends EObject> input;
    private final CallSemantics subSemantics;

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
        this.subSemantics = module.get(CallSemantics.class);
    }



    private Maybe<Call> generateMethodCall() {
        return Call.call(
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
