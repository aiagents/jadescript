package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.GlobalFunctionOrProcedure;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.UsesOntologyElement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
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
public abstract class UsesOntologyTopLevelDeclarationSemantics
    <T extends UsesOntologyElement>
    extends ExtendingTopLevelDeclarationSemantics<T>
    implements OntologyAssociatedDeclarationSemantics<T> {

    public UsesOntologyTopLevelDeclarationSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    public List<IJadescriptType> getUsedOntologyTypes(Maybe<T> input) {
        return getUsedOntologiesTypeRefs(input).stream()
            .map(module.get(TypeSolver.class)::fromJvmTypeReference)
            .map(TypeArgument::ignoreBound)
            .collect(Collectors.toList());
    }


    @Override
    public void validateOnEdit(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }

        super.validateOnEdit(input, acceptor);

    }


    @Override
    public void validateOnSave(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        final MaybeList<JvmTypeReference> ontologies =
            input.__toList(UsesOntologyElement::getOntologies);

        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        for (int i = 0; i < ontologies.size(); i++) {
            Maybe<JvmTypeReference> ontologyTypeRef = ontologies.get(i);

            IJadescriptType ontology = ontologyTypeRef
                .__(typeSolver::fromJvmTypeReference)
                .__(TypeArgument::ignoreBound)
                .orElseGet(() -> builtins.any("No used ontology specified."));

            validationHelper.assertExpectedType(
                builtins.ontology(),
                ontology,
                "InvalidOntologyType",
                input,
                JadescriptPackage.eINSTANCE.getUsesOntologyElement_Ontologies(),
                i,
                acceptor
            );
        }
        super.validateOnSave(input, acceptor);
    }


    /**
     * Infers a {@link UsesOntologyElement}s, i.e., agents or behaviours.<br />
     * Generated code for the ontology: {@code private Onto _ontology0 =
     * (Onto) Onto.getInstance();}<br />
     * Generated code for the codec: {@code public Codec _codec0 = new
     * SLCodec();}
     */
    @Override
    public void populateMainMembers(
        Maybe<T> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        if (input == null) {
            return;
        }

        List<JvmTypeReference> ontologyTypes = getUsedOntologiesTypeRefs(input);

        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        input.safeDo(inputsafe -> {
            for (final JvmTypeReference ontologyType : ontologyTypes) {
                members.add(jvmTypesBuilder.toField(
                    inputsafe,
                    CompilationHelper.extractOntologyVarName(ontologyType),
                    ontologyType,
                    itField -> {
                        itField.setVisibility(JvmVisibility.PUBLIC);

                        compilationHelper.createAndSetInitializer(
                            itField,
                            scb -> {
                                String ontologyName =
                                    ontologyType.getQualifiedName('.');

                                scb.line(
                                    "(" + ontologyName + ") " +
                                        ontologyName + ".getInstance()"
                                );
                            }
                        );
                    }
                ));
            }


            members.add(jvmTypesBuilder.toMethod(
                inputsafe,
                "__registerOntologies",
                builtins.javaVoid().asJvmTypeReference(),
                itMethod -> {
                    itMethod.getParameters().add(
                        jvmTypesBuilder.toParameter(
                            inputsafe,
                            "cm",
                            jvm.typeRef(ContentManager.class)
                        )
                    );
                    compilationHelper.createAndSetBody(itMethod, scb -> {
                        if (!(inputsafe instanceof GlobalFunctionOrProcedure)) {
                            scb.line("super.__registerOntologies(cm);");
                        }

                        for (JvmTypeReference ontologyType : ontologyTypes) {
                            scb.line("cm.registerOntology(" +
                                ontologyType.getQualifiedName('.')
                                + ".getInstance());");
                        }
                    });
                }
            ));


            members.add(jvmTypesBuilder.toField(
                inputsafe,
                CODEC_VAR_NAME,
                jvm.typeRef(jade.content.lang.Codec.class),
                itField -> {
                    itField.setVisibility(JvmVisibility.PUBLIC);
                    compilationHelper.createAndSetInitializer(
                        itField,
                        scb -> scb.line(
                            "new jade.content.lang.leap.LEAPCodec" +
                                "()")
                    );
                }
            ));
        });

        super.populateMainMembers(input, members, itClass);
    }


    @NotNull
    private List<JvmTypeReference> getUsedOntologiesTypeRefs(Maybe<T> input) {
        MaybeList<JvmTypeReference> ontologies =
            input.__toList(UsesOntologyElement::getOntologies);

        List<JvmTypeReference> ontologyTypes = ontologies.stream()
            .filter(Maybe::isPresent)
            .map(Maybe::toNullable)
            .collect(Collectors.toCollection(ArrayList::new));
        if (ontologyTypes.isEmpty()) {
            final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
            ontologyTypes.add(jvm.typeRef(
                jadescript.content.onto.Ontology.class
            ));
        }
        return ontologyTypes;
    }


}
