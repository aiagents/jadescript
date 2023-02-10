package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberNamedCell;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.*;

import java.util.Objects;
import java.util.Optional;
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
        final JvmTypeNamespace jvmTypeNamespace = jvmNamespace();
        if (jvmTypeNamespace == null) {
            return true;
        }

        final Optional<? extends JvmOperation> metadata =
            jvmTypeNamespace.getMetadataMethod();

        return metadata.map(op -> op.getParameters()
                .stream()
                .filter(Objects::nonNull)
                .map(JvmFormalParameter::getParameterType)
                .map(jvmTypeNamespace::resolveType)
                .allMatch(IJadescriptType::isSendable))
            //when empty, assume true; sometimes the metatadata method is
            // generated/visible later in the process
            .orElse(true);
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        final JvmTypeNamespace jvmNamespace = jvmNamespace();

        final Optional<? extends JvmOperation> meta;

        if (jvmNamespace != null) {
            meta = jvmNamespace.getMetadataMethod();
        } else {
            meta = Optional.empty();
        }

        if (meta.isPresent()) {
            final IJadescriptType returnType =
                jvmNamespace.resolveType(meta.get().getReturnType());

            if (returnType instanceof OntologyType) {
                return some(((OntologyType) returnType));
            }
            if (returnType instanceof UnknownJVMType) {
                // Sometimes, after workspace cleaning, TypeHelper is not able
                // to correctly compute the ontology type reference as a
                // UserDefinedOntologyType.
                // Here we force this (we know that the return type of the
                // metadata method is an ontology type).
                // Note that the ontology-subtyping relationships might still
                // be wrong in this phase, but laziness will help us by reading
                // the correct super-ontology field only when the underlying
                // JvmTypeReference is resolved.
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
            return new UserDefinedOntoContentNamespace(
                jvmTypeReference,
                jvmDeclaredType
            );
        } else {
            return getRootCategoryType().namespace();
        }

    }


    @Override
    public boolean isNativeOntoContentType() {
        final JvmTypeNamespace jvmNamespace = jvmNamespace();
        if (jvmNamespace == null) {
            return false;
        }
        return jvmNamespace.searchJvmOperation()
            .filter(Objects::nonNull)
            .map(JvmOperation::getSimpleName)
            .anyMatch("__isNative"::equals);
    }


    public class UserDefinedOntoContentNamespace
        extends JadescriptTypeNamespace {

        private final LazyValue<JvmTypeNamespace> jvmNamespace;
        private final JvmDeclaredType jvmDeclaredType;


        public UserDefinedOntoContentNamespace(
            JvmTypeReference jvmTypeReference,
            JvmDeclaredType jvmDeclaredType
        ) {
            super(UserDefinedOntoContentType.this.module);
            this.jvmDeclaredType = jvmDeclaredType;
            jvmNamespace = new LazyValue<>(
                () -> JvmTypeNamespace.resolve(
                    module,
                    jvmTypeReference
                )
            );
        }


        @Override
        public Stream<? extends MemberCallable> memberCallables() {
            return Stream.empty();
        }


        @Override
        public Stream<? extends MemberNamedCell> memberNamedCells() {
            return namedCellsFromJvm(jvmNamespace.get())
                .memberNamedCells();
        }


        @Override
        public SearchLocation currentLocation() {
            return UserDefinedOntoContentType.this.getLocation();
        }


        @Override
        public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
            return some(jvmDeclaredType.getExtendedClass()).__(
                extendedClass ->
                    module.get(TypeHelper.class)
                        .jtFromJvmTypeRef(extendedClass)
                        .namespace()
            );
        }

    }

}
