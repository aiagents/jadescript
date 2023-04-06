package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class IntegerType extends BasicType {

    public IntegerType(
        SemanticsModule module
        ) {
        super(
            module,
            builtinPrefix + "integer",
            "integer",
            "jade.content.onto.BasicOntology.INTEGER",
            module.get(JvmTypeHelper.class).typeRef(Integer.class),
            "0"
        );
    }

}
