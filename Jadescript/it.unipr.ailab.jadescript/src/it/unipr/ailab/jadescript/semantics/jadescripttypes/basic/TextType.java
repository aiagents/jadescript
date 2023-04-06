package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class TextType extends BasicType {

    public TextType(
        SemanticsModule module
    ) {
        super(
            module,
            builtinPrefix + "text",
            "text",
            "jade.content.onto.BasicOntology.STRING",
            module.get(JvmTypeHelper.class).typeRef(String.class),
            "\"\""
        );

addBultinProperty(Property.readonlyProperty(
    //TODO fix -> builtin properties should be moved in namespace
    // (so they are generated with types when type provider is ready)
                "length",
                integer(),
                textType.getLocation(),
                Property.compileGetWithCustomMethod("length")
            ));
    }

}
