package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.utils.LazyInit;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

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

    private final LazyInit<JadescriptTypeNamespace.Empty> namespace =
        lazyInit(() ->
            new JadescriptTypeNamespace.Empty(module, getLocation())
        );


    @Override
    public TypeNamespace namespace() {
        return this.namespace.get();
    }

}
