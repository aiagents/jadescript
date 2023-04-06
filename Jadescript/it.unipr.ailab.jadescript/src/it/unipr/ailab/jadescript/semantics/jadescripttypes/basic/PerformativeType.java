package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class PerformativeType extends BasicType {

    public PerformativeType(
        SemanticsModule module
    ) {
        super(
            module,
            builtinPrefix + "performative",
            "performative",
            "jadescript.content.onto.Ontology.PERFORMATIVE",
            module.get(JvmTypeHelper.class)
                .typeRef(jadescript.lang.Performative.class),
            "jadescript.lang.Performative.UNKNOWN"
        );
    }

}
