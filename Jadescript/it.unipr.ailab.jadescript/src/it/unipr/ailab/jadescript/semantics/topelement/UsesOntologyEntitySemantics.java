package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.GlobalFunctionOrProcedure;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.UsesOntologyElement;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import jade.content.ContentManager;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 27/04/18.
 */
@Singleton
public abstract class UsesOntologyEntitySemantics<T extends UsesOntologyElement> extends ExtendingEntitySemantics<T> {

    public UsesOntologyEntitySemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    public List<IJadescriptType> getUsedOntologyTypes(Maybe<T> input) {
        return getUsedOntologiesTypeRefs(input).stream()
                .map(module.get(TypeHelper.class)::jtFromJvmTypeRef)
                .collect(Collectors.toList());
    }

    @Override
    public void validate(Maybe<T> input, ValidationMessageAcceptor acceptor) {
        super.validate(input, acceptor);
        if (input == null) return;
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        final List<Maybe<JvmTypeReference>> ontologies =
                Maybe.toListOfMaybes(input.__(UsesOntologyElement::getOntologies));
        for (int i = 0; i < ontologies.size(); i++) {
            Maybe<JvmTypeReference> ontologyTypeRef = ontologies.get(i);
            IJadescriptType ontology = ontologyTypeRef.__(o -> module.get(TypeHelper.class).jtFromJvmTypeRef(o))
                    .orElseGet(() -> module.get(TypeHelper.class).ANY);
            module.get(ValidationHelper.class).assertExpectedType(jade.content.onto.Ontology.class, ontology,
                    "InvalidOntologyType",
                    input,
                    JadescriptPackage.eINSTANCE.getUsesOntologyElement_Ontologies(),
                    i,
                    interceptAcceptor
            );
        }

    }

    /**
     * Infers a {@link UsesOntologyElement}s, i.e., agents or behaviours.<br />
     * Generated code for the ontology: {@code private Onto _ontology0 = (Onto) Onto.getInstance();}<br />
     * Generated code for the codec: {@code public Codec _codec0 = new SLCodec();}
     */
    @Override
    public void populateMainMembers(Maybe<T> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        super.populateMainMembers(input, members, itClass);
        if (input == null) return;
        List<JvmTypeReference> ontologyTypes = getUsedOntologiesTypeRefs(input);

        input.safeDo(inputsafe -> {
            for (final JvmTypeReference ontologyType : ontologyTypes) {
                String ontologyName = ontologyType.getQualifiedName('.');
                members.add(module.get(JvmTypesBuilder.class).toField(
                        inputsafe,
                        CompilationHelper.extractOntologyVarName(ontologyType),
                        ontologyType,
                        itField -> {
                            itField.setVisibility(JvmVisibility.PUBLIC);
                            module.get(CompilationHelper.class).createAndSetInitializer(
                                    itField,
                                    scb -> scb.line("(" + ontologyName + ") " + ontologyName + ".")
                                            .add("getInstance()")

                            );
                        }
                ));
            }


            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputsafe,
                    "__registerOntologies",
                    module.get(TypeHelper.class).VOID.asJvmTypeReference(),
                    itMethod -> {
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                inputsafe,
                                "cm",
                                module.get(TypeHelper.class).typeRef(ContentManager.class)
                        ));
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            itMethod.setVisibility(JvmVisibility.PROTECTED);
                            if(!(inputsafe instanceof GlobalFunctionOrProcedure)) {
                                scb.line("super.__registerOntologies(cm);");
                            }

                            for (JvmTypeReference ontologyType : ontologyTypes) {
                                scb.line("cm.registerOntology(" +
                                        CompilationHelper.extractOntologyVarName(ontologyType)
                                        + ");");
                            }
                        });
                    }
            ));

            members.add(module.get(JvmTypesBuilder.class).toField(
                    inputsafe,
                    CODEC_VAR_NAME,
                    module.get(TypeHelper.class).typeRef(jade.content.lang.Codec.class),
                    itField -> {
                        itField.setVisibility(JvmVisibility.PUBLIC);
                        module.get(CompilationHelper.class).createAndSetInitializer(
                                itField,
                                scb -> scb.line("new jade.content.lang.leap.LEAPCodec()")
                        );
                    }
            ));
        });

    }

    @NotNull
    private List<JvmTypeReference> getUsedOntologiesTypeRefs(Maybe<T> input) {
        List<Maybe<JvmTypeReference>> ontologies = Maybe.toListOfMaybes(input.__(UsesOntologyElement::getOntologies));

        List<JvmTypeReference> ontologyTypes = ontologies.stream()
                .filter(Maybe::isPresent)
                .map(Maybe::toNullable)
                .collect(Collectors.toCollection(ArrayList::new));
        if (ontologyTypes.isEmpty()) {
            ontologyTypes.add(module.get(TypeHelper.class).typeRef(jadescript.content.onto.Ontology.class));
        }
        return ontologyTypes;
    }


}
