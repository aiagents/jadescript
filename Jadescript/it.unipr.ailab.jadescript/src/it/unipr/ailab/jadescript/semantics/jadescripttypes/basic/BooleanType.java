package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class BooleanType extends BasicType {

    public BooleanType(
        SemanticsModule module
    ) {
        super(
            module,
            builtinPrefix + "boolean",
            "boolean",
            "jade.content.onto.BasicOntology.BOOLEAN",
            module.get(JvmTypeHelper.class).typeRef(Boolean.class),
            "false"
        );
    }

}
