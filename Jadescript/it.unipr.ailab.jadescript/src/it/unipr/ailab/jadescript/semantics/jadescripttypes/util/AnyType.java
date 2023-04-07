package it.unipr.ailab.jadescript.semantics.jadescripttypes.util;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;

public class AnyType extends ErroneousType {


    public AnyType(SemanticsModule module, String errorMessage) {
        super(
            module,
            TypeHelper.builtinPrefix + "ANY",
            "(error)any",
            module.get(TypeHelper.class).objectTypeRef(),
            errorMessage
        );
    }


    @Override
    public TypeCategory category() {
        return new TypeCategoryAdapter() {
            @Override
            public boolean isAny() {
                return true;
            }
        };
    }


    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
            module,
            Maybe.nothing(),
            List.of(),
            List.of(),
            getLocation()
        );
    }

}
