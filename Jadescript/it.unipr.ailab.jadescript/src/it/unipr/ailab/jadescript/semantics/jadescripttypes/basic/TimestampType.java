package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class TimestampType extends BasicType {


    public TimestampType(
        SemanticsModule module
    ) {
        super(
            module,
            builtinPrefix + "timestamp",
            "timestamp",
            "jadescript.content.onto.Ontology.TIMESTAMP",
            module.get(JvmTypeHelper.class)
                .typeRef(jadescript.lang.Timestamp.class),
            "jadescript.lang.Timestamp.now()"
        );
    }

}
