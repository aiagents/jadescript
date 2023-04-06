package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class RealType extends BasicType {

    public RealType(
        SemanticsModule module
    ) {
        super(module, builtinPrefix + "real",
            "real",
            "jade.content.onto.BasicOntology.FLOAT",
            module.get(JvmTypeHelper.class).typeRef(Float.class),
            "0.0f"
        );
    }

}
