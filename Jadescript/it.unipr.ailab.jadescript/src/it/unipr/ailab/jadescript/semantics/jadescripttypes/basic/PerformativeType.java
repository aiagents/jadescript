package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.utils.LazyInit;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

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


    private final LazyInit<JadescriptTypeNamespace.Empty> namespace =
        lazyInit(() ->
            new JadescriptTypeNamespace.Empty(module, getLocation())
        );


    @Override
    public TypeNamespace namespace() {
        return this.namespace.get();
    }

}
