package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.common.collect.HashMultimap;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.feature.MemberBehaviourSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsDispatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Functional;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.OptionalUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 27/04/18.
 */
@Singleton
public abstract class FeatureContainerSemantics<T extends FeatureContainer>
        extends NamedEntitySemantics<T> {


    public FeatureContainerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    private static boolean repeateableHandler(Feature feature) {
        return feature instanceof OnMessageHandler
                || feature instanceof OnPerceptHandler
                || feature instanceof OnExecuteHandler
                || feature instanceof OnExceptionHandler
                || feature instanceof OnBehaviourFailureHandler;
    }

    protected abstract void prepareAndEnterContext(Maybe<T> input, JvmDeclaredType jvmDeclaredType);

    protected abstract void exitContext(Maybe<T> input);

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void validate(Maybe<T> input, ValidationMessageAcceptor acceptor) {
        super.validate(input, acceptor);
        if (input == null) return;

        //noinspection unchecked
        validateForForwardDeclaration((Maybe<FeatureContainer>) input, acceptor);

        Iterable<Maybe<Feature>> features = Maybe.toListOfMaybes(input.__(FeatureContainer::getFeatures));

        validateDuplicateFeatures(input, acceptor, features);


        input.safeDo(inputsafe -> {
            input.__(module.get(CompilationHelper.class)::getFullyQualifiedName).safeDo(fullyqnsafe -> {
                //this is done only if input and getFullyQualifiedName(input) are both != null
                JvmDeclaredType itClass = module.get(JvmTypesBuilder.class).toClass(inputsafe, fullyqnsafe, it -> {
                    populateMainSuperTypes(input, it.getSuperTypes());
                    populateMainMembers(input, it.getMembers(), it);
                });

                prepareAndEnterContext(input, itClass);

                validateAdditionalContextualizedAspects(input, acceptor);

                for (Maybe<Feature> feature : features) {
                    if (feature.isPresent()) {

                        //noinspection unchecked,rawtypes
                        module.get(SemanticsDispatchHelper.class).dispachFeatureSemantics(feature, sem ->
                                sem.validateFeature((Maybe) feature, (Maybe<FeatureContainer>) input, acceptor)
                        );

                    }
                }

                exitContext(input);
            });
        });
    }

    private void validateDuplicateFeatures(Maybe<T> input, ValidationMessageAcceptor acceptor, Iterable<Maybe<Feature>> features) {
        HashMultimap<String, Feature> multimap = HashMultimap.create();
        for (Maybe<Feature> feature : features) {
            Optional<String> nameO = feature.__(this::extractName).toOpt();
            Optional<Feature> featureO = feature.toOpt();

            if (OptionalUtils.areAllPresent(nameO, featureO)) {
                //noinspection OptionalGetWithoutIsPresent
                if (featureO.get() instanceof Field) {
                    //noinspection OptionalGetWithoutIsPresent
                    multimap.put(nameO.get(), featureO.get());
                    multimap.put("get" + Strings.toFirstUpper(nameO.get()), featureO.get());
                    multimap.put("set" + Strings.toFirstUpper(nameO.get()), featureO.get());
                } else if (featureO.get() instanceof FunctionOrProcedure) {
                    int parCount = ((FunctionOrProcedure) featureO.get()).getParameters().size();
                    String funcName = ((FunctionOrProcedure) featureO.get()).getName();
                    multimap.put(funcName + "#" + parCount, featureO.get());
                } else if (repeateableHandler(featureO.get())) {
                    //do nothing
                } else {
                    //noinspection OptionalGetWithoutIsPresent
                    multimap.put(nameO.get(), featureO.get());
                }
            }
        }
        for (Map.Entry<String, Collection<Feature>> entry : multimap.asMap().entrySet()) {
            Collection<Feature> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                for (Feature d : duplicates) {

                    acceptor.acceptError(
                            "Duplicate feature '" + entry.getKey() + "' in '" +
                                    input.__(FeatureContainer::getName).orElse("") + "'",
                            d,
                            getNameStructuralFeature(d),
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            ISSUE_DUPLICATE_ELEMENT
                    );
                }
            }
        }
    }

    protected void validateAdditionalContextualizedAspects(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    ) {
        // Override if needed.
    }

    private void validateForForwardDeclaration(Maybe<FeatureContainer> input, ValidationMessageAcceptor acceptor) {
        List<Maybe<Feature>> maybeFeatures = Maybe.toListOfMaybes((input).__(FeatureContainer::getFeatures));
        Set<String> inCurrentClass = maybeFeatures.stream()
                .filter(Maybe::isPresent)
                .map(Maybe::toNullable)
                .flatMap(Functional.filterAndCast(Field.class))
                .map(Field::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        HashSet<String> declared = new HashSet<>();
        maybeFeatures.stream()
                .filter(Maybe::isPresent)
                .map(Maybe::toNullable)
                .flatMap(Functional.filterAndCast(Field.class))
                .map(Maybe::of)
                .forEach(f -> {
                    Maybe<RValueExpression> right = f.__(Field::getRight);
                    Maybe<String> name = f.__(Field::getName);
                    right.safeDo(rightSafe -> {

                        List<? extends List<String>> listOfLists =
                                module.get(RValueExpressionSemantics.class).collectFromAllNodes(
                                        right,
                                        (in, expressionSemantics1) -> {
                                            @SuppressWarnings({"unchecked", "rawtypes"}) final List<String> list =
                                                    expressionSemantics1.describeExpression((Maybe) in, );
                                            return list;
                                        }
                                );
                        List<List<String>> collect = new ArrayList<>();
                        for (List<String> l : listOfLists) {
                            if (!l.isEmpty()) {
                                collect.add(l);
                            }
                        }

                        for (List<String> propChain : collect) {
                            if (propChain.size() == 2
                                    && Util.getOuterClassThisReference(input).orElse(THIS).equals(propChain.get(1))
                                    && inCurrentClass.contains(propChain.get(0))) {
                                module.get(ValidationHelper.class).assertion(
                                        declared.contains(propChain.get(0)),
                                        "FieldForwardDeclaration",
                                        "Unable to resolve name '" + propChain.get(0) + "'",
                                        of(rightSafe),
                                        acceptor
                                );
                            } else if (propChain.size() == 1 && inCurrentClass.contains(propChain.get(0))) {
                                module.get(ValidationHelper.class).assertion(
                                        declared.contains(propChain.get(0)),
                                        "FieldForwardDeclaration",
                                        "Unable to resolve name '" + propChain.get(0) + "'",
                                        of(rightSafe),
                                        acceptor
                                );
                            }
                        }
                    });

                    name.safeDo(declared::add);
                });
    }


    @Override
    public void populateMainMembers(Maybe<T> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        super.populateMainMembers(input, members, itClass);
        if (input == null) return;

        prepareAndEnterContext(input, itClass);
        for (Maybe<Feature> feature : Maybe.iterate(input.__(FeatureContainer::getFeatures))) {
            module.get(SemanticsDispatchHelper.class).dispachFeatureSemantics(feature, featureSemantics -> {
                featureSemantics.generateJvmMembers(wrappedSubCast(feature), wrappedSuperCast(input), members, itClass);
            });
        }
        populateAdditionalContextualizedMembers(input, members, itClass);
        addThrower(input, members);
        addExceptionHandlingDispatcher(input, members);
        if (input.__(i -> i instanceof Agent).extract(nullAsFalse)) {
            addBehaviourFailureHandlingDispatcher(input, members);
        }
        exitContext(input);

    }

    private void addBehaviourFailureHandlingDispatcher(Maybe<T> input, EList<JvmMember> members) {
        input.safeDo(inputSafe -> {
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    BEHAVIOUR_FAILURE_HANDLER_METHOD_NAME,
                    module.get(TypeHelper.class).VOID.asJvmTypeReference(),
                    itMethod -> {
                        final String behaviourParam = "__behaviour";
                        final String reasonParam = "__reason";
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                inputSafe,
                                behaviourParam,
                                module.get(TypeHelper.class).typeRef("jadescript.core.behaviours.Behaviour<?>")
                        ));
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                inputSafe,
                                reasonParam,
                                module.get(TypeHelper.class).typeRef(jadescript.content.JadescriptProposition.class)
                        ));

                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {

                            w.variable("boolean", "__handled", w.False).writeSonnet(scb);
                            final List<Maybe<Feature>> features = toListOfMaybes(input.__(FeatureContainer::getFeatures));
                            if (features.stream()
                                    .anyMatch(m -> m.__(f -> f instanceof OnBehaviourFailureHandler).extract(nullAsFalse))) {
                                // Adds all the behaviour failure handlers invocations
                                scb.open("switch (0) {");
                                AtomicInteger counter = new AtomicInteger(0);
                                for (Maybe<Feature> feature : features) {
                                    if (feature.__(f -> f instanceof OnBehaviourFailureHandler).extract(nullAsFalse)) {
                                        feature.__(f -> (OnBehaviourFailureHandler) f).safeDo(behaviourFailureHandler -> {
                                            final String event = synthesizeBehaviourFailureEventVariableName(behaviourFailureHandler);
                                            scb.open("case " + counter.get() + ": {");
                                            // Invoke handle
                                            w.callStmnt(
                                                    event + ".handle",
                                                    w.expr(behaviourParam),
                                                    w.expr(reasonParam)
                                            ).writeSonnet(scb);

                                            // Check after each invoke if handler matched
                                            w.ifStmnt(
                                                    w.expr(event + "." + FAILURE_MATCHED_BOOL_VAR_NAME),
                                                    w.block()
                                                            // On match, set the flag...
                                                            .addStatement(w.assign("__handled", w.True))
                                                            // ... cleanup event state ...
                                                            .addStatement(w.callStmnt(
                                                                    event + "." + EVENT_HANDLER_STATE_RESET_METHOD_NAME
                                                            ))
                                                            // ...and skip to the end (do not attempt to match with
                                                            // other handlers).

                                                            .addStatement(w.breakStmnt())
                                            ).setElseBranch(
                                                    // Cleanup event state.
                                                    w.block().add(w.callStmnt(
                                                            event + "." + EVENT_HANDLER_STATE_RESET_METHOD_NAME
                                                    ))
                                            ).writeSonnet(scb);
                                            scb.close("}");
                                            counter.incrementAndGet();
                                        });

                                    }
                                }
                                scb.close("}");
                            }


                            w.ifStmnt(
                                    //When flag is not set ...
                                    w.expr("!__handled"),
                                    // ... the dispatcher "tried everything" -> log the failure.
                                    w.block().addStatement(w.callStmnt(
                                            "jadescript.core.Agent.doLog",
                                            w.expr("java.util.logging.Level.INFO"),
                                            w.expr("this.getClass().getName()"),
                                            w.expr("this"),
                                            w.expr("\"<behaviour failure dispatcher>\""),
                                            w.expr("\"Behaviour \" + __behaviour + \" failed with reason: \" + __reason")
                                    ))
                            ).writeSonnet(scb);
                        });

                    }
            ));
        });
    }

    private void addExceptionHandlingDispatcher(Maybe<? extends FeatureContainer> input, EList<JvmMember> members) {
        input.safeDo(inputsafe -> {
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputsafe,
                    EXCEPTION_HANDLER_METHOD_NAME,
                    module.get(TypeHelper.class).VOID.asJvmTypeReference(),
                    itMethod -> {
                        final String excParameter = "__exc";
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                inputsafe,
                                excParameter,
                                module.get(TypeHelper.class).typeRef(jadescript.core.exception.JadescriptException.class)
                        ));
                        //Used to limit recursive throw; normally, throw stataments use a default thrower initialized
                        // as field in the current feature container.
                        //Exception handlers, instead, use the thrower generated here, which escalates the exception.
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            w.variable(
                                    "jadescript.core.exception.ExceptionThrower",
                                    EXCEPTION_THROWER_NAME,
                                    w.callExpr(
                                            "jadescript.core.exception.ExceptionThrower.__getExceptionEscalator",
                                            w.expr(Util.getOuterClassThisReference(input).orElse(THIS))
                                    )
                            ).writeSonnet(scb);


                            w.variable("boolean", "__handled", w.False).writeSonnet(scb);
                            final List<Maybe<Feature>> features = toListOfMaybes(input.__(FeatureContainer::getFeatures));
                            if (features.stream()
                                    .anyMatch(m -> m.__(f -> f instanceof OnExceptionHandler).extract(nullAsFalse))) {
                                // Adds all the exception handlers invocations
                                scb.open("switch (0) {");
                                AtomicInteger counter = new AtomicInteger(0);
                                for (Maybe<Feature> feature : features) {
                                    if (feature.__(f -> f instanceof OnExceptionHandler).extract(nullAsFalse)) {
                                        feature.__(f -> (OnExceptionHandler) f).safeDo(exceptionHandler -> {
                                            final String event = synthesizeExceptionEventVariableName(exceptionHandler);
                                            scb.open("case " + counter.get() + ": {");
                                            // Invoke handle
                                            w.callStmnt(
                                                    event + ".handle",
                                                    w.expr(excParameter),
                                                    w.expr(EXCEPTION_THROWER_NAME)
                                            ).writeSonnet(scb);

                                            // Check after each invoke if handler matched
                                            w.ifStmnt(
                                                    w.expr(event + "." + EXCEPTION_MATCHED_BOOL_VAR_NAME),
                                                    w.block()
                                                            // On match, set the flag...
                                                            .addStatement(w.assign("__handled", w.True))
                                                            // ... cleanup event state ...
                                                            .addStatement(w.callStmnt(
                                                                    event + "." + EVENT_HANDLER_STATE_RESET_METHOD_NAME
                                                            ))
                                                            // ...and skip to the end (do not attempt to match with
                                                            // other handlers).

                                                            .addStatement(w.breakStmnt())
                                            ).setElseBranch(
                                                    // Cleanup event state.
                                                    w.block().add(w.callStmnt(
                                                            event + "." + EVENT_HANDLER_STATE_RESET_METHOD_NAME
                                                    ))
                                            ).writeSonnet(scb);
                                            scb.close("}");
                                            counter.incrementAndGet();
                                        });

                                    }
                                }
                                scb.close("}");
                            }


                            w.ifStmnt(
                                    //When flag is not set ...
                                    w.expr("!__handled"),
                                    // ... the dispatcher "tried everything" -> escalate.
                                    w.block().addStatement(w.callStmnt(
                                            EXCEPTION_THROWER_NAME + ".__throwJadescriptException",
                                            w.expr(excParameter)
                                    ))
                            ).writeSonnet(scb);
                        });

                    }
            ));
        });
    }

    private void addThrower(Maybe<T> input, EList<JvmMember> members) {
        input.safeDo(inputSafe -> {
            members.add(module.get(JvmTypesBuilder.class).toField(
                    inputSafe,
                    EXCEPTION_THROWER_NAME,
                    module.get(TypeHelper.class).typeRef(jadescript.core.exception.ExceptionThrower.class),
                    itField -> {
                        module.get(CompilationHelper.class).createAndSetInitializer(itField, scb -> {
                            w.expr("jadescript.core.exception.ExceptionThrower.__DEFAULT_THROWER").writeSonnet(scb);
                        });
                    }
            ));
        });
    }


    public void populateAdditionalContextualizedMembers(
            Maybe<T> input,
            EList<JvmMember> members,
            JvmDeclaredType itClass
    ) {
        // Override if needed.
    }

    @Override
    public void generateDeclaredTypes(Maybe<T> input, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {

        super.generateDeclaredTypes(input, acceptor, isPreIndexingPhase);

        for (Maybe<Feature> feature : Maybe.iterate(input.__(FeatureContainer::getFeatures))) {
            if (feature.isInstanceOf(MemberBehaviour.class)) {
                Maybe<MemberBehaviour> memberBehaviour = feature.__(f -> (MemberBehaviour) f);
                module.get(MemberBehaviourSemantics.class).generateMemberBehaviour(
                        memberBehaviour,
                        input,
                        acceptor,
                        isPreIndexingPhase
                );
            }
        }


    }

    private String extractName(final Feature f) {
        String result = null;
        if (f instanceof Field) {
            result = ((Field) f).getName();
        } else if (f instanceof FunctionOrProcedure) {
            result = ((FunctionOrProcedure) f).getName();
        } else if (f instanceof OnMessageHandler) {
            result = "message";
        } else if (f instanceof OnPerceptHandler) {
            result = "percept";
        } else if (f instanceof OnCreateHandler) {
            result = "create";
        } else if (f instanceof OnDestroyHandler) {
            result = "destroy";
        } else if (f instanceof OnActivateHandler) {
            result = "activate";
        } else if (f instanceof OnDeactivateHandler) {
            result = "deactivate";
        } else if (f instanceof OnExecuteHandler) {
            result = "execute";
        } else if (f instanceof OnExceptionHandler) {
            result = "exception";
        } else if (f instanceof OnBehaviourFailureHandler) {
            result = "behaviour failure";
        } else if (f instanceof Predicate) {
            result = ((Predicate) f).getName();
        } else if (f instanceof Proposition) {
            result = ((Proposition) f).getName();
        } else if (f instanceof Concept) {
            result = ((Concept) f).getName();
        } else if (f instanceof OntologyAction) {
            result = ((OntologyAction) f).getName();
        }
        return result;
    }

    private EStructuralFeature getNameStructuralFeature(final Feature f) {
        final JadescriptPackage jp = JadescriptPackage.eINSTANCE;
        EAttribute result = null;
        if (f instanceof Field) {
            result = jp.getField_Name();
        } else if (f instanceof FunctionOrProcedure) {
            result = jp.getFunctionOrProcedure_Name();
        } else if (f instanceof OnMessageHandler) {
            result = jp.getOnMessageHandler_Name();
        } else if (f instanceof OnPerceptHandler) {
            result = jp.getOnPerceptHandler_Name();
        } else if (f instanceof OnCreateHandler) {
            result = jp.getOnCreateHandler_Name();
        } else if (f instanceof OnDestroyHandler) {
            result = jp.getOnDestroyHandler_Name();
        } else if (f instanceof OnActivateHandler) {
            result = jp.getOnActivateHandler_Name();
        } else if (f instanceof OnDeactivateHandler) {
            result = jp.getOnDeactivateHandler_Name();
        } else if (f instanceof OnExecuteHandler) {
            result = jp.getOnExecuteHandler_Name();
        } else if (f instanceof OnExceptionHandler) {
            result = jp.getOnExceptionHandler_Name();
        } else if (f instanceof OnBehaviourFailureHandler) {
            result = jp.getOnBehaviourFailureHandler_Name();
        } else if (f instanceof Predicate) {
            result = jp.getNamedFeature_Name();
        } else if (f instanceof Proposition) {
            result = jp.getNamedFeature_Name();
        } else if (f instanceof Concept) {
            result = jp.getNamedFeature_Name();
        } else if (f instanceof OntologyAction) {
            result = jp.getNamedFeature_Name();
        }

        return result;
    }

}
