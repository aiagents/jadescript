package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.namespace.JvmModelBasedNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public interface IJadescriptType extends TypeArgument {
    @Override
    JvmTypeReference asJvmTypeReference();

    boolean typeEquals(IJadescriptType other);

    boolean isAssignableFrom(IJadescriptType other);

    String getCategoryName();

    SearchLocation getLocation();

    @Override
    String getID();

    default IJadescriptType postResolve(){
        //Override if needed.
        return this;
    }

    /**
     * Whether this is a Jadescript Basic type (integer, real, aid...)
     */
    boolean isBasicType();

    /**
     * Whether this is a slottable type (it can be part of a content)
     */
    boolean isSlottable();

    /**
     * Whether values of this type can be sent as content
     */
    boolean isSendable();

    /**
     * Whether Jadescript user code can contain references to this type
     */
    boolean isReferrable();

    /**
     * Whether Jadescript user code can operate on values of this type
     */
    boolean isManipulable();

    /**
     * Whether this is the type of value resulting from a compilation error
     */
    boolean isErroneous();


    Maybe<OntologyType> getDeclaringOntology();

    void validateType(Maybe<? extends EObject> input, ValidationMessageAcceptor acceptor);

    @Override
    String getJadescriptName();

    Maybe<IJadescriptType> getElementTypeIfCollection();

    boolean isCollection();

    String compileConversionType();

    String getSlotSchemaName();

    default String getGetSlotSchemaExpression(){
        return "getSchema(" + getSlotSchemaName() + ")";
    }

    @Override
    IJadescriptType ignoreBound();

    String compileAsJavaCast();

    TypeNamespace namespace();

    JvmModelBasedNamespace jvmNamespace();

    String getDebugPrint();
    //overridden by VOID type in TypeHelper

    default boolean isJavaVoid(){
        return false;
    }
}
