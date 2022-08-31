package it.unipr.ailab.jadescript.semantics.context.search;

import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

public class JvmTypeLocation extends FQNameLocation{
    public JvmTypeLocation(JvmTypeReference jvmTypeReference) {
        super(jvmTypeReference.getQualifiedName('.'));
    }

    public JvmTypeLocation(JvmType jvmType){
        super(jvmType.getQualifiedName('.'));
    }

    @Override
    public String toString() {
        return "(JVM type declaration: "+getFullyQualifiedName()+")";
    }
}
