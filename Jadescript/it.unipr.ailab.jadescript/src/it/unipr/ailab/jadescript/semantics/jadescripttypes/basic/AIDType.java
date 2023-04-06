package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;

public class AIDType extends BasicType {

    public AIDType(
        SemanticsModule module
    ) {
        super(
            module,
            builtinPrefix + "aid",
            "aid",
            "jade.content.lang.sl.SL0Vocabulary.AID",
            module.get(JvmTypeHelper.class).typeRef(jade.core.AID.class),
            "new jade.core.AID()"
        );

        AID.addBultinProperty(Property.readonlyProperty(
            "name",
            TEXT,
            aidLocation,
            Property.compileWithJVMGetter("name")
        ));
        AID.addBultinProperty(Property.readonlyProperty(
            "platform",
            TEXT,
            aidLocation,
            Property.compileGetWithCustomMethod("getPlatformID")
        ));
    }

}
