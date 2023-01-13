package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmModelBasedNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class UserDefinedOntoContentType
        extends UserDefinedType<BaseOntoContentType>
        implements OntoContentType {
    public UserDefinedOntoContentType(
            SemanticsModule module,
            JvmTypeReference jvmType,
            BaseOntoContentType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
    }

    @Override
    public String compileNewEmptyInstance() {
        return "new " + compileToJavaTypeReference() + "()";
    }

    @Override
    public boolean isSendable() {
        final Optional<? extends CallableSymbol> metadata = JvmTypeNamespace.fromTypeReference(module, asJvmTypeReference()).searchCallable(
                name -> name.startsWith("__metadata"),
                null,
                null,
                null
        ).findAny();
        if (metadata.isEmpty()) {
            return true; //assuming true; sometimes the metatadata method is generated/visible later in the process
        } else {
            return metadata.get().parameterTypes().stream().allMatch(IJadescriptType::isSendable);
        }
    }

    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        final JvmModelBasedNamespace jvmNamespace = jvmNamespace();
        final Optional<? extends CallableSymbol> meta;
        if (jvmNamespace == null) {
            meta = Optional.empty();
        } else {
            meta = jvmNamespace.searchCallable(
                    name -> name.startsWith("__metadata"),
                    null,
                    null,
                    null
            ).findAny();
        }
        if (meta.isPresent()) {
            final IJadescriptType returnType = meta.get().returnType();
            if (returnType instanceof OntologyType) {
                return some(((OntologyType) returnType));
            }
            if (returnType instanceof UnknownJVMType) {
                //Sometimes, after workspace cleaning, TypeHelper is not able to correctly compute the ontology type
                // reference as a UserDefinedOntologyType. Here we force this (we know that the return type of the
                // metadata method is an ontology type).
                // Note that the ontology-subtyping relationships might still be wrong in this phase, but laziness will
                // help us by reading the correct super-ontology field only when the underlying JvmTypeReference is
                // resolved.
                return some(new UserDefinedOntologyType(
                        module,
                        returnType.asJvmTypeReference(),
                        module.get(TypeHelper.class).ONTOLOGY
                ));
            }
        }
        return nothing();
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public TypeNamespace namespace() {
        final JvmTypeReference jvmTypeReference = asJvmTypeReference();
        final JvmType type = jvmTypeReference.getType();
        if (type instanceof JvmDeclaredType) {
            JvmDeclaredType jvmDeclaredType = (JvmDeclaredType) type;
            return new JadescriptTypeNamespace(module) {
                @Override
                public SearchLocation currentLocation() {
                    return UserDefinedOntoContentType.this.getLocation();
                }

                private final LazyValue<JvmTypeNamespace> jvmNamespace = new LazyValue<>(() ->
                        JvmTypeNamespace.fromTypeReference(module, jvmTypeReference)
                );

                @Override
                public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
                    return some(jvmDeclaredType.getExtendedClass()).__(extendedClass ->
                            module.get(TypeHelper.class)
                                    .jtFromJvmTypeRef(extendedClass)
                                    .namespace()
                    );
                }

                @Override
                public Stream<? extends CallableSymbol> searchCallable(
                        String name,
                        Predicate<IJadescriptType> returnType,
                        BiPredicate<Integer, Function<Integer, String>> parameterNames,
                        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
                ) {
                    return computeUserDefinedSymbols(jvmNamespace.get()).searchCallable(
                            n -> n.startsWith("get") && n.equals(name),
                            returnType,
                            (s, n) -> s == 0 && (parameterNames == null || parameterNames.test(s, n)),
                            (s, t) -> s == 0 && (parameterTypes == null || parameterTypes.test(s, t))
                    ).filter(ce -> //only those who are effectively getters
                            computeUserDefinedSymbols(jvmNamespace.get()).searchName(
                                    n -> ce.name().substring(3)/*removing "get"*/.equals(n),
                                    returnType,
                                    null
                            ).findAny().isPresent()
                    );
                }

                @Override
                public Stream<? extends CallableSymbol> searchCallable(
                        Predicate<String> name,
                        Predicate<IJadescriptType> returnType,
                        BiPredicate<Integer, Function<Integer, String>> parameterNames,
                        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
                ) {
                    return computeUserDefinedSymbols(jvmNamespace.get()).searchCallable(
                            n -> n.startsWith("get") && (name == null || name.test(n)),
                            returnType,
                            (s, n) -> s == 0 && (parameterNames == null || parameterNames.test(s, n)),
                            (s, t) -> s == 0 && (parameterTypes == null || parameterTypes.test(s, t))
                    ).filter(ce -> //only those who are effectively getters
                            computeUserDefinedSymbols(jvmNamespace.get()).searchName(
                                    n -> ce.name().substring(3)/*removing "get"*/.equals(n),
                                    returnType,
                                    null
                            ).findAny().isPresent()
                    );
                }

                @Override
                public Stream<? extends NamedSymbol> searchName(Predicate<String> name, Predicate<IJadescriptType> readingType, Predicate<Boolean> canWrite) {
                    return computeUserDefinedSymbols(jvmNamespace.get()).searchName(name, readingType, canWrite);
                }
            };

        } else {
            return getRootCategoryType().namespace();
        }

    }

    @Override
    public boolean isNativeOntoContentType() {
        final JvmModelBasedNamespace jvmNamespace = jvmNamespace();
        if (jvmNamespace == null) {
            return false;
        }
        return jvmNamespace
                .searchCallable("__isNative", null, null, null)
                .findAny().isPresent();
    }
}
