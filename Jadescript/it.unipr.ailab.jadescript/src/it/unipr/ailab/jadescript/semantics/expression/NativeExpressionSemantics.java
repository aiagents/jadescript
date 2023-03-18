package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.NativeExpression;
import it.unipr.ailab.jadescript.semantics.NativeCallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.NativeCall;
import it.unipr.ailab.maybe.Maybe;

import java.util.Optional;

/**
 * Created on 2019-05-20.
 */
@Singleton
public class NativeExpressionSemantics
    extends AssignableExpressionSemantics.AssignableAdapter<NativeExpression> {


    public NativeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected boolean mustTraverse(Maybe<NativeExpression> input) {
        return true;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(
        Maybe<NativeExpression> input
    ) {
        return Optional.of(new SemanticsBoundToAssignableExpression<>(
            module.get(NativeCallSemantics.class),
            NativeCall.fromExpression(input)
        ));
    }

}
