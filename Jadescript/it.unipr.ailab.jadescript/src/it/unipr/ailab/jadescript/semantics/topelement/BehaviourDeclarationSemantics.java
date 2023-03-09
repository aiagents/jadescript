package it.unipr.ailab.jadescript.semantics.topelement;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelBehaviourDeclarationContext;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.BehaviourDeclaration;
import it.unipr.ailab.maybe.Maybe;
import jadescript.core.behaviours.CyclicBehaviour;
import jadescript.core.behaviours.OneShotBehaviour;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.*;

public class BehaviourDeclarationSemantics
    extends ForAgentDeclarationSemantics<BehaviourDeclaration> {

    public BehaviourDeclarationSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    private static boolean hasEventClassSystem(Feature input) {
        return input instanceof OnExecuteHandler
            || input instanceof OnMessageHandler
            || input instanceof OnPerceptHandler;
    }


    @Override
    protected void prepareAndEnterContext(
        Maybe<BehaviourDeclaration> input,
        JvmDeclaredType jvmDeclaredType
    ) {
        final ContextManager contextManager = module.get(ContextManager.class);


        if (input.__(BehaviourDeclaration::isMemberBehaviour)
            .extract(nullAsFalse)) {
            contextManager.enterEmulatedFile();
        }

        contextManager.enterTopLevelDeclaration((module, outer) ->
            new TopLevelBehaviourDeclarationContext(
                module,
                outer,
                getUsedOntologyTypes(input),
                getAssociatedAgentType(input, null),
                jvmDeclaredType
            )
        );


        contextManager.enterProceduralFeatureContainer(
            module.get(TypeHelper.class)
                .jtFromJvmTypePermissive(jvmDeclaredType),
            input
        );
    }


    @Override
    protected void exitContext(Maybe<BehaviourDeclaration> input) {
        // From ProceduralFeatureContainerContext:
        module.get(ContextManager.class).exit();

        // From TopLevelBehaviourDeclarationContext:
        module.get(ContextManager.class).exit();

        if (input.__(BehaviourDeclaration::isMemberBehaviour)
            .extract(nullAsFalse)) {
            //From EmulatedFileContext:
            module.get(ContextManager.class).exit();
        }

    }


    @Override
    public void validate(
        Maybe<BehaviourDeclaration> input,
        ValidationMessageAcceptor acceptor
    ) {

        //Keep super.validate at the end
        super.validate(input, acceptor);
    }


    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(
        Maybe<BehaviourDeclaration> input
    ) {
        final Maybe<JvmTypeReference> typeArg =
            input.__(BehaviourDeclaration::getForAgent)
                .extract(Maybe::flatten);

        List<IJadescriptType> typeArgs = new ArrayList<>();

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (typeArg.isPresent()) {
            typeArgs.add(typeHelper.jtFromJvmTypeRef(typeArg.toNullable()));
        } else {
            typeArgs.add(typeHelper.AGENT);
        }

        if (Maybe.nullAsFalse(input
            .__(BehaviourDeclaration::getType)
            .__(String::equals, "cyclic"))) {

            return Collections.singletonList(typeHelper.jtFromClass(
                jadescript.core.behaviours.CyclicBehaviour.class,
                typeArgs
            ));

        } else {//it's one shot

            return Collections.singletonList(typeHelper.jtFromClass(
                jadescript.core.behaviours.OneShotBehaviour.class,
                typeArgs
            ));

        }
    }


    @Override
    public Optional<IJadescriptType> defaultSuperType(
        Maybe<BehaviourDeclaration> input
    ) {
        final Maybe<JvmTypeReference> typeArg =
            input.__(BehaviourDeclaration::getForAgent)
                .extract(Maybe::flatten);

        List<IJadescriptType> typeArgs = new ArrayList<>(1);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (typeArg.isPresent()) {
            typeArgs.add(typeHelper.jtFromJvmTypeRef(typeArg.toNullable()));
        }
        if (Maybe.nullAsFalse(input
            .__(BehaviourDeclaration::getType)
            .__(String::equals, "cyclic"))) {

            final IJadescriptType value = typeHelper.jtFromClass(
                CyclicBehaviour.class,
                typeArgs
            );
            return Optional.of(value);

        } else { //it's one shot

            final IJadescriptType value = typeHelper.jtFromClass(
                OneShotBehaviour.class,
                typeArgs
            );

            return Optional.of(value);

        }
    }


    @Override
    public void populateMainMembers(
        Maybe<BehaviourDeclaration> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        if (input.isNothing()) {
            return;
        }

        final BehaviourDeclaration inputSafe = input.toNullable();

        final Optional<OnCreateHandler> onCreateHandler =
            stream(input.__(BehaviourDeclaration::getFeatures))
                .filter(j -> j.__(f -> f instanceof OnCreateHandler)
                    .extract(nullAsFalse))
                .map(Maybe::toOpt)
                .findFirst()
                .flatMap(x -> x)
                .map(f -> (OnCreateHandler) f);

        List<String> paramDefaultValues = new ArrayList<>();

        final ContextManager contextManager =
            module.get(ContextManager.class);

        final SavedContext savedContext = contextManager.save();


        if (onCreateHandler.isPresent()) {
            final List<Maybe<FormalParameter>> formalParameters =
                toListOfMaybes(onCreateHandler.get().getParameters());

            final TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);

            formalParameters.stream()
                .map(m -> m.__(FormalParameter::getType))
                .map(tes::toJadescriptType)
                .map(t -> {
                    if (t instanceof EmptyCreatable) {
                        return ((EmptyCreatable) t).compileNewEmptyInstance();
                    } else {
                        return "null";
                    }
                }).forEach(paramDefaultValues::add);

        } else {
            // if there are no "on create" event handlers, create a default ctor
            createDefaultOnCreateHandler(input, members, savedContext);
        }


        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final IQualifiedNameProvider qnProvider =
            module.get(IQualifiedNameProvider.class);


        members.add(jvmTB.toMethod(inputSafe, "__createEmpty",
            typeHelper.typeRef(inputSafe),
            itMethod -> {

                contextManager.restore(savedContext);

                final IJadescriptType contextAgent =
                    getAssociatedAgentType(input, null);

                itMethod.setStatic(true);


                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    AGENT_ENV,
                    typeHelper.AGENTENV
                        .apply(List.of(
                            typeHelper.covariant(contextAgent),
                            typeHelper.jtFromClass(
                                AgentEnvType.toSEModeClass(
                                    AgentEnvType.SEMode.WITH_SE
                                )
                            )
                        )).asJvmTypeReference()
                ));

                compilationHelper.createAndSetBody(
                    itMethod,
                    scb -> {
                        scb.line("return new " +
                            input.__(qnProvider::getFullyQualifiedName)
                                .__(qn -> qn.toString(".")) +
                            "(" +
                            AGENT_ENV +
                            (paramDefaultValues.isEmpty() ? "" : ", ") +
                            String.join(", ", paramDefaultValues) +
                            ");"
                        );
                    }
                );
            }
        ));


        members.add(jvmTB.toField(
                inputSafe,
                IGNORE_MSG_HANDLERS_VAR_NAME,
                typeHelper.typeRef(Boolean.class),
                itField -> compilationHelper.createAndSetInitializer(
                    itField,
                    scb -> scb.add("false")
                )
            )
        );

        // It creates the doAction() method with a set of mechanisms that
        // ensures that the behaviour is safely and efficiently put to
        // "waiting" state when there is no action to execute (i.e., there
        // is no "on execute" and no message in the inbox that matched  the
        // "on message" handler conditions...). Moreover, if this behaviour
        // did not extract any message from the inbox, it updates the agent's
        // internal state to declare all messages in the inbox as "ignored"
        // by this behaviour. This is useful for stale message detection
        // (stale message = message in the inbox ignored at least once by
        // all the currently active behaviours).
        members.add(jvmTB.toMethod(
            inputSafe,
            "doAction",
            typeHelper.typeRef(void.class),
            itMethod -> {
                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    "_tickCount",
                    typeHelper.typeRef(Integer.TYPE)
                ));
                itMethod.setVisibility(JvmVisibility.PUBLIC);

                compilationHelper.createAndSetBody(itMethod, scb -> {
                    w.assign(
                        "this." + IGNORE_MSG_HANDLERS_VAR_NAME,
                        w.expr("false")
                    ).writeSonnet(scb);

                    scb.line("super.doAction(_tickCount);");

                    toListOfMaybes(input.__(FeatureContainer::getFeatures))
                        .stream()
                        .filter(Maybe::isPresent)
                        .map(Maybe::toNullable)
                        .filter(
                            BehaviourDeclarationSemantics::hasEventClassSystem
                        ).map(this::synthesizeEventFieldName)
                        .forEach(eventName -> scb.line(eventName + ".run();"));

                    w.ifStmnt(
                        w.expr("!this." + IGNORE_MSG_HANDLERS_VAR_NAME),
                        w.block().addStatement(
                            w.callStmnt("this.__noMessageHandled")
                        )
                    ).writeSonnet(scb);

                    scb.add("if ( true ");
                    toListOfMaybes(input.__(FeatureContainer::getFeatures))
                        .stream()
                        .filter(Maybe::isPresent)
                        .map(Maybe::toNullable)
                        .filter(
                            BehaviourDeclarationSemantics::hasEventClassSystem
                        )
                        .map(this::synthesizeEventFieldName)
                        .forEach(eventName -> {
                            scb.add(" && !" + eventName +
                                "." + MESSAGE_RECEIVED_BOOL_VAR_NAME);
                        });
                    scb.line(") __awaitForEvents();");

                });

            }
        ));

        // It creates a boolean method that returns true when there is at
        // least one stale message handler.
        //  This is used to detect at run-time if the agent has at least
        //  one of these message handlers in its active
        //  behaviours. If not, a default stale message handler (defined
        //  in jadescript.core.Agent) will extract
        //  the stale messages to properly react to them. This overrides
        //  a method from the base Jadescript behaviour
        //  class.
        members.add(jvmTB.toMethod(
            inputSafe,
            "__hasStaleMessageHandler",
            typeHelper.BOOLEAN.asJvmTypeReference(),
            itMethod -> {
                boolean result =
                    Maybe.stream(input.__(BehaviourDeclaration::getFeatures))
                        .anyMatch(featureMaybe -> {
                            if (featureMaybe.isPresent()) {
                                Feature f = featureMaybe.toNullable();
                                if (f instanceof OnMessageHandler) {
                                    OnMessageHandler onMessageHandler =
                                        (OnMessageHandler) f;
                                    return onMessageHandler.isStale();
                                }
                            }
                            return false;
                        });

                compilationHelper.createAndSetBody(
                    itMethod,
                    scb -> {
                        w.returnStmnt(result ? w.True : w.False)
                            .writeSonnet(scb);
                    }
                );
            }
        ));

        super.populateMainMembers(input, members, itClass);
    }


    /**
     * Creates the behaviour's "default constructor".
     */
    private void createDefaultOnCreateHandler(
        Maybe<BehaviourDeclaration> input,
        EList<JvmMember> members,
        SavedContext savedContext
    ) {
        if (input.isNothing()) {
            return;
        }

        final BehaviourDeclaration inputSafe = input.toNullable();

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final ContextManager contextManager =
            module.get(ContextManager.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        members.add(jvmTB.toConstructor(
            inputSafe,
            itCtor -> {
                contextManager.restore(savedContext);


                final IJadescriptType contextAgent =
                    getAssociatedAgentType(input, null);

                final TypeHelper typeHelper = module.get(TypeHelper.class);

                itCtor.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    AGENT_ENV,
                    typeHelper.AGENTENV
                        .apply(List.of(
                            typeHelper.covariant(contextAgent),
                            typeHelper.jtFromClass(
                                AgentEnvType.toSEModeClass(
                                    AgentEnvType.SEMode.WITH_SE
                                )
                            )
                        )).asJvmTypeReference()
                ));

                compilationHelper.createAndSetBody(
                    itCtor,
                    scb -> {
                        w.callStmnt("super", w.expr(AGENT_ENV))
                            .writeSonnet(scb);
                        w.callStmnt("__initializeAgentEnv").writeSonnet(scb);
                        w.callStmnt("__initializeProperties").writeSonnet(scb);
                    }
                );
            }
        ));
    }


}
