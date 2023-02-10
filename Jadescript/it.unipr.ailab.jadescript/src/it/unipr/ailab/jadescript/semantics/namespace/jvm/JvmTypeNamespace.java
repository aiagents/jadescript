package it.unipr.ailab.jadescript.semantics.namespace.jvm;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.JvmTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JvmTypeNamespace implements Searcheable {

    public static final String CTOR_INTERNAL_NAME = "###CTOR###";
    private final SemanticsModule module;
    private final JvmDeclaredType jvmDeclaredType;
    private final boolean isGeneric;
    private final LazyValue<Map<String, IJadescriptType>>
        typeParameterAssignments;
    private final Supplier<JvmTypeLocation> locationSupplier;


    private JvmTypeNamespace(
        SemanticsModule module,
        JvmDeclaredType jvmDeclaredType,
        JvmTypeReference... typeParameters
    ) {
        this.module = module;
        this.jvmDeclaredType = jvmDeclaredType;
        this.isGeneric = jvmDeclaredType instanceof JvmGenericType;
        this.typeParameterAssignments = new LazyValue<>(() -> {
            final HashMap<String, IJadescriptType> result = new HashMap<>();
            if (isGeneric) {
                JvmGenericType genericType = (JvmGenericType) jvmDeclaredType;
                for (int i = 0; i < typeParameters.length; i++) {
                    final TypeHelper typeHelper =
                        this.module.get(TypeHelper.class);
                    result.put(
                        genericType.getTypeParameters().get(i).getIdentifier(),
                        typeHelper.jtFromJvmTypeRef(typeParameters[i])
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
        this.module = module;
        this.isGeneric = false;
        this.jvmDeclaredType = null;
        this.typeParameterAssignments = new LazyValue<>(HashMap::new);
        this.locationSupplier = () -> new JvmTypeLocation(reference);
    }


    public static JvmTypeNamespace resolved(
        SemanticsModule module,
        JvmDeclaredType jvmDeclaredType,
        JvmTypeReference... typeParameters
    ) {
        return new JvmTypeNamespace(module, jvmDeclaredType, typeParameters);
    }


    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull JvmTypeNamespace unresolved(
        SemanticsModule module,
        JvmTypeReference reference
    ) {
        return new JvmTypeNamespace(module, reference);
    }


    @Contract("_, null -> null")
    public static JvmTypeNamespace resolve(
        SemanticsModule module,
        JvmTypeReference reference
    ) {
        if (reference == null) {
            return null;
        }
        if (reference.getType() instanceof JvmDeclaredType) {
            if (reference instanceof JvmParameterizedTypeReference) {
                JvmParameterizedTypeReference genericReference =
                    (JvmParameterizedTypeReference) reference;
                List<JvmTypeReference> typeParams = new ArrayList<>(
                    genericReference.getArguments());
                return resolved(
                    module,
                    (JvmDeclaredType) reference.getType(),
                    typeParams.toArray(new JvmTypeReference[0])
                );
            }
            return resolved(
                module,
                (JvmDeclaredType) reference.getType()
            );
        }
        return JvmTypeNamespace.unresolved(module, reference);
    }


    public Optional<? extends JvmOperation> getMetadataMethod() {
        return searchJvmOperation()
            .filter(o -> o.getSimpleName().startsWith("__metadata"))
            .findAny();
    }


    public Maybe<JvmType> declarator() {
        return Maybe.some(jvmDeclaredType);
    }


    public Maybe<? extends JvmTypeNamespace> superJvmNamespace() {
        if (jvmDeclaredType == null) {
            return Maybe.nothing();
        }

        //this recursively resolves eventual type parameters of superclasses
        if (jvmDeclaredType.getExtendedClass()
            instanceof JvmParameterizedTypeReference
            && jvmDeclaredType.getExtendedClass().getType()
            instanceof JvmDeclaredType) {

            List<IJadescriptType> parentArguments = new ArrayList<>();
            final JvmParameterizedTypeReference jvmPTR =
                (JvmParameterizedTypeReference) jvmDeclaredType
                    .getExtendedClass();

            final TypeHelper typeHelper = module.get(TypeHelper.class);

            for (JvmTypeReference argument : jvmPTR.getArguments()) {
                //if the current type reference is a parameter
                // (for example, T) use the mapped reference, otherwise just
                // use the current type reference.
                parentArguments.add(typeParameterAssignments.get().getOrDefault(
                    argument.getIdentifier(),
                    typeHelper.jtFromJvmTypeRef(argument)
                ));
            }

            return Maybe.some(resolve(
                module,
                typeHelper.typeRef(
                    jvmDeclaredType.getExtendedClass().getType(),
                    parentArguments.stream()
                        .map(IJadescriptType::asJvmTypeReference)
                        .collect(Collectors.toList())
                )
            ));

        } else if (jvmDeclaredType.getExtendedClass() != null) {
            return Maybe.some(resolve(
                module,
                jvmDeclaredType.getExtendedClass()
            ));
        } else {
            return Maybe.nothing();
        }
    }


    public Stream<JvmField> searchJvmField() {
        if (jvmDeclaredType == null) {
            return Stream.empty();
        }
        return Streams.stream(jvmDeclaredType.getDeclaredFields());
    }


    public Stream<JvmOperation> searchJvmOperation() {
        if (jvmDeclaredType == null) {
            return Stream.empty();
        }
        return Streams.stream(jvmDeclaredType.getDeclaredOperations());
    }


    public Stream<JvmConstructor> searchJvmConstructor() {
        if (jvmDeclaredType == null) {
            return Stream.empty();
        }
        return Streams.stream(jvmDeclaredType.getDeclaredConstructors());
    }


    public IJadescriptType resolveType(JvmTypeReference ref) {
        if (ref == null) {
            return module.get(TypeHelper.class).ANY;
        } else {
            final Map<String, IJadescriptType> assignments =
                typeParameterAssignments.get();
            if (isGeneric && assignments.containsKey(ref.getIdentifier())) {
                return assignments.get(ref.getIdentifier());
            } else {
                return module.get(TypeHelper.class).jtFromJvmTypeRef(ref);
            }
        }
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return superJvmNamespace();
    }


    public JvmTypeLocation currentLocation() {
        return locationSupplier.get();
    }


    public void debugDump(SourceCodeBuilder scb) {
        scb.line("--> is JvmTypeNamespace (for type " +
            currentLocation().getFullyQualifiedName() + ")");
    }


    /**
     * This is used as method to compute the set of properties of the type.
     * This is done by reading the parameters of the constructor with highest
     * arity,
     * and it works for those generated types where there are at most two
     * constructors,
     * one with no parameters, and the other with N parameters used to
     * initialize
     * the N properties.
     */
    public Map<String, IJadescriptType> getPropertiesFromBiggestCtor() {
        HashMap<String, IJadescriptType> result = new HashMap<>();
        Optional<? extends JvmConstructor> biggest = getBiggestCtor();

        biggest.ifPresent(c -> {
            final EList<JvmFormalParameter> parameters = c.getParameters();
            if(parameters == null){
                return;
            }

            for (JvmFormalParameter parameter : parameters) {
                result.put(
                    parameter.getName(),
                    resolveType(parameter.getParameterType())
                );
            }
        });

        return result;
    }


    public Optional<? extends JvmConstructor> getBiggestCtor() {
        return this.searchJvmConstructor().max(Comparator.comparingInt(c ->
                c.getParameters() == null
                    ? 0
                    : c.getParameters().size()
            )
        );
    }

}
