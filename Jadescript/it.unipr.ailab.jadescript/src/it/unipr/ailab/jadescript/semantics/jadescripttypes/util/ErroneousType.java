package it.unipr.ailab.jadescript.semantics.jadescripttypes.util;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;

import java.util.List;

import static it.unipr.ailab.maybe.Maybe.nothing;

public class ErroneousType extends UtilityType {

    private final String description;
    private final IJadescriptType relationshipDelegate;


    private ErroneousType(
        SemanticsModule module,
        String typeID,
        String simpleName,
        JvmTypeReference jvmType,
        String description,
        IJadescriptType relationshipDelegate
    ) {
        super(module, typeID, simpleName, jvmType);
        this.description = description;
        this.relationshipDelegate = relationshipDelegate;
    }


    public static ErroneousType top(
        SemanticsModule module,
        String description,
        IJadescriptType giveMeANY
    ) {
        return new ErroneousType(
            module,
            TypeHelper.builtinPrefix + "TOP_ERR",
            "(error)any",
            module.get(JvmTypeReferenceBuilder.class).typeRef(
                "/*ANY*/java.lang.Object"),
            description,
            giveMeANY
        );
    }


    public static ErroneousType bottom(
        SemanticsModule module,
        String description,
        IJadescriptType giveMeNOTHING
    ) {
        return new ErroneousType(
            module,
            TypeHelper.builtinPrefix + "BOTTOM_ERR",
            "(error)nothing",
            module.get(JvmTypeReferenceBuilder.class).typeRef(
                "/*NOTHING*/java.lang.Object"),
            description,
            giveMeNOTHING
        );
    }


    @Override
    public boolean typeEquals(IJadescriptType other) {
        return relationshipDelegate.typeEquals(other);
    }


    @Override
    public boolean isSupertypeOrEqualTo(IJadescriptType other) {
        return relationshipDelegate.isSupertypeOrEqualTo(other);
    }


    @Override
    public boolean validateType(
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    ) {
        return module.get(ValidationHelper.class).emitError(
            "InvalidType",
            description,
            input,
            acceptor
        );
    }


    @Override
    public boolean isSendable() {
        return false;
    }


    @Override
    public boolean isErroneous() {
        return true;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return nothing();
    }


    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
            module,
            nothing(),
            List.of(),
            List.of(),
            getLocation()
        );
    }

}
