package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import org.eclipse.xtext.common.types.JvmTypeReference;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class DurationType extends BasicType{

    public DurationType(
        SemanticsModule module
    ) {
        super(module,
builtinPrefix + "duration",
            "duration",
            "jadescript.content.onto.Ontology.DURATION",
            module.get(JvmTypeHelper.class)
                .typeRef(jadescript.lang.Duration.class),
            "new jadescript.lang.Duration()"
            );
    }

}
