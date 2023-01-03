package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.AgentDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import jade.wrapper.ContainerController;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 27/04/18.
 */
@Singleton
public class AgentSemantics extends UsesOntologyEntitySemantics<Agent> {

    public AgentSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected void prepareAndEnterContext(Maybe<Agent> input, JvmDeclaredType jvmDeclaredType) {
        module.get(ContextManager.class).enterTopLevelDeclaration((module, outer) ->
                new AgentDeclarationContext(
                        module,
                        outer,
                        getUsedOntologyTypes(input),
                        jvmDeclaredType
                )
        );
        module.get(ContextManager.class).enterProceduralFeatureContainer(
                module.get(TypeHelper.class).jtFromJvmType(jvmDeclaredType),
                input
        );

    }

    @Override
    protected void exitContext(Maybe<Agent> input) {
        module.get(ContextManager.class).exit();//ProceduralFeatureContainerContext
        module.get(ContextManager.class).exit();//AgentDeclarationContext
    }


    @Override
    public void validate(Maybe<Agent> input, ValidationMessageAcceptor acceptor) {
        super.validate(input, acceptor);
    }


    @Override
    public void populateMainMembers(Maybe<Agent> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        super.populateMainMembers(input, members, itClass);
        input.safeDo((inputSafe) -> {

            members.add(module.get(JvmTypesBuilder.class).toField(
                    inputSafe,
                    THE_AGENT,
                    module.get(TypeHelper.class).typeRef(itClass),
                    itField -> module.get(CompilationHelper.class).createAndSetInitializer(itField, scb -> scb.line("this"))
            ));

            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    THE_AGENT,
                    module.get(TypeHelper.class).typeRef(itClass),
                    itMethod -> module.get(CompilationHelper.class).createAndSetBody(
                            itMethod,
                            scb -> {
                                w.returnStmnt(w.expr("this")).writeSonnet(scb);
                            }
                    )
            ));

            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    "setup",
                    module.get(TypeHelper.class).VOID.asJvmTypeReference(),
                    itMethod -> {
                        module.get(JvmTypesBuilder.class).setDocumentation(
                                itMethod,
                                module.get(CompilationHelper.class).getFullyQualifiedName(inputSafe) + " SETUP"
                        );
                        itMethod.setVisibility(JvmVisibility.PROTECTED);
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {

                            w.callStmnt("super.setup").writeSonnet(scb);
                            scb
                                    .line("getContentManager().registerLanguage(" + CODEC_VAR_NAME + ");")
                                    .line();
                            if (stream(input.__(FeatureContainer::getFeatures))
                                    .anyMatch(maybeFeature ->
                                            //f instanceof EventHandler && ((EventHandler) f).getEvent().equals("create")
                                            maybeFeature
                                                    .__(f -> f instanceof OnCreateHandler)
                                                    .extract(nullAsFalse)
                                    )) {
                                w.callStmnt("this.onCreate").writeSonnet(scb);
                            }
                        });
                    }
            ));

            input.__(NamedElement::getName).safeDo(nameSafe -> {

                addCreateMethod(input, members, inputSafe);



            });

        });


    }


    private void addCreateMethod(Maybe<Agent> input, EList<JvmMember> members, Agent inputSafe) {
        members.add(module.get(JvmTypesBuilder.class).toMethod(
                inputSafe,
                "create",
                module.get(TypeHelper.class).typeRef(it.unipr.ailab.jadescript.javaapi.JadescriptAgentController.class),
                itMethod -> {
                    itMethod.setVisibility(JvmVisibility.PUBLIC);
                    itMethod.setStatic(true);

                    final Optional<Maybe<OnCreateHandler>> createHandler = stream(input.__(FeatureContainer::getFeatures))
                            .filter(maybeFeature ->
                                    //f instanceof OnCreateHandler
                                    maybeFeature.__(f -> f instanceof OnCreateHandler).extract(nullAsFalse)
                            ).findAny()
                            .map(maybeFeature -> maybeFeature.__(f -> (OnCreateHandler) f));
                    if(createHandler.isPresent()){
                        Maybe<OnCreateHandler> featureMaybe = createHandler.get();
                        // get feature, get params, compile into create's params
                        if (featureMaybe.isPresent()) {
                            OnCreateHandler createHandlerSafe = featureMaybe.toNullable();
                            itMethod.getParameters().add(module.get(JvmTypesBuilder.class)
                                    .toParameter(
                                            inputSafe,
                                            "_container",
                                            module.get(TypeHelper.class).typeRef(ContainerController.class)
                                    ));
                            itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                    inputSafe,
                                    "_agentName",
                                    module.get(TypeHelper.class).typeRef(String.class)
                            ));

                            itMethod.getExceptions().add(module.get(TypeHelper.class)
                                    .typeRef(jade.wrapper.StaleProxyException.class));

                            List<ExpressionWriter> createArgs = new ArrayList<>();
                            createArgs.add(w.expr("_container"));
                            createArgs.add(w.expr("_agentName"));
                            createArgs.add(w.expr(module.get(CompilationHelper.class).getFullyQualifiedName(inputSafe) + ".class"));

                            for (FormalParameter parameter : createHandlerSafe.getParameters()) {
                                Maybe<FormalParameter> mp = some(parameter);
                                final String paramName = mp.__(FormalParameter::getName).extract(nullAsEmptyString);

                                mp.safeDo(mpsafe -> {
                                    itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                            mpsafe,
                                            paramName,
                                            mp.__(FormalParameter::getType)
                                                    .__(t -> module.get(TypeExpressionSemantics.class).toJadescriptType(
                                                        some(t)))
                                                    .__(IJadescriptType::asJvmTypeReference)
                                                    .orElse(module.get(TypeHelper.class).ANY.asJvmTypeReference())
                                    ));
                                });

                                createArgs.add(w.expr(paramName));

                            }

                            module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                w.returnStmnt(w.callExpr(
                                        "it.unipr.ailab.jadescript.javaapi.JadescriptAgentController.createRaw",
                                        createArgs.toArray(new ExpressionWriter[0])
                                )).writeSonnet(scb);
                            });


                        }
                    }else{
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class)
                                .toParameter(
                                        inputSafe,
                                        "_container",
                                        module.get(TypeHelper.class).typeRef(ContainerController.class)
                                ));
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                inputSafe,
                                "_agentName",
                                module.get(TypeHelper.class).typeRef(String.class)
                        ));

                        itMethod.getExceptions().add(module.get(TypeHelper.class)
                                .typeRef(jade.wrapper.StaleProxyException.class));

                        List<ExpressionWriter> createArgs = new ArrayList<>();
                        createArgs.add(w.expr("_container"));
                        createArgs.add(w.expr("_agentName"));
                        createArgs.add(w.expr(module.get(CompilationHelper.class).getFullyQualifiedName(inputSafe) + ".class"));


                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            w.returnStmnt(w.callExpr(
                                    "it.unipr.ailab.jadescript.javaapi.JadescriptAgentController.createRaw",
                                    createArgs.toArray(new ExpressionWriter[0])
                            )).writeSonnet(scb);
                        });
                    }
                }
        ));
    }

    @Override
    public Optional<IJadescriptType> defaultSuperType(Maybe<Agent> input) {
        return Optional.ofNullable(module.get(TypeHelper.class).AGENT);
    }

    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(Maybe<Agent> input) {
        return Collections.singletonList(module.get(TypeHelper.class).AGENT);
    }
}
