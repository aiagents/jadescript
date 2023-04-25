package it.unipr.ailab.jadescript.semantics.jadescripttypes.util;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.content.JadescriptOntoElement;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;

import static it.unipr.ailab.maybe.Maybe.nothing;

public class AnyOntologyElementType extends UtilityType {


    public AnyOntologyElementType(
        SemanticsModule module
    ) {
        this(
            module,
            module.get(JvmTypeHelper.class).typeRef(JadescriptOntoElement.class)
        );
    }


    private AnyOntologyElementType(
        SemanticsModule module,
        final JvmTypeReference typeRef
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + "ANY_ONTO_ELEMENT",
            "«any ontology element»",
            typeRef
        );
    }


    @Override
    public TypeCategory category() {
        return new TypeCategoryAdapter() {
            @Override
            public boolean isOntoContent() {
                return true;
            }
        };
    }


    @Override
    public boolean isSendable() {
        return false;
    }


    @Override
    public boolean isErroneous() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return nothing();
    }


    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
            module,
            Maybe.nothing(),
            List.of(),
            List.of(),
            getLocation()
        );
    }

}
