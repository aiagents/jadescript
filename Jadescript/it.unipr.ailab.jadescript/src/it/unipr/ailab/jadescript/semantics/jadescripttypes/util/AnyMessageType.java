package it.unipr.ailab.jadescript.semantics.jadescripttypes.util;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.core.message.Message;

import java.util.List;

import static it.unipr.ailab.maybe.Maybe.some;

public class AnyMessageType extends UtilityType {

    private final TypeHelper typeHelper;


    private AnyMessageType(SemanticsModule module, TypeHelper typeHelper) {
        super(
            module,
            TypeHelper.builtinPrefix + "ANYMESSAGE",
            "Message",
            typeHelper.typeRef(Message.class)
        );
        this.typeHelper = typeHelper;
    }


    public AnyMessageType(SemanticsModule module) {
        this(module, module.get(TypeHelper.class));
    }


    @Override
    public boolean isErroneous() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return some(typeHelper.ONTOLOGY);
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


    @Override
    public TypeCategory category() {
        return new TypeCategoryAdapter() {
            @Override
            public boolean isMessage() {
                return true;
            }
        };
    }


    @Override
    public boolean isSendable() {
        return true;
    }

}
