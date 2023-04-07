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

import java.util.List;

import static it.unipr.ailab.maybe.Maybe.nothing;

public class JavaVoidType extends UtilityType {


    public JavaVoidType(SemanticsModule module) {
        super(
            module,
            TypeHelper.VOID_TYPEID,
            "(error)javaVoid",
            module.get(JvmTypeHelper.class).typeRef(void.class)
        );
    }


    @Override
    public TypeCategory category() {
        return new TypeCategoryAdapter() {
            @Override
            public boolean isJavaVoid() {
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
            Maybe.nothing(),
            List.of(),
            List.of(),
            getLocation()
        );
    }

}
