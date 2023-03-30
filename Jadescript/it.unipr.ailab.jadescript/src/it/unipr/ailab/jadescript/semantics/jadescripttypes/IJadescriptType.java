package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.stream.Stream;

public interface IJadescriptType extends TypeArgument {


    /**
     * @return a stream of all the extensionally-defined supertypes of this
     * type, exploring the tree breadth-first.
     */
    default Stream<IJadescriptType> allSupertypesBFS() {
        return Stream.concat(
            this.declaredSupertypes(),
            this.declaredSupertypes()
                .flatMap(IJadescriptType::allSupertypesBFS)
        );
    }

    /**
     * @return a stream of all the extensionally-defined supertypes of this
     * type, exploring the tree depth-first.
     */
    default Stream<IJadescriptType> allSupertypesDFS() {
        return this.declaredSupertypes().flatMap(s ->
            Stream.concat(
                Stream.of(s),
                s.allSupertypesDFS()
            )
        );
    }

    /**
     * @return a stream of all the extensionally-defined direct supertypes of
     * this type.
     */
    Stream<IJadescriptType> declaredSupertypes();


    List<TypeArgument> typeArguments();




    @Override
    JvmTypeReference asJvmTypeReference();

    /**
     * @deprecated use the facilities provided by {@link TypeComparator}.
     */
    @Deprecated
    boolean typeEquals(IJadescriptType other);

    /**
     * @deprecated use the facilities provided by {@link TypeComparator}.
     */
    @Deprecated
    boolean isSupertypeOrEqualTo(IJadescriptType other);

    String getCategoryName();

    SearchLocation getLocation();

    @Override
    String getID();

    default IJadescriptType postResolve() {
        //Override if needed.
        return this;
    }

    /**
     * Whether this is a Jadescript Basic type (integer, real, aid...)
     */
    boolean isBasicType();

    /**
     * Whether this is a slottable type (it can be part of a content)
     */
    boolean isSlottable();

    /**
     * Whether values of this type can be sent as content
     */
    boolean isSendable();

    /**
     * Whether Jadescript user code can contain references to this type
     */
    boolean isReferrable();

    /**
     * Whether Jadescript user code can access the properties of the values
     * of this type
     */
    boolean hasProperties();

    /**
     * Whether this is the type of value resulting from a compilation error
     */
    boolean isErroneous();


    Maybe<OntologyType> getDeclaringOntology();

    boolean validateType(
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    );

    @Override
    String getJadescriptName();

    /**
     * It returns the type of the elements if this is a list or set type, the
     * type of the mapped values if this is a map type, and
     * {@link Maybe#nothing()} otherwise.
     */
    Maybe<IJadescriptType> getElementTypeIfCollection();

    boolean isCollection();

    boolean isBehaviour();

    boolean isMessage();

    boolean isMessageContent();

    boolean isAgentEnv();

    String compileConversionType();

    String getSlotSchemaName();

    default String getGetSlotSchemaExpression() {
        return "getSchema(" + getSlotSchemaName() + ")";
    }

    @Override
    IJadescriptType ignoreBound();

    String compileAsJavaCast();

    TypeNamespace namespace();

    JvmTypeNamespace jvmNamespace();

    String getDebugPrint();
    //overridden by VOID type in TypeHelper

    default boolean isJavaVoid() {
        return false;
    }

}
