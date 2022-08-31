package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import org.eclipse.xtext.common.types.JvmTypeReference;

public interface TypeArgument{
    String getID();
    String getJadescriptName();
    JvmTypeReference asJvmTypeReference();
    default String compileToJavaTypeReference(){
        return asJvmTypeReference().getQualifiedName('.');
    }

    IJadescriptType ignoreBound();
}
