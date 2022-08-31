package it.unipr.ailab.jadescript.semantics.topelement;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelBehaviourDeclarationContext;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.BehaviourDefinition;
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

public class BehaviourDefinitionSemantics extends ForEntitySemantics<BehaviourDefinition> {
    public BehaviourDefinitionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected void prepareAndEnterContext(Maybe<BehaviourDefinition> input, JvmDeclaredType jvmDeclaredType) {
        if (input.__(BehaviourDefinition::isMemberBehaviour).extract(nullAsFalse)) {
            module.get(ContextManager.class).enterEmulatedFile();
        }
        module.get(ContextManager.class).enterTopLevelDeclaration((module, outer) ->
                new TopLevelBehaviourDeclarationContext(
                        module,
                        outer,
                        getUsedOntologyTypes(input),
                        getAssociatedAgentType(input),
                        jvmDeclaredType
                )
        );
        module.get(ContextManager.class).enterProceduralFeatureContainer(
                module.get(TypeHelper.class).jtFromJvmType(jvmDeclaredType),
                input
        );
    }

    @Override
    protected void exitContext(Maybe<BehaviourDefinition> input) {
        module.get(ContextManager.class).exit();//ProceduralFeatureContainerContext
        module.get(ContextManager.class).exit();//TopLevelBehaviourDeclarationContext
        if (input.__(BehaviourDefinition::isMemberBehaviour).extract(nullAsFalse)) {
            module.get(ContextManager.class).exit();//EmulatedFileContext
        }

    }


    @Override
    public void validate(Maybe<BehaviourDefinition> input, ValidationMessageAcceptor acceptor) {
        super.validate(input, acceptor);
    }

    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(Maybe<BehaviourDefinition> input) {
        final Maybe<JvmTypeReference> typeArg = input.__(BehaviourDefinition::getForAgent).extract(Maybe::flatten);
        List<IJadescriptType> typeArgs = new ArrayList<>();
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (typeArg.isPresent()) {
            typeArgs.add(typeHelper.jtFromJvmTypeRef(typeArg.toNullable()));
        } else {
            typeArgs.add(typeHelper.AGENT);
        }
        if (Maybe.nullAsFalse(input
                .__(BehaviourDefinition::getType)
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
    public Optional<IJadescriptType> defaultSuperType(Maybe<BehaviourDefinition> input) {
        final Maybe<JvmTypeReference> typeArg = input.__(BehaviourDefinition::getForAgent).extract(Maybe::flatten);
        List<IJadescriptType> typeArgs = new ArrayList<>();
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (typeArg.isPresent()) {
            typeArgs.add(typeHelper.jtFromJvmTypeRef(typeArg.toNullable()));
        }
        if (Maybe.nullAsFalse(input
                .__(BehaviourDefinition::getType)
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
    public void populateMainMembers(Maybe<BehaviourDefinition> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        super.populateMainMembers(input, members, itClass);
        if (input == null) return;
        // if there are no "on create" event handlers
        final Optional<OnCreateHandler> onCreateHandler = stream(input.__(BehaviourDefinition::getFeatures))
                .filter(j -> j.__(f -> f instanceof OnCreateHandler).extract(nullAsFalse))
                .map(Maybe::toOpt)
                .findFirst()
                .flatMap(x -> x)
                .map(f -> (OnCreateHandler) f);

        List<String> ctorParametersDefaultValues = new ArrayList<>();
        if (onCreateHandler.isPresent()) {
            final List<Maybe<FormalParameter>> formalParameters = toListOfMaybes(onCreateHandler.get().getParameters());
            formalParameters
                    .stream()
                    .map(m -> m.__(FormalParameter::getType))
                    .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                    .map(t -> {
                        if (t instanceof EmptyCreatable) {
                            return ((EmptyCreatable) t).compileNewEmptyInstance();
                        } else {
                            return "null";
                        }
                    })
                    .forEach(ctorParametersDefaultValues::add);

        } else {
            createDefaultOnCreateHandler(input, members);
        }


        input.safeDo(inputSafe -> {

            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    "__createEmpty",
                    module.get(TypeHelper.class).typeRef(inputSafe),
                    itMethod -> {
                        itMethod.setStatic(true);
                        module.get(CompilationHelper.class).createAndSetBody(
                                itMethod,
                                scb -> {
                                    scb.line("return new "
                                            + module.get(IQualifiedNameProvider.class)
                                            .getFullyQualifiedName(inputSafe)
                                            .toString(".")
                                            + "(" + String.join(", ", ctorParametersDefaultValues)
                                            + ");");
                                }
                        );
                    }
            ));


            members.add(module.get(JvmTypesBuilder.class).toField(
                            inputSafe,
                            IGNORE_MSG_HANDLERS_VAR_NAME,
                            module.get(TypeHelper.class).typeRef(Boolean.class),
                            itField -> module.get(CompilationHelper.class).createAndSetInitializer(itField, scb -> scb.add("false"))
                    )
            );

            // It creates the doAction() method with a set of mechanisms that ensures that the behaviour is
            //  safely and efficiently put to "waiting" state when there is no action to execute (i.e., there
            //  is no "on execute" and no message in the inbox that matched the "on message" handler conditions...).
            // Moreover, if this behaviour did not extract any message from the inbox, it updates the agent's internal
            //  state to declare all messages in the inbox as "ignored" by this behaviour. This is useful for stale
            //  message detection (stale message = message in the inbox ignored at least once by all the currently
            //  active behaviours).
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    "doAction",
                    module.get(TypeHelper.class).typeRef(void.class),
                    itMethod -> {
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class)
                                .toParameter(inputSafe, "_tickCount", module.get(TypeHelper.class).typeRef(Integer.TYPE)));
                        itMethod.setVisibility(JvmVisibility.PUBLIC);
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            w.assign("this." + IGNORE_MSG_HANDLERS_VAR_NAME, w.expr("false")).writeSonnet(scb);
                            scb.line("super.doAction(_tickCount);");

                            toListOfMaybes(input.__(FeatureContainer::getFeatures)).stream()
                                    .filter(Maybe::isPresent)
                                    .map(Maybe::toNullable)
                                    .filter(BehaviourDefinitionSemantics::hasEventClassSystem)
                                    .map(this::synthesizeEventVariableName)
                                    .forEach(eventName -> scb.line(eventName + ".run();"));

                            w.ifStmnt(
                                    w.expr("!this." + IGNORE_MSG_HANDLERS_VAR_NAME),
                                    w.block().addStatement(
                                            w.callStmnt("this.__noMessageHandled")
                                    )
                            ).writeSonnet(scb);

                            scb.add("if ( true ");
                            toListOfMaybes(input.__(FeatureContainer::getFeatures)).stream()
                                    .filter(Maybe::isPresent)
                                    .map(Maybe::toNullable)
                                    .filter(BehaviourDefinitionSemantics::hasEventClassSystem)
                                    .map(this::synthesizeEventVariableName)
                                    .forEach(eventName -> {
                                        scb.add(" && !" + eventName + "." + MESSAGE_RECEIVED_BOOL_VAR_NAME);
                                    });
                            scb.line(") __awaitForEvents();");

                        });

                    }
            ));

            // It creates a boolean method that returns true when there is at least one stale message handler.
            //  This is used to detect at run-time if the agent has at least one of these message handlers in its active
            //  behaviours. If not, a default stale message handler (defined in jadescript.core.Agent) will extract
            //  the stale messages to properly react to them. This overrides a method from the base Jadescript behaviour
            //  class.
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    "__hasStaleMessageHandler",
                    module.get(TypeHelper.class).BOOLEAN.asJvmTypeReference(),
                    itMethod -> {

                        boolean result = Maybe.stream(input.__(BehaviourDefinition::getFeatures))
                                .anyMatch(featureMaybe -> {
                                    if (featureMaybe.isPresent()) {
                                        Feature f = featureMaybe.toNullable();
                                        if (f instanceof OnMessageHandler) {
                                            OnMessageHandler onMessageHandler = (OnMessageHandler) f;
                                            return onMessageHandler.isStale();
                                        }
                                    }
                                    return false;
                                });

                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            w.returnStmnt(result ? w.True : w.False).writeSonnet(scb);
                        });
                    }
            ));
        });
    }

    private static boolean hasEventClassSystem(Feature input) {
        return input instanceof OnExecuteHandler || input instanceof OnMessageHandler || input instanceof OnPerceptHandler;
    }

    private void createDefaultOnCreateHandler(Maybe<BehaviourDefinition> input, EList<JvmMember> members) {
        //create the behaviour's "default constructor"
        input.safeDo(inputSafe -> {
            members.add(module.get(JvmTypesBuilder.class).toConstructor(inputSafe, itCtor -> {
                module.get(CompilationHelper.class).createAndSetBody(itCtor, s -> {
                    s.line("// do nothing.");
                });
            }));
        });
    }
}
