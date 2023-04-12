package it.unipr.ailab.jadescript.semantics.jadescripttypes.util;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.namespace.MessageTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.core.message.Message;

import static it.unipr.ailab.maybe.Maybe.some;

public class AnyMessageType extends UtilityType implements MessageType {


    private AnyMessageType(SemanticsModule module) {
        super(
            module,
            TypeHelper.builtinPrefix + "ANYMESSAGE",
            "Message",
            module.get(JvmTypeHelper.class).typeRef(Message.class)
        );
    }




    @Override
    public boolean isErroneous() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return some(module.get(BuiltinTypeProvider.class).ontology());
    }


    @Override
    public IJadescriptType getContentType() {
        return module.get(BuiltinTypeProvider.class).anyOntologyElement();
    }


    @Override
    public MessageTypeNamespace namespace() {
        return MessageTypeNamespace.messageTypeNamespace(
            module,
            module.get(TypeHelper.class).covariant(
                module.get(BuiltinTypeProvider.class).anyOntologyElement()
            ),
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


    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.core.message.Message<>(" +
            "jadescript.lang.Performative.UNKNOWN)";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }

}
