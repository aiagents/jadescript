package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.content.JadescriptOntoElement;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.of;

public class UnknownJVMType extends JadescriptType implements EmptyCreatable {
    private final JvmTypeReference typeReference;

    public UnknownJVMType(SemanticsModule module, JvmTypeReference typeReference) {
        super(module, typeReference.getQualifiedName('.'), typeReference.getQualifiedName('.'), "OTHER");
        this.typeReference = typeReference;
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        return typeReference;
    }

    @Override
    public IJadescriptType postResolve() {
        final JvmTypeReference attemptedResolution = TypeHelper.attemptResolveTypeRef(module, asJvmTypeReference());
        if(attemptedResolution == typeReference){
            return this;
        }
        return module.get(TypeHelper.class).jtFromJvmTypeRef(attemptedResolution);
    }

    @Override
    public void addProperty(Property prop) {

    }


    @Override
    public boolean isBasicType() {
        return false;
    }

    @Override
    public boolean isSlottable() {
        return false;
    }

    @Override
    public boolean isSendable() {
        return false;
    }

    @Override
    public boolean isReferrable() {
        return false;
    }

    @Override
    public boolean isManipulable() {
        return false;
    }

    @Override
    public String getJadescriptName() {
        return "[JVM] " + super.getJadescriptName();
    }


    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public TypeNamespace namespace() {
        return JvmTypeNamespace.fromTypeReference(module, asJvmTypeReference());
    }

    @Override
    public boolean isErroneous() {
        return true;
    }

    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        if (module.get(TypeHelper.class).isAssignable(
                JadescriptOntoElement.class,
                asJvmTypeReference()
        )) {

            final Optional<IJadescriptType> metadata = this.namespace()
                    //local search:
                    .searchCallable(
                            name -> name.startsWith("__metadata"),
                            null,
                            null,
                            null
                    ).findFirst()
                    .map(CallableSymbol::returnType);
            if (metadata.isPresent()) {
                if (metadata.get() instanceof OntologyType) {
                    return of(((OntologyType) metadata.get()));
                }
            }
        }
        return Maybe.nothing();

    }

    @Override
    public String compileNewEmptyInstance() {
        //falls back to runtime resolution
        return "it.unipr.ailab.jadescript.javaapi.Jadescript.<" +
                compileToJavaTypeReference() +
                ">createEmptyValue(" +
                compileToJavaTypeReference() + ".class)" +
                "/*<- runtime resolution - a cyclic type reference was probably detected*/";

    }
}
