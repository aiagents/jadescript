package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;

import java.util.List;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class AIDType extends BasicType {


    private final LazyInit<AIDNamespace> namespace =
        lazyInit(() -> new AIDNamespace(
            module,
            getLocation()
        ));


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
    }


    public static class AIDNamespace extends BuiltinOpsNamespace {

        public AIDNamespace(
            SemanticsModule module, SearchLocation location
        ) {
            super(
                module,
                Maybe.nothing(),
                builtinProperties(module, location),
                List.of(),
                location
            );
        }


        private static List<MemberName> builtinProperties(
            SemanticsModule module, SearchLocation location
        ) {

            final BuiltinTypeProvider builtinTypeProvider = module.get(
                BuiltinTypeProvider.class);
            final TextType text = builtinTypeProvider.text();
            return List.of(Property.readonlyProperty(
                "name",
                text,
                location,
                Property.compileWithJVMGetter("name")
            ), Property.readonlyProperty(
                "platform",
                text,
                location,
                Property.compileGetWithCustomMethod("getPlatformID")
            ));
        }

    }


    @Override
    public TypeNamespace namespace() {
        return this.namespace.get();
    }

}
