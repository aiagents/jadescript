package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
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

    default String getParametricIntroductor() {
        return "of";
    }

    default String getParametricListDelimiterOpen() {
        return "(";
    }

    default String getParametricListDelimiterClose() {
        return ")";
    }

    default String getParametricListSeparator() {
        return ",";
    }

    /**
     * @return a stream of all the extensionally-defined direct supertypes of
     * this type.
     */
    Stream<IJadescriptType> declaredSupertypes();

    List<TypeArgument> typeArguments();

    @Override
    JvmTypeReference asJvmTypeReference();

    String getCategoryName();

    TypeCategory category();

    SearchLocation getLocation();

    String getRawID();

    @Override
    String getID();


    default IJadescriptType postResolve() {
        //Override if needed.
        return this;
    }


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
    String getFullJadescriptName();

    String getRawJadescriptName();

    /**
     * It returns the type of the elements if this is a list or set type, the
     * type of the mapped values if this is a map type, and
     * {@link Maybe#nothing()} otherwise.
     */
    Maybe<IJadescriptType> getElementTypeIfCollection();

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


}
