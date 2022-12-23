package it.unipr.ailab.jadescript.semantics.namespace;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
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
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.*;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    public interface JvmSymbol extends Symbol {
        boolean isStatic();

        Maybe<JvmType> declaringType();

        @Override
        default SearchLocation sourceLocation() {
            return declaringType().__(it -> new JvmTypeLocation(it))
                    .__(x -> (SearchLocation) x)
                    .orElse(UnknownLocation.getInstance());
        }
    }

    public static class JvmOperationSymbol implements CallableSymbol, JvmSymbol {
        private final JvmOperation jvmOperation;
        private final Function<JvmTypeReference, IJadescriptType> typeResolver;
        private final boolean isStatic;
        private final Maybe<JvmType> declaringType;

        public JvmOperationSymbol(
                JvmOperation jvmOperation,
                Function<JvmTypeReference, IJadescriptType> typeResolver,
                boolean isStatic,
                Maybe<JvmType> declaringType
        ) {
            this.jvmOperation = jvmOperation;
            this.typeResolver = typeResolver;
            this.isStatic = isStatic;
            this.declaringType = declaringType;
        }

        @Override
        public String name() {
            return jvmOperation.getSimpleName();
        }

        @Override
        public IJadescriptType returnType() {
            return typeResolver.apply(jvmOperation.getReturnType());
        }

        @Override
        public Map<String, IJadescriptType> parameterTypesByName() {
            return jvmOperation.getParameters().stream()
                    .collect(Collectors.toMap(
                            JvmFormalParameter::getName,
                            p -> typeResolver.apply(p.getParameterType())
                    ));
        }

        @Override
        public List<String> parameterNames() {
            return jvmOperation.getParameters().stream()
                    .map(JvmFormalParameter::getName)
                    .collect(Collectors.toList());
        }

        @Override
        public String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs) {
            List<String> argNames = new ArrayList<>();
            List<String> args = new ArrayList<>();
            compiledRexprs.forEach((name, arg) -> {
                argNames.add(name);
                args.add(arg);
            });
            return dereferencePrefix + jvmOperation.getSimpleName() + "(" +
                    String.join(
                            ", ",
                            MethodInvocationSemantics.sortToMatchParamNames(args, argNames, parameterNames())
                    ) + ")";
        }

        @Override
        public boolean isPure() {
            //RETURN TRUE FOR PATTERN-MATCHEABLE FACTORY METHODS!
        }

        @Override
        public List<IJadescriptType> parameterTypes() {
            return jvmOperation.getParameters().stream()
                    .map(p -> typeResolver.apply(p.getParameterType()))
                    .collect(Collectors.toList());
        }

        @Override
        public String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs) {
            return dereferencePrefix + jvmOperation.getSimpleName() + "(" +
                    String.join(", ", compiledRexprs) +
                    ")";
        }

        @Override
        public boolean isStatic() {
            return this.isStatic;
        }

        @Override
        public Maybe<JvmType> declaringType() {
            return this.declaringType;
        }

        @Override
        public void debugDumpCallableSymbol(SourceCodeBuilder scb) {
            CallableSymbol.super.debugDumpCallableSymbol(scb);
            scb.indent().line("--> (" + name() + " is also JvmOperationSymbol; isStatic=" + isStatic() + ")").dedent();
        }


    }

    public static class JvmConstructorSymbol implements CallableSymbol, JvmSymbol {
        private final SemanticsModule module;
        private final JvmConstructor jvmConstructor;
        private final Function<JvmTypeReference, IJadescriptType> typeResolver;
        private final Maybe<JvmType> declaringType;

        public JvmConstructorSymbol(
                SemanticsModule module,
                JvmConstructor jvmConstructor,
                Function<JvmTypeReference, IJadescriptType> typeResolver,
                Maybe<JvmType> declaringType
        ) {
            this.module = module;
            this.jvmConstructor = jvmConstructor;
            this.typeResolver = typeResolver;
            this.declaringType = declaringType;
        }

        @Override
        public String name() {
            return CTOR_INTERNAL_NAME;
        }

        @Override
        public IJadescriptType returnType() {
            return typeResolver.apply(module.get(TypeHelper.class).typeRef(jvmConstructor.getDeclaringType()));
        }

        @Override
        public Map<String, IJadescriptType> parameterTypesByName() {
            return jvmConstructor.getParameters().stream()
                    .collect(Collectors.toMap(
                            JvmFormalParameter::getName,
                            p -> typeResolver.apply(p.getParameterType())
                    ));
        }

        @Override
        public List<String> parameterNames() {
            return jvmConstructor.getParameters().stream()
                    .map(JvmFormalParameter::getName)
                    .collect(Collectors.toList());
        }

        @Override
        public String compileInvokeByName(String dereferencePrefix_ignored, Map<String, String> compiledRexprs) {
            List<String> argNames = new ArrayList<>();
            List<String> args = new ArrayList<>();
            compiledRexprs.forEach((name, arg) -> {
                argNames.add(name);
                args.add(arg);
            });
            return "new " + jvmConstructor.getQualifiedName('.') + "(" +
                    String.join(
                            ", ",
                            MethodInvocationSemantics.sortToMatchParamNames(args, argNames, parameterNames())
                    ) + ")";
        }

        @Override
        public boolean isPure() {
            // RETURN TRUE?
        }

        @Override
        public List<IJadescriptType> parameterTypes() {
            return jvmConstructor.getParameters().stream()
                    .map(p -> typeResolver.apply(p.getParameterType()))
                    .collect(Collectors.toList());
        }

        @Override
        public String compileInvokeByArity(String dereferencePrefix_ignored, List<String> compiledRexprs) {
            return "new " + jvmConstructor.getQualifiedName('.') + "(" +
                    String.join(", ", compiledRexprs) +
                    ")";
        }

        @Override
        public void debugDumpCallableSymbol(SourceCodeBuilder scb) {
            CallableSymbol.super.debugDumpCallableSymbol(scb);
            scb.indent().line("--> (" + name() + " is also JvmConstructorSymbol)").dedent();
        }

        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public Maybe<JvmType> declaringType() {
            return declaringType;
        }
    }


    public static class JvmFieldSymbol implements NamedSymbol, JvmSymbol {
        private final JvmField jvmField;
        private final Function<JvmTypeReference, IJadescriptType> typeResolver;
        private final boolean isStatic;
        private final Maybe<JvmType> declaringType;

        public JvmFieldSymbol(
                JvmField jvmField,
                Function<JvmTypeReference, IJadescriptType> typeResolver,
                boolean isStatic,
                Maybe<JvmType> declaringType
        ) {
            this.jvmField = jvmField;
            this.typeResolver = typeResolver;
            this.isStatic = isStatic;
            this.declaringType = declaringType;
        }

        public boolean isStatic() {
            return isStatic;
        }

        @Override
        public Maybe<JvmType> declaringType() {
            return declaringType;
        }

        @Override
        public String name() {
            return jvmField.getSimpleName();
        }

        @Override
        public String compileRead(String dereferencePrefix) {
            return dereferencePrefix + jvmField.getSimpleName();
        }

        @Override
        public IJadescriptType readingType() {
            return typeResolver.apply(jvmField.getType());
        }

        @Override
        public boolean canWrite() {
            return !jvmField.isFinal();
        }

        @Override
        public String compileWrite(String dereferencePrefix, String rexpr) {
            return dereferencePrefix + jvmField.getSimpleName() + " = " + rexpr;
        }

        @Override
        public void debugDumpNamedSymbol(SourceCodeBuilder scb) {
            NamedSymbol.super.debugDumpNamedSymbol(scb);
            scb.indent().line("--> (" + name() + " is also JvmFieldSymbol; isStatic=" + isStatic() + ")").dedent();
        }
    }


    public static ActualParameter symbolFromJvmParameter(
            SemanticsModule module,
            SearchLocation location,
            JvmFormalParameter jvmParameter
    ) {

        return new ActualParameter(jvmParameter.getName(), module.get(TypeHelper.class).jtFromJvmTypeRef(
                jvmParameter.getParameterType()
        ));
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
            for (int i = 0; i < Math.min(parameterNames.size(), parameterTypes.size()); i++) {
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
