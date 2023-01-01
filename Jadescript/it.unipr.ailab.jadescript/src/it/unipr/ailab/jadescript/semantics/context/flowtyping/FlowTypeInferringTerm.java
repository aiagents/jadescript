package it.unipr.ailab.jadescript.semantics.context.flowtyping;

import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.eclipse.xtext.common.types.JvmTypeReference;

/**
 * Created on 04/09/2019.
 */
public class FlowTypeInferringTerm {
    private IJadescriptType typeRef;
    private boolean isNegated;

    private FlowTypeInferringTerm() {
    }

    public static FlowTypeInferringTerm of(IJadescriptType typeRef) {
        FlowTypeInferringTerm result = new FlowTypeInferringTerm();
        result.typeRef = typeRef;
        result.isNegated = false;
        return result;
    }

    public static FlowTypeInferringTerm of(
        TypeHelper typeHelper,
        JvmTypeReference typeRef
    ) {
        FlowTypeInferringTerm result = new FlowTypeInferringTerm();
        result.typeRef = typeHelper.jtFromJvmTypeRef(typeRef);
        result.isNegated = false;
        return result;
    }

    public static FlowTypeInferringTerm ofNegated(IJadescriptType typeRef) {
        FlowTypeInferringTerm result = new FlowTypeInferringTerm();
        result.typeRef = typeRef;
        result.isNegated = true;
        return result;
    }

    public static FlowTypeInferringTerm ofNegated(
        TypeHelper typeHelper,
        JvmTypeReference typeRef
    ) {
        FlowTypeInferringTerm result = new FlowTypeInferringTerm();
        result.typeRef = typeHelper.jtFromJvmTypeRef(typeRef);
        result.isNegated = true;
        return result;
    }

    public IJadescriptType getType() {
        return typeRef;
    }

    public boolean isNegated() {
        return isNegated;
    }

    public FlowTypeInferringTerm negate() {
        if (isNegated) {
            return of(typeRef);
        } else {
            return ofNegated(typeRef);
        }
    }
}
