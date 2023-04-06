package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.eclipse.xtext.common.types.JvmTypeReference;

public interface TypeArgument {

    String getID();

    String getFullJadescriptName();

    JvmTypeReference asJvmTypeReference();

    default String compileToJavaTypeReference() {
        return asJvmTypeReference().getQualifiedName('.');
    }

    IJadescriptType ignoreBound();

    String getDebugPrint();



}
