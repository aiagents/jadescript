package it.unipr.ailab.jadescript.semantics.namespace;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.JvmTypeLocation;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JvmTypeNamespace extends JvmModelBasedNamespace {
    private final JvmDeclaredType jvmDeclaredType;
    private final boolean isGeneric;
    private final LazyValue<Map<String, IJadescriptType>> typeParameterAssignments;
    private final Supplier<JvmTypeLocation> locationSupplier;

    public JvmTypeNamespace(
            SemanticsModule module,
            JvmDeclaredType jvmDeclaredType,
            JvmTypeReference... typeParameters
    ) {
        super(module);
        this.jvmDeclaredType = jvmDeclaredType;
        this.isGeneric = jvmDeclaredType instanceof JvmGenericType;
        typeParameterAssignments = new LazyValue<>(() -> {
            final HashMap<String, IJadescriptType> result = new HashMap<>();
            if (isGeneric) {
                JvmGenericType genericType = (JvmGenericType) jvmDeclaredType;
                for (int i = 0; i < typeParameters.length; i++) {
                    result.put(
                            genericType.getTypeParameters().get(i).getIdentifier(),
                            super.module.get(TypeHelper.class).jtFromJvmTypeRef(typeParameters[i])
                    );
                }
            }
            return result;
        });
        this.locationSupplier = () -> new JvmTypeLocation(jvmDeclaredType);

    }

    private JvmTypeNamespace(
            SemanticsModule module,
            JvmTypeReference reference
    ) {
        super(module);
        isGeneric = false;
        jvmDeclaredType = null;
        typeParameterAssignments = new LazyValue<>(HashMap::new);
        locationSupplier = () -> new JvmTypeLocation(reference);
    }

    public static JvmTypeNamespace unresolved(SemanticsModule module, JvmTypeReference reference) {
        return new JvmTypeNamespace(module, reference);
    }


    @Override
    public Maybe<JvmType> declarator() {
        return Maybe.some(jvmDeclaredType);
    }

    @Override
    public Maybe<? extends JvmModelBasedNamespace> superJvmNamespace() {
        if (jvmDeclaredType == null) {
            return Maybe.nothing();
        }

        //this recursively resolves eventual type parameters of superclasses
        if (jvmDeclaredType.getExtendedClass() instanceof JvmParameterizedTypeReference
                && jvmDeclaredType.getExtendedClass().getType() instanceof JvmDeclaredType) {
            List<IJadescriptType> parentArguments = new ArrayList<>();
            for (JvmTypeReference argument : ((JvmParameterizedTypeReference) jvmDeclaredType.getExtendedClass()).getArguments()) {
                //if the current type reference is a parameter (for example, 'T')
                //  use the mapped reference
                //  otherwise just use the current type reference
                parentArguments.add(typeParameterAssignments.get().getOrDefault(
                        argument.getIdentifier(),
                        module.get(TypeHelper.class).jtFromJvmTypeRef(argument)
                ));
            }
            return Maybe.some(fromTypeReference(
                    module,
                    module.get(TypeHelper.class).typeRef(
                            jvmDeclaredType.getExtendedClass().getType(),
                            parentArguments.stream()
                                    .map(IJadescriptType::asJvmTypeReference)
                                    .collect(Collectors.toList())
                    )
            ));

        } else if (jvmDeclaredType.getExtendedClass() != null) {
            return Maybe.some(new JvmTypeNamespace(
                    module,
                    (JvmDeclaredType) jvmDeclaredType.getExtendedClass().getType()
            ));
        } else {
            return Maybe.nothing();
        }
    }

    @Override
    protected Stream<JvmField> streamOfJvmFields() {
        if (jvmDeclaredType == null) {
            return Stream.empty();
        }
        return Streams.stream(jvmDeclaredType.getDeclaredFields());
    }

    @Override
    protected Stream<JvmOperation> streamOfJvmOperations() {
        if (jvmDeclaredType == null) {
            return Stream.empty();
        }
        return Streams.stream(jvmDeclaredType.getDeclaredOperations());
    }

    @Override
    protected Stream<JvmConstructor> streamOfJvmConstructors() {
        if (jvmDeclaredType == null) {
            return Stream.empty();
        }
        return Streams.stream(jvmDeclaredType.getDeclaredConstructors());
    }

    @Override
    protected IJadescriptType resolveType(JvmTypeReference ref) {
        if (ref == null) {
            return module.get(TypeHelper.class).ANY;
        } else {
            if (isGeneric && typeParameterAssignments.get().containsKey(ref.getIdentifier())) {
                return typeParameterAssignments.get().get(ref.getIdentifier());
            } else {
                return module.get(TypeHelper.class).jtFromJvmTypeRef(ref);
            }
        }
    }

    @Override
    public JvmTypeLocation currentLocation() {
        return locationSupplier.get();
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.line("--> is JvmTypeNamespace (for type " + currentLocation().getFullyQualifiedName() + ")");
        super.debugDump(scb);
    }
}
