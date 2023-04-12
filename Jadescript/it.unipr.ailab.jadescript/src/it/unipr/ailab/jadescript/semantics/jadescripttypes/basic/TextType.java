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

public class TextType extends BasicType {

    public TextType(
        SemanticsModule module
    ) {
        super(
            module,
            builtinPrefix + "text",
            "text",
            "jade.content.onto.BasicOntology.STRING",
            module.get(JvmTypeHelper.class).typeRef(String.class),
            "\"\""
        );
    }


    private final LazyInit<TextNamespace> namespace =
        lazyInit(() -> new TextNamespace(module, getLocation()));


    public static class TextNamespace extends BuiltinOpsNamespace {

        public TextNamespace(
            SemanticsModule module,
            SearchLocation location
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
            SemanticsModule module,
            SearchLocation location
        ) {

            final BuiltinTypeProvider builtinTypeProvider =
                module.get(BuiltinTypeProvider.class);
            return List.of(
                Property.readonlyProperty(
                    "length",
                    builtinTypeProvider.integer(),
                    location,
                    Property.compileGetWithCustomMethod("length")
                )
            );
        }

    }


    @Override
    public TypeNamespace namespace() {
        return this.namespace.get();
    }

}
