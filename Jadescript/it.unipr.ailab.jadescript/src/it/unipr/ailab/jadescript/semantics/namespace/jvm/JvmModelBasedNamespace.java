package it.unipr.ailab.jadescript.semantics.namespace.jvm;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.context.search.JvmTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UnknownLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Symbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.*;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public abstract class JvmModelBasedNamespace
        extends TypeNamespace {

    public static final String CTOR_INTERNAL_NAME = "###CTOR###";

    public JvmModelBasedNamespace(SemanticsModule module) {
        super(module);
    }


    public static JvmTypeNamespace fromTypeReference(
            SemanticsModule module,
            JvmTypeReference reference
    ) {
        if (reference == null) {
            return null;
        }
        if (reference.getType() instanceof JvmDeclaredType) {
            if (reference instanceof JvmParameterizedTypeReference) {
                JvmParameterizedTypeReference genericReference = (JvmParameterizedTypeReference) reference;
                List<JvmTypeReference> typeParams = new ArrayList<>(genericReference.getArguments());
                return new JvmTypeNamespace(
                        module,
                        (JvmDeclaredType) reference.getType(),
                        typeParams.toArray(new JvmTypeReference[0])
                );
            }
            return new JvmTypeNamespace(
                    module,
                    (JvmDeclaredType) reference.getType()
            );
        }
        return JvmTypeNamespace.unresolved(module, reference);
    }



    public abstract Maybe<JvmType> declarator();


    public abstract Maybe<? extends JvmModelBasedNamespace> superJvmNamespace();

    @Override
    public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
        return superJvmNamespace();
    }



    protected abstract Stream<JvmField> streamOfJvmFields();

    protected abstract Stream<JvmOperation> streamOfJvmOperations();

    protected abstract Stream<JvmConstructor> streamOfJvmConstructors();

    protected abstract IJadescriptType resolveType(JvmTypeReference ref);


    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return this.searchCallable(
                n -> n.equals(name),
                returnType,
                parameterNames,
                parameterTypes
        );
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        Stream<JvmConstructor> ctors = streamOfJvmConstructors();
        ctors = safeFilter(ctors, __ -> CTOR_INTERNAL_NAME, name);
        ctors = safeFilter(ctors, ctor ->
                resolveType(module.get(TypeHelper.class)
                        .typeRef(ctor.getDeclaringType())), returnType
        );
        ctors = safeFilter(
                ctors,
                c -> c.getParameters().size(),
                c -> i -> c.getParameters().get(i).getName(),
                parameterNames
        );
        ctors = safeFilter(
                ctors,
                c -> c.getParameters().size(),
                c -> i -> resolveType(c.getParameters().get(i).getParameterType()),
                parameterTypes
        );
        Stream<JvmOperation> methods = streamOfJvmOperations();
        methods = safeFilter(methods, JvmMember::getSimpleName, name);
        methods = safeFilter(methods, m -> module.get(TypeHelper.class).jtFromJvmTypeRef(m.getReturnType()), returnType);
        methods = safeFilter(
                methods,
                c -> c.getParameters().size(),
                c -> i -> c.getParameters().get(i).getName(),
                parameterNames
        );
        methods = safeFilter(
                methods,
                c -> c.getParameters().size(),
                c -> i -> resolveType(c.getParameters().get(i).getParameterType()),
                parameterTypes
        );
        return Streams.concat(methods.map(this::elementFromJvmOperation), ctors.map(this::elementFromJvmConstructor));
    }


    @Override
    public Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<JvmField> streamF = streamOfJvmFields();
        streamF = safeFilter(streamF, JvmMember::getSimpleName, name);
        streamF = safeFilter(streamF, jvmField -> resolveType(jvmField.getType()), readingType);
        streamF = safeFilter(streamF, jvmField -> !jvmField.isFinal(), canWrite);
        return streamF.map(this::elementFromJvmField);
    }

    private JvmFieldSymbol elementFromJvmField(JvmField jvmField) {
        return new JvmFieldSymbol(jvmField, this::resolveType, jvmField.isStatic(), this.declarator());
    }

    private CallableSymbol elementFromJvmConstructor(JvmConstructor jvmConstructor) {
        return new JvmConstructorSymbol(module, jvmConstructor, this::resolveType, this.declarator());
    }

    private CallableSymbol elementFromJvmOperation(JvmOperation jvmOperation) {
        return new JvmOperationSymbol(jvmOperation, this::resolveType, jvmOperation.isStatic(), this.declarator());
    }


    public static ActualParameter symbolFromJvmParameter(
            SemanticsModule module,
            SearchLocation location,
            JvmFormalParameter jvmParameter
    ) {

        return new ActualParameter(
            jvmParameter.getName(),
            module.get(TypeHelper.class)
                .jtFromJvmTypeRef(jvmParameter.getParameterType())
        );
    }


    /**
     * This is used as method to compute the set of properties of the type.
     * This is done by reading the parameters of the constructor with highest arity,
     * and it works for those generated types where there are at most two constructors,
     * one with no parameters, and the other with N parameters used to initialize
     * the N properties.
     */
    public Map<String, IJadescriptType> getPropertiesFromBiggestCtor() {
        HashMap<String, IJadescriptType> result = new HashMap<>();
        Optional<? extends CallableSymbol> biggest = getBiggestCtor();

        biggest.ifPresent(c -> {
            List<String> parameterNames = c.parameterNames();
            List<IJadescriptType> parameterTypes = c.parameterTypes();
            final int assumedSize = Math.min(
                parameterNames.size(),
                parameterTypes.size()
            );
            for (int i = 0; i < assumedSize; i++) {
                result.put(parameterNames.get(i), parameterTypes.get(i));
            }
        });

        return result;
    }

    public Optional<? extends CallableSymbol> getBiggestCtor() {
        return this.searchCallable(CTOR_INTERNAL_NAME, null, null, null)
                .max(Comparator.comparingInt(c -> c.parameterTypes().size()));
    }
}
