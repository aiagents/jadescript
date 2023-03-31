package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.EmptyTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.PermissiveJvmBasedNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import jadescript.content.JadescriptOntoElement;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class UnknownJVMType
    extends JvmBasedType
    implements EmptyCreatable {

    private final boolean permissive;


    public UnknownJVMType(
        SemanticsModule module,
        JvmTypeReference typeReference,
        boolean permissive
    ) {
        super(
            module,
            typeReference.getQualifiedName('.'),
            typeReference.getQualifiedName('.'),
            "OTHER",
            typeReference
        );

        this.permissive = permissive;
    }





    @Override
    public void addBultinProperty(Property prop) {

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
    public boolean hasProperties() {
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
    public boolean isBehaviour() {
        return false;
    }


    @Override
    public boolean isMessage() {
        return false;
    }


    @Override
    public boolean isMessageContent() {
        return false;
    }


    @Override
    public boolean isAgentEnv() {
        return false;
    }


    @Override
    public boolean isOntology() {
        return false;
    }


    @Override
    public TypeNamespace namespace() {
        if (permissive) {
            return new PermissiveJvmBasedNamespace(
                module,
                this.jvmNamespace(),
                () -> module.get(TypeHelper.class)
                    .jtFromJvmTypeRef(this.asJvmTypeReference())
                    .namespace().getSuperTypeNamespace(),
                getLocation()
            );
        } else {
            return module.get(EmptyTypeNamespace.class);
        }
    }


    @Override
    public boolean isErroneous() {
        return true;
    }


    @Override
    public boolean isUnknownJVM() {
        return true;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        if (module.get(TypeHelper.class).isAssignable(
            JadescriptOntoElement.class,
            asJvmTypeReference()
        )) {

            final JvmTypeNamespace jvmTypeNamespace = this.jvmNamespace();
            if (jvmTypeNamespace == null) {
                return nothing();
            }

            final Optional<IJadescriptType> metadata = jvmTypeNamespace
                //local search:
                .getMetadataMethod()
                .map(JvmOperation::getReturnType)
                .map(jvmTypeNamespace::resolveType);

            if (metadata.isPresent()) {
                if (metadata.get() instanceof OntologyType) {
                    return some(((OntologyType) metadata.get()));
                }
            }
        }
        return Maybe.nothing();

    }


    @Override
    public String compileNewEmptyInstance() {
        //falls back to runtime resolution
        return "jadescript.java.Jadescript.<" +
            compileToJavaTypeReference() +
            ">createEmptyValue(" +
            compileToJavaTypeReference() + ".class)" +
            "/*<- runtime resolution - a cyclic type " +
            "reference was probably detected*/";

    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }

}
