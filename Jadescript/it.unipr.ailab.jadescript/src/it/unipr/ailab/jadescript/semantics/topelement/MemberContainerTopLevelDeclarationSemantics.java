package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.common.collect.HashMultimap;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FieldInitializerContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.feature.MemberBehaviourSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsDispatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Functional;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.naming.QualifiedName;
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
public abstract class MemberContainerTopLevelDeclarationSemantics
    <T extends FeatureContainer>
    extends NamedTopLevelDeclarationSemantics<T> {


    public MemberContainerTopLevelDeclarationSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    private static boolean isRepeateableHandler(Feature feature) {
        return feature instanceof OnMessageHandler
            || feature instanceof OnNativeEventHandler
            || feature instanceof OnExecuteHandler
            || feature instanceof OnExceptionHandler
            || feature instanceof OnBehaviourFailureHandler;
    }


    protected abstract void prepareAndEnterContext(
        Maybe<T> input,
        JvmDeclaredType jvmDeclaredType
    );

    protected abstract void exitContext(Maybe<T> input);


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void validateOnEdit(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }

        super.validateOnEdit(input, acceptor);

        MaybeList<Feature> features =
            input.__toList(FeatureContainer::getFeatures);

        if (input.isNothing()) {
            return;
        }


        final T inputSafe = input.toNullable();

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final Maybe<QualifiedName> qualifiedNameMaybe =
            input.__(compilationHelper::getFullyQualifiedName);

        if (qualifiedNameMaybe.isNothing()) {
            return;
        }

        final QualifiedName fullyQN = qualifiedNameMaybe.toNullable();

        JvmDeclaredType itClass = module.get(JvmTypesBuilder.class)
            .toClass(inputSafe, fullyQN, it -> {
                populateMainSuperTypes(input, it.getSuperTypes());
                populateMainMembers(input, it.getMembers(), it);
            });

        prepareAndEnterContext(input, itClass);

        validateAdditionalContextualizedAspectsOnEdit(input, acceptor);

        final SemanticsDispatchHelper semanticsDispatchHelper =
            module.get(SemanticsDispatchHelper.class);


        for (Maybe<Feature> feature : features) {
            if (feature.isNothing()) {
                continue;
            }


            semanticsDispatchHelper.dispachMemberSemantics(
                feature,
                sem -> sem.validateOnEdit(
                    (Maybe) feature,
                    (Maybe<FeatureContainer>) input,
                    acceptor
                )
            );

        }
        exitContext(input);
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void validateOnSave(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        super.validateOnSave(input, acceptor);
        MaybeList<Feature> features =
            input.__toList(FeatureContainer::getFeatures);

        validateDuplicateMembers(input, acceptor, features);

        if (input.isNothing()) {
            return;
        }


        final T inputSafe = input.toNullable();

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final Maybe<QualifiedName> qualifiedNameMaybe =
            input.__(compilationHelper::getFullyQualifiedName);

        if (qualifiedNameMaybe.isNothing()) {
            return;
        }

        final QualifiedName fullyQN = qualifiedNameMaybe.toNullable();

        JvmDeclaredType itClass = module.get(JvmTypesBuilder.class)
            .toClass(inputSafe, fullyQN, it -> {
                populateMainSuperTypes(input, it.getSuperTypes());
                populateMainMembers(input, it.getMembers(), it);
            });

        prepareAndEnterContext(input, itClass);

        validateAdditionalContextualizedAspectsOnSave(input, acceptor);

        final SemanticsDispatchHelper semanticsDispatchHelper =
            module.get(SemanticsDispatchHelper.class);

        HashSet<String> previousFields = new HashSet<>();


        Set<String> allFields = features.streamNonNulls()
            .flatMap(Functional.filterAndCast(Field.class))
            .map(Field::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        final ContextManager contextManager = module.get(ContextManager.class);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        for (Maybe<Feature> feature : features) {
            if (feature.isNothing()) {
                continue;
            }


            semanticsDispatchHelper.dispachMemberSemantics(
                feature,
                sem -> sem.validateOnSave(
                    (Maybe) feature,
                    (Maybe<FeatureContainer>) input,
                    acceptor
                )
            );


            final Feature featureSafe = feature.toNullable();

            // Checking against illegal forward references in field initializers
            if (featureSafe instanceof Field) {
                Field fieldSafe = (Field) featureSafe;
                Maybe<Field> field = some(fieldSafe);

                Maybe<RValueExpression> right = field.__(Field::getRight);
                Maybe<String> name = field.__(Field::getName);

                if (name.isNothing()) {
                    continue;
                }

                String nameSafe = name.toNullable();

                if (nameSafe.isBlank()) {
                    continue;
                }

                if (right.isNothing()) {
                    previousFields.add(nameSafe);
                    continue;
                }

                final RValueExpressionSemantics rves =
                    module.get(RValueExpressionSemantics.class);

                contextManager.enterProceduralFeature(
                    FieldInitializerContext::new
                );

                StaticState inInitializer =
                    StaticState.beginningOfOperation(module);

                List<? extends List<String>> listOfLists =
                    rves.collectFromAllNodes(right, (i, sem) -> {
                        final Maybe<ExpressionDescriptor> descriptorMaybe =
                            sem.describeExpression(i, inInitializer);

                        if (descriptorMaybe.isNothing()) {
                            return List.of(); //return from lambda
                        }

                        final ExpressionDescriptor descriptor =
                            descriptorMaybe.toNullable();

                        if (!(descriptor
                            instanceof ExpressionDescriptor.PropertyChain)) {
                            return List.of();
                        }

                        return ((ExpressionDescriptor.PropertyChain) descriptor)
                            .getProperties().toMutable();

                    });

                listOfLists.removeIf(List::isEmpty);

                listOfLists.removeIf(l -> l.size() > 2);

                for (List<String> propChain : listOfLists) {
                    if (
                        // if propChain represents an "XXX of this" expression
                        (propChain.size() == 2 && THIS.equals(propChain.get(1))
                            // or it represents an "XXX" expression (no 'this')
                            || propChain.size() == 1)
                            // where XXX is a declared field
                            && allFields.contains(propChain.get(0))
                            // but XXX is not yet declared up to this point
                            //  (so it is declared later),
                            && !previousFields.contains(propChain.get(0))) {
                        //then the init expr refers to a field not yet declared
                        validationHelper.emitError(
                            "FieldForwardDeclaration",
                            "Illegal forward reference '" +
                                propChain.get(0) + "'",
                            right,
                            acceptor
                        );
                    }
                }

                contextManager.exit();

                previousFields.add(nameSafe);
            }


        }
        exitContext(input);
    }


    private void validateDuplicateMembers(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor,
        Iterable<Maybe<Feature>> features
    ) {
        HashMultimap<String, Feature> multimap = HashMultimap.create();

        for (Maybe<Feature> featureMaybe : features) {

            Maybe<String> nameMaybe = featureMaybe.__(this::extractName);

            if (featureMaybe.isNothing() || nameMaybe.isNothing()) {
                continue;
            }

            Feature feature = featureMaybe.toNullable();
            String name = nameMaybe.toNullable();


            if (feature instanceof Field) {
                multimap.put(name, feature);
                multimap.put(
                    "get" + Strings.toFirstUpper(name),
                    feature
                );
                multimap.put(
                    "set" + Strings.toFirstUpper(name),
                    feature
                );
            } else if (feature instanceof FunctionOrProcedure) {
                int parCount =
                    ((FunctionOrProcedure) feature).getParameters().size();
                String funcName =
                    ((FunctionOrProcedure) feature).getName();
                multimap.put(funcName + "#" + parCount, feature);
            } else if (!isRepeateableHandler(feature)) {
                multimap.put(name, feature);
            }

        }

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        for (Map.Entry<String, Collection<Feature>> entry :
            multimap.asMap().entrySet()) {
            Collection<Feature> duplicates = entry.getValue();
            if (duplicates.size() <= 1) {
                continue;
            }

            for (Feature duplicate : duplicates) {
                validationHelper.emitError(
                    ISSUE_DUPLICATE_ELEMENT,
                    "Duplicate '" + entry.getKey() + "' in '" +
                        input.__(FeatureContainer::getName).orElse("") +
                        "'",
                    duplicate,
                    getNameStructuralFeature(duplicate),
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    acceptor
                );
            }
        }
    }


    protected void validateAdditionalContextualizedAspectsOnEdit(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        // Override if needed.
    }


    protected void validateAdditionalContextualizedAspectsOnSave(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        // Override if needed.
    }


    @Override
    public void populateMainMembers(
        Maybe<T> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        super.populateMainMembers(input, members, itClass);

        if (input == null) {
            return;
        }


        final SemanticsDispatchHelper dispatchHelper =
            module.get(SemanticsDispatchHelper.class);


        prepareAndEnterContext(input, itClass);


        final BlockWriter fieldInitBlock = w.block();

        BlockElementAcceptor fieldInitializerAcceptor = fieldInitBlock::add;

        for (Maybe<Feature> feature :
            Maybe.iterate(input.__(FeatureContainer::getFeatures))) {


            dispatchHelper.dispachMemberSemantics(
                feature,
                sem -> sem.generateJvmMembers(
                    wrappedSubCast(feature),
                    wrappedSuperCast(input),
                    members,
                    itClass,
                    fieldInitializerAcceptor
                )
            );


        }


        populateAdditionalContextualizedMembers(input, members, itClass);

        addThrower(input, members);

        addExceptionHandlingDispatcher(input, members);

        if (input.__(i -> i instanceof Agent).orElse(false)) {
            addBehaviourFailureHandlingDispatcher(input, members);
        }

        addInitializePropertiesMethod(
            input,
            fieldInitBlock,
            members
        );

        exitContext(input);

    }


    private void addInitializePropertiesMethod(
        Maybe<T> input,
        BlockWriter fieldInitBlock,
        List<JvmMember> members
    ) {
        if (input.isNothing()) {
            return;
        }

        final T inputSafe = input.toNullable();

        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        members.add(jvmTypesBuilder.toMethod(
            inputSafe,
            "__initializeProperties",
            jvm.typeRef(void.class),
            itMethod -> {
                itMethod.setVisibility(JvmVisibility.PRIVATE);
                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);

                compilationHelper.createAndSetBody(itMethod, scb -> {
                    w.comment("Initializing properties and event handlers:")
                        .writeSonnet(scb);
                    fieldInitBlock.writeSonnet(scb);
                });
            }
        ));
    }


    private void addBehaviourFailureHandlingDispatcher(
        Maybe<T> input,
        EList<JvmMember> members
    ) {

        if (input.isNothing()) {
            return;
        }

        final T inputSafe = input.toNullable();

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        final CompilationHelper compilationHelper = module.get(
            CompilationHelper.class);

        members.add(jvmTB.toMethod(
            inputSafe,
            BEHAVIOUR_FAILURE_HANDLER_METHOD_NAME,
            jvm.typeRef(void.class),
            itMethod -> {
                final String behaviourParam = "__behaviour";
                final String reasonParam = "__reason";
                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    behaviourParam,
                    jvm.typeRef(
                        "jadescript.core.behaviours.Behaviour<?>"
                    )
                ));
                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    reasonParam,
                    builtins.proposition().asJvmTypeReference()
                ));

                compilationHelper.createAndSetBody(
                    itMethod,
                    scb -> fillBehaviourFailureHandlingDispatcher(
                        input,
                        scb
                    )
                );

            }
        ));
    }


    private void fillBehaviourFailureHandlingDispatcher(
        Maybe<T> input,
        SourceCodeBuilder scb
    ) {
        w.variable("boolean", "__handled", w.False)
            .writeSonnet(scb);

        final MaybeList<Feature> features =
            input.__toList(FeatureContainer::getFeatures);

        final boolean thereIsAtLeastOneBFH = features.stream()
            .anyMatch(m -> m.__(f -> f instanceof OnBehaviourFailureHandler)
                .orElse(false));

        if (thereIsAtLeastOneBFH) {
            // Adds all the behaviour failure handlers invocation using
            // switch's fallthrough semantics to test all cases until one
            // matches
            scb.open("switch (0) {");
            AtomicInteger counter = new AtomicInteger(0);

            for (Maybe<Feature> feature : features) {
                if (feature.isNothing()) {
                    continue;
                }
                final Feature featureSafe = feature.toNullable();
                if (!(featureSafe instanceof OnBehaviourFailureHandler)) {
                    continue;
                }
                OnBehaviourFailureHandler behaviourFailureHandler =
                    (OnBehaviourFailureHandler) featureSafe;

                final String event =
                    synthesizeBehaviourFailureEventVariableName(
                        behaviourFailureHandler);
                scb.open("case " + counter.get() + ": {");
                // Invoke handle
                w.callStmnt(
                    event + ".handle",
                    w.expr("__behaviour"),
                    w.expr("__reason")
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
                        // ...and skip to the end (do not attempt to match
                        // with other handlers).
                        .addStatement(w.breakStmnt())
                ).setElseBranch(
                    // On mismatch, just cleanup event state...
                    w.block().add(w.callStmnt(
                        event + "." + EVENT_HANDLER_STATE_RESET_METHOD_NAME
                    ))
                    // ... then fallthrough to next switch case (no break).
                ).writeSonnet(scb);
                scb.close("}");
                counter.incrementAndGet();
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
                w.expr("\"<behaviour failure " +
                    "dispatcher>\""),
                w.expr(
                    "\"Behaviour \" + __behaviour + \" " +
                        "failed with reason: \" + __reason")
            ))
        ).writeSonnet(scb);
    }


    private void addExceptionHandlingDispatcher(
        Maybe<? extends FeatureContainer> input,
        EList<JvmMember> members
    ) {

        if (input.isNothing()) {
            return;
        }

        final FeatureContainer inputSafe = input.toNullable();

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        final CompilationHelper compilationHelper = module.get(
            CompilationHelper.class);

        members.add(jvmTB.toMethod(
            inputSafe,
            EXCEPTION_HANDLER_METHOD_NAME,
            jvm.typeRef(void.class),
            itMethod -> {
                final String excParameter = "__exc";
                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    excParameter,
                    jvm.typeRef(
                        jadescript.core.exception.JadescriptException.class
                    )
                ));
                compilationHelper.createAndSetBody(
                    itMethod,
                    scb -> fillExceptionHandlingDispatcher(input, scb)
                );

            }
        ));
    }


    private void fillExceptionHandlingDispatcher(
        Maybe<? extends FeatureContainer> input,
        SourceCodeBuilder scb
    ) {
        // Used to limit recursive throw; normally, throw statements use a
        // default thrower initialized as field in their outer feature
        // container. Exception handlers, instead, use the thrower generated
        // here, which escalates the exception.
        w.variable(
            "jadescript.core.exception.ExceptionThrower",
            EXCEPTION_THROWER_NAME,
            w.callExpr(
                "jadescript.core.exception" +
                    ".ExceptionThrower" +
                    ".__getExceptionEscalator",
                w.expr(
                    SemanticsUtils.getOuterClassThisReference(input)
                        .orElse(THIS))
            )
        ).writeSonnet(scb);


        w.variable("boolean", "__handled", w.False).writeSonnet(scb);


        final MaybeList<Feature> features =
            input.__toList(FeatureContainer::getFeatures);

        final boolean thereIsAtLeastOneEH = features.stream()
            .anyMatch(m -> m.__(f -> f instanceof OnExceptionHandler)
                .orElse(false));

        if (thereIsAtLeastOneEH) {
            // Adds all the exception handlers invocation using
            // switch's fallthrough semantics to test all cases until one
            // matches
            scb.open("switch (0) {");
            AtomicInteger counter = new AtomicInteger(0);

            for (Maybe<Feature> feature : features) {

                if (feature.isNothing()) {
                    continue;
                }

                final Feature featureSafe = feature.toNullable();

                if (!(featureSafe instanceof OnExceptionHandler)) {
                    continue;
                }

                OnExceptionHandler exceptionHandler =
                    (OnExceptionHandler) featureSafe;


                final String event =
                    synthesizeExceptionEventVariableName(exceptionHandler);

                scb.open("case " + counter.get() + ": {");

                // Invoke handle
                w.callStmnt(
                    event + ".handle",
                    w.expr("__exc"),
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
                        // ...and skip to the end (do not attempt to match
                        // with other handlers).
                        .addStatement(w.breakStmnt())
                ).setElseBranch(
                    // On mismatch, just cleanup event state...
                    w.block().add(w.callStmnt(
                        event + "." + EVENT_HANDLER_STATE_RESET_METHOD_NAME
                    ))
                    // ... then fallthrough to next switch case (no break).
                ).writeSonnet(scb);

                scb.close("}");

                counter.incrementAndGet();
            }
            scb.close("}");
        }

        w.ifStmnt(
            //When flag is not set ...
            w.expr("!__handled"),
            // ... the dispatcher "tried everything" -> escalate.
            w.block().addStatement(w.callStmnt(
                EXCEPTION_THROWER_NAME +
                    ".__throwJadescriptException",
                w.expr("__exc")
            ))
        ).writeSonnet(scb);
    }


    private void addThrower(Maybe<T> input, EList<JvmMember> members) {
        if (input.isNothing()) {
            return;
        }
        final T inputSafe = input.toNullable();

        members.add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            EXCEPTION_THROWER_NAME,
            module.get(JvmTypeHelper.class)
                .typeRef(jadescript.core.exception.ExceptionThrower.class),
            itField -> {
                module.get(CompilationHelper.class).createAndSetInitializer(
                    itField,
                    scb -> scb.add("jadescript.core.exception" +
                        ".ExceptionThrower.__DEFAULT_THROWER"
                    )
                );
            }
        ));
    }


    public void populateAdditionalContextualizedMembers(
        Maybe<T> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        // Override if needed.
    }


    @Override
    public void generateDeclaredTypes(
        Maybe<T> input,
        IJvmDeclaredTypeAcceptor acceptor,
        boolean isPreIndexingPhase
    ) {

        super.generateDeclaredTypes(input, acceptor, isPreIndexingPhase);

        final MemberBehaviourSemantics mbs =
            module.get(MemberBehaviourSemantics.class);

        for (Maybe<Feature> feature :
            Maybe.iterate(input.__(FeatureContainer::getFeatures))) {
            if (feature.isInstanceOf(MemberBehaviour.class)) {
                Maybe<MemberBehaviour> memberBehaviour =
                    feature.__(f -> (MemberBehaviour) f);
                mbs.generateMemberBehaviour(
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
        } else if (f instanceof OnNativeEventHandler) {
            result = "native";
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
        } else if (f instanceof OnNativeEventHandler) {
            result = jp.getOnNativeEventHandler_Name();
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
