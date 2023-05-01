package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnMessageHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnMessageHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeLatticeComputer;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.BaseMessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jadescript.lang.Performative;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.eitherGet;
import static it.unipr.ailab.maybe.Maybe.some;

/**
 * Created on 26/10/2018.
 */
@SuppressWarnings("restriction")
@Singleton
public class OnMessageHandlerSemantics
    extends DeclarationMemberSemantics<OnMessageHandler> {

    public OnMessageHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnMessageHandler> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        if (input.isNothing()) {
            return;
        }
        OnMessageHandler inputSafe = input.toNullable();

        final SavedContext savedContext =
            module.get(ContextManager.class).save();


        final String eventClassName =
            synthesizeBehaviourEventClassName(inputSafe);

        JvmGenericType eventClass = createEventClass(
            input,
            inputSafe,
            savedContext,
            eventClassName
        );

        members.add(eventClass);

        addEventField(
            members,
            inputSafe,
            eventClass,
            fieldInitializationAcceptor
        );
    }


    private void addEventField(
        EList<JvmMember> members,
        OnMessageHandler inputSafe,
        JvmGenericType eventClass,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        final String eventFieldName = synthesizeEventFieldName(inputSafe);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        members.add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            eventFieldName,
            jvm.typeRef(eventClass),
            itField -> {
                itField.setVisibility(JvmVisibility.PRIVATE);
                module.get(CompilationHelper.class)
                    .createAndSetInitializer(itField, scb -> {
                        scb.add("null");
                        fieldInitializationAcceptor.accept(
                            w.assign(
                                eventFieldName,
                                w.expr("new " +
                                    jvm.typeRef(eventClass)
                                        .getQualifiedName('.') +
                                    "()"
                                )
                            )
                        );
                    });
            }
        ));
    }


    private void fillRunMethod(
        Maybe<OnMessageHandler> input,
        SavedContext savedContext,
        SourceCodeBuilder scb
    ) {
//generating => if ([OUTERCLASS].this.__ignoreMessageHandlers) {
//generating =>     this.__eventFired = false;
//generating =>     return;
//generating => }
        w.ifStmnt(
            w.expr(SemanticsUtils.getOuterClassThisReference(input)
                + "." + IGNORE_MSG_HANDLERS_VAR_NAME),
            w.block().addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("false")
                )
            ).addStatement(w.returnStmnt())
        ).writeSonnet(scb);


//generating => [... message template auxiliary statements...]
//generating => MessageTemplate _mt = [...]
        final Maybe<String> performativeString =
            input.__(OnMessageHandler::getPerformative);
        Maybe<Performative> performative = performativeString
            .__(Performative.performativeByName::get);

        final Maybe<WhenExpression> whenBody =
            input.__(OnMessageHandler::getWhenBody);
        final Maybe<Pattern> contentPattern =
            input.__(OnMessageHandler::getPattern);

        final Maybe<OptionalBlock> body =
            input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);


        module.get(ContextManager.class).restore(savedContext);

        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);


        module.get(ContextManager.class).enterProceduralFeature(
            (m, o) -> new OnMessageHandlerWhenExpressionContext(
                m, performative, o));

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        final IJadescriptType contentUpperBound = performative
            .__(typeSolver::getContentBoundForPerformative)
            .orElseGet(() -> builtins.any(
                "Could not solve message type for performative '" +
                    performative + "'."));

        MessageType initialMsgType = typeSolver.instantiateMessageType(
            input.__(OnMessageHandler::getPerformative),
            contentUpperBound,
            /*normalizeToUpperBounds=*/ true
        );

        IJadescriptType pattNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedMessageType = initialMsgType;

        final Maybe<LValueExpression> pattern = input
            .__(OnMessageHandler::getPattern)
            .__(x -> (LValueExpression) x);

        Function<StaticState, StaticState> prepareBodyState;
        final StaticState afterPatternDidMatch;
        String part1;
        if (pattern.isPresent()) {
            final PatternMatchHelper patternMatchHelper =
                module.get(PatternMatchHelper.class);

            PatternMatchInput<LValueExpression> patternMatchInput
                = patternMatchHelper.handlerHeader(
                contentUpperBound,
                pattern,
                some(ExpressionDescriptor.contentOfMessageReference)
            );

            LValueExpressionSemantics lves =
                module.get(LValueExpressionSemantics.class);

            // Compile the pattern match operation into a PatternMatcher and
            // fill scb of the corresponding auxiliary statements
            PatternMatcher matcher = lves.compilePatternMatch(
                patternMatchInput,
                beforePattern,
                (member) -> member.writeSonnet(scb)
            );


            pattNarrowedContentType = lves.inferPatternType(
                patternMatchInput,
                beforePattern
            ).solve(contentUpperBound);


            final String patternMatcherClassName =
                patternMatchHelper.getPatternMatcherClassName(pattern);

            final String patternMatcherVariableName =
                patternMatchHelper.getPatternMatcherVariableName(pattern);

            LocalClassStatementWriter patternMatchClass = w.localClass(
                patternMatcherClassName
            );

            patternMatchClass.addMember(
                patternMatchHelper.getSelfField(pattern)
            );

            matcher.getAllWriters().forEach(patternMatchClass::addMember);

            patternMatchClass.writeSonnet(scb);

            w.variable(
                patternMatcherClassName,
                patternMatcherVariableName,
                w.expr("new " + patternMatcherClassName + "()")
            ).writeSonnet(scb);

            final StaticState advancePattern = lves.advancePattern(
                patternMatchInput,
                beforePattern
            );
            afterPatternDidMatch = lves.assertDidMatch(
                patternMatchInput,
                advancePattern
            );

            prepareBodyState = s -> lves.assertDidMatch(
                patternMatchInput,
                s
            );

            part1 = matcher.compilePatternMatchExpression(
                initialMsgType.namespace().getContentProperty()
                    .dereference((__) -> MESSAGE_VAR_NAME)
                    .compileRead((member) -> member.writeSonnet(scb))
            );
        } else {
            prepareBodyState = Function.identity();
            afterPatternDidMatch = beforePattern;
            part1 = "";
        }


        String part2;
        StaticState afterWhenExprRetunedTrue;
        if (whenExpr.isPresent()) {
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);

            part2 = rves.compile(
                whenExpr,
                afterPatternDidMatch,
                (member) -> member.writeSonnet(scb)
            );

            final StaticState afterWhenExpr = rves.advance(
                whenExpr,
                afterPatternDidMatch
            );

            afterWhenExprRetunedTrue = rves.assertReturnedTrue(
                whenExpr,
                afterWhenExpr
            );

            wexpNarrowedContentType = afterWhenExprRetunedTrue.inferUpperBound(
                    ExpressionDescriptor.contentOfMessageReference
                ).findFirst()
                .orElse(contentUpperBound);

            wexpNarrowedMessageType = afterWhenExprRetunedTrue.inferUpperBound(
                    ExpressionDescriptor.messageReference
                ).findFirst()
                .orElse(initialMsgType);

            prepareBodyState = prepareBodyState.andThen(
                s -> rves.assertReturnedTrue(
                    whenExpr,
                    s
                )
            );

        } else {
            afterWhenExprRetunedTrue = afterPatternDidMatch;
            part2 = "";
        }

        String compiledExpression;
        if (!part1.isBlank() && !part2.isBlank()) { // Both are present...
            // ...infix &&
            compiledExpression = "(" + part1 + ") && (" + part2 + ")";
        } else if (part1.isBlank() && part2.isBlank()) { // Both are absent...
            // ... use true
            compiledExpression = "true";
        } else {
            // ... otherwise return the one present
            compiledExpression = part1 + part2;
        }

        module.get(ContextManager.class).exit();

        final IJadescriptType finalContentType;
        if (wexpNarrowedMessageType instanceof BaseMessageType) {
            final IJadescriptType messageBasedContentType =
                ((BaseMessageType) wexpNarrowedMessageType)
                    .getContentType();
            finalContentType = lattice.getGLB(
                getNarrowedContentErrorMsg(
                    pattNarrowedContentType,
                    wexpNarrowedContentType,
                    messageBasedContentType
                ),
                pattNarrowedContentType,
                wexpNarrowedContentType,
                messageBasedContentType
            );
        } else {
            finalContentType = lattice.getGLB(
                pattNarrowedContentType,
                wexpNarrowedContentType,
                TypeHelper.getNarrowedContentErrorMsg(
                    pattNarrowedContentType,
                    wexpNarrowedContentType
                )
            );
        }


        //Building the message template
        List<ExpressionWriter> messageTemplateExpressions =
            new ArrayList<>();


        // add performative constraint (if there is one)
        performativeString.safeDo(p -> {
            messageTemplateExpressions.add(
                TemplateCompilationHelper.performative(p)
            );
        });

        // add "Not a native event" constraint
        messageTemplateExpressions.add(
            TemplateCompilationHelper.notNative()
        );

        if (input.__(OnMessageHandler::isStale).orElse(false)) {
            // add staleness constraint
            messageTemplateExpressions.add(
                TemplateCompilationHelper.isStale()
            );
        }


        // if there is a when-exprssion or a pattern,then add
        // the corresponding constraint
        if (whenBody.isPresent() || contentPattern.isPresent()) {
            messageTemplateExpressions.add(
                TemplateCompilationHelper.customMessage("__templMsg", w.block()
                    .addStatement(w.variable(
                        "jadescript.core.message.Message",
                        MESSAGE_VAR_NAME,
                        w.expr("jadescript.core.message.Message" +
                            ".wrap(__templMsg)")
                    )).addStatement(
                        w.tryCatch(
                            w.block().addStatement(
                                w.returnStmnt(w.expr(compiledExpression))
                            )
                        ).addCatchBranch("java.lang.Throwable", "_e",
                            w.block()
                                .addStatement(w.callStmnt("_e.printStackTrace"))
                                .addStatement(w.returnStmnt(w.expr("false")))
                        ))
                )
            );
        }

        // Put all constraints in a
        // MessageTemplate.and(..., MessageTemplate.and(..., ...))
        // chain.
        final ExpressionWriter composedMT =
            messageTemplateExpressions.stream().reduce(
                TemplateCompilationHelper.True(),
                TemplateCompilationHelper::and
            );

        w.variable(
            "jade.lang.acl.MessageTemplate",
            MESSAGE_TEMPLATE_NAME,
            composedMT
        ).writeSonnet(scb);

// generating => Message __receivedMessage = null;
        w.variable("jadescript.core.message.Message", MESSAGE_VAR_NAME, w.Null)
            .writeSonnet(scb);

// generating => if(myAgent!=null) {
// generating =>     __receivedMessage = jadescript.core.message.Message.wrap
// generating =>         (myAgent.receive(__mt));
// generating => }
        w.ifStmnt(w.expr("myAgent!=null"), w.block()
            .addStatement(w.assign(
                MESSAGE_VAR_NAME,
                w.callExpr(
                    "jadescript.core.message.Message.wrap",
                    w.callExpr(
                        "myAgent.receive",
                        w.expr(MESSAGE_TEMPLATE_NAME)
                    )
                )
            ))

        ).writeSonnet(scb);

        MessageType finalMessageType = typeSolver.instantiateMessageType(
            input.__(OnMessageHandler::getPerformative),
            finalContentType,
            /*normalizeToUpperBounds=*/ true
        );

        final StaticState preparedState = prepareBodyState.apply(
            afterWhenExprRetunedTrue);


        final MessageType effectivelyFinalMsg = finalMessageType;
        module.get(ContextManager.class).enterProceduralFeature((
            mod,
            out
        ) -> new OnMessageHandlerContext(
            mod,
            out,
            input.__(OnMessageHandler::getPerformative),
            effectivelyFinalMsg,
            finalContentType
        ));

        StaticState inBody = StaticState.beginningOfOperation(module)
            .copyInnermostContentFrom(preparedState);

        inBody = inBody.enterScope();


        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final PSR<SourceCodeBuilder> bodyPSR =
            compilationHelper.compileBlockToNewSCB(inBody, body);


        final StatementWriter tryCatchWrappedBody =
            encloseInGeneralHandlerTryCatch(bodyPSR.result());


        module.get(ContextManager.class).exit();


//generating => if (__receivedMessage != null) {
//generating =>     [OUTERCLASS].this.__ignoreMessageHandlers = true;
//generating =>
//generating =>    __theAgent().__cleanIgnoredFlagForMessage(__receivedMessage);
//generating =>
//generating =>     this.__eventFired = true;
//generating =>
//generating =>     try {
//generating =>         try {
//generating =>           [...USERCODE...]
//generating =>         } catch (jadescript.core.exception.JadescriptException
//generating =>             __throwable) {
//generating =>             __handleJadescriptException(__throwable);
//generating =>         } catch (java.lang.Throwable __throwable) {
//generating =>            __handleJadescriptException(jadescript.core.exception
//generating =>             .JadescriptException.wrap(__throwable));
//generating =>         }
//generating =>
//generating =>         this.__receivedMessage = null;
//generating =>     } catch (Exception _e) {
//generating =>         _e.printStackTrace();
//generating =>     }
//generating => } else {
//generating =>     this.__eventFired = false;
//generating => }
        w.ifStmnt(
            w.expr(MESSAGE_VAR_NAME + " != null"),
            w.block().addStatement(
                w.assign(SemanticsUtils.getOuterClassThisReference(input) + "."
                    + IGNORE_MSG_HANDLERS_VAR_NAME, w.expr("true"))
            ).addStatement(w.callStmnt(
                    CompilationHelper.compileAgentReference() +
                        ".__cleanIgnoredFlagForMessage",
                    w.expr(MESSAGE_VAR_NAME)
                )
            ).addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("true")
                )
            ).addStatement(
                w.tryCatch(w.block()
                    .addStatement(tryCatchWrappedBody)
                    .addStatement(w.assign(MESSAGE_VAR_NAME, w.expr("null")))
                ).addCatchBranch(
                    "Exception",
                    "_e",
                    w.block().addStatement(
                        w.callStmnt("_e.printStackTrace")
                    )
                )
            )
        ).setElseBranch(w.block()
            .addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("false")
                ))
        ).writeSonnet(scb);

    }


    @NotNull
    private String getNarrowedContentErrorMsg(
        IJadescriptType pattNarrowedContentType,
        IJadescriptType wexpNarrowedBehaviourType,
        IJadescriptType messageContentType
    ) {
        return "Could not compute content type: cannot find common " +
            "subtype of type (inferred from pattern) '"
            + pattNarrowedContentType + "', type (inferred from " +
            "when-expression) '" + wexpNarrowedBehaviourType + "', and" +
            " type (content type of the inferred message type in the " +
            "when-expressions) '" + messageContentType + "'.";
    }


    private JvmGenericType createEventClass(
        Maybe<OnMessageHandler> input,
        OnMessageHandler inputSafe,
        SavedContext savedContext,
        String className
    ) {
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        return jvmTypesBuilder.toClass(
            inputSafe,
            className,
            it -> {
                it.setVisibility(JvmVisibility.PRIVATE);

                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);

                final BuiltinTypeProvider builtins =
                    module.get(BuiltinTypeProvider.class);
                final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

                it.getMembers().add(jvmTypesBuilder.toField(
                    inputSafe,
                    MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    builtins.boolean_().asJvmTypeReference(),
                    itField -> {
                        itField.setVisibility(JvmVisibility.DEFAULT);
                        compilationHelper
                            .createAndSetInitializer(
                                itField,
                                w.False::writeSonnet
                            );
                    }
                ));

                it.getMembers().add(jvmTypesBuilder.toMethod(
                    inputSafe,
                    "run",
                    jvm.typeRef(void.class),
                    itMethod -> compilationHelper
                        .createAndSetBody(itMethod, scb -> {
                            fillRunMethod(
                                input,
                                savedContext,
                                scb
                            );
                        })
                ));
            }
        );
    }


    @Override
    public void validateOnEdit(
        Maybe<OnMessageHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<String> performativeString =
            input.__(OnMessageHandler::getPerformative);
        Maybe<Performative> performative = performativeString
            .__(Performative.performativeByName::get);

        Maybe<WhenExpression> whenBody =
            input.__(OnMessageHandler::getWhenBody);

        final Maybe<OptionalBlock> body =
            input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);
        final Maybe<LValueExpression> pattern = input
            .__(OnMessageHandler::getPattern)
            .__(x -> (LValueExpression) x);


        boolean performativeCheck = module.get(ValidationHelper.class)
            .assertSupportedPerformative(
                performativeString,
                input,
                acceptor
            );


        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        final IJadescriptType contentUpperBound = performative
            .__(typeSolver::getContentBoundForPerformative)
            .orElseGet(() -> builtins.any("Could not resolve " +
                "message type from performative '" + performative + "'."));

        MessageType initialMsgType = typeSolver.instantiateMessageType(
            input.__(OnMessageHandler::getPerformative),
            contentUpperBound,
            /*
             Not normalizing to upper bounds when validating, in
             order to not interfere with the
             type-inferring-from-pattern-match-and-when-expression
             system
            */
            /*normalizeToUpperBounds=*/ false
        );

        module.get(ContextManager.class).enterProceduralFeature(
            (m, o) -> new OnMessageHandlerWhenExpressionContext(
                m, performative, o));

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        IJadescriptType pattNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedMessageType = initialMsgType;


        Function<StaticState, StaticState> prepareBodyState =
            Function.identity();
        StaticState afterPatternDidMatch = beforePattern;

        if (pattern.isPresent() && performativeCheck) {
            final PatternMatchHelper patternMatchHelper =
                module.get(PatternMatchHelper.class);

            PatternMatchInput<LValueExpression> patternMatchInput
                = patternMatchHelper.handlerHeader(
                contentUpperBound,
                pattern,
                some(ExpressionDescriptor.contentOfMessageReference)
            );

            LValueExpressionSemantics lves =
                module.get(LValueExpressionSemantics.class);


            boolean patternMatchingCheck = lves.validatePatternMatch(
                patternMatchInput,
                beforePattern,
                acceptor
            );


            if (patternMatchingCheck == VALID) {
                pattNarrowedContentType = lves.inferPatternType(
                    patternMatchInput,
                    beforePattern
                ).solve(contentUpperBound);

                final StaticState afterPattern = lves.advancePattern(
                    patternMatchInput,
                    beforePattern
                );

                afterPatternDidMatch = lves.assertDidMatch(
                    patternMatchInput,
                    afterPattern
                );

                prepareBodyState = s -> lves.assertDidMatch(
                    patternMatchInput,
                    s
                );
            }


        }


        StaticState afterWhenExprReturnedTrue = afterPatternDidMatch;
        if (whenExpr.isPresent() && performativeCheck) {
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);
            boolean whenExprCheck =
                rves.validate(
                    whenExpr,
                    afterPatternDidMatch,
                    acceptor
                ) && rves.validateUsageAsHandlerCondition(
                    whenExpr,
                    whenExpr,
                    afterPatternDidMatch,
                    acceptor
                );


            if (whenExprCheck == VALID) {
                final StaticState afterWhenExpr = rves.advance(
                    whenExpr,
                    afterPatternDidMatch
                );
                afterWhenExprReturnedTrue = rves.assertReturnedTrue(
                    whenExpr,
                    afterWhenExpr
                );


                wexpNarrowedContentType = afterWhenExprReturnedTrue
                    .inferUpperBound(
                        ExpressionDescriptor.contentOfMessageReference
                    ).findFirst()
                    .orElse(contentUpperBound);

                wexpNarrowedMessageType = afterWhenExprReturnedTrue
                    .inferUpperBound(
                        ExpressionDescriptor.messageReference
                    ).findFirst()
                    .orElse(initialMsgType);

                prepareBodyState = prepareBodyState.andThen(
                    s -> rves.assertReturnedTrue(
                        whenExpr,
                        s
                    )
                );
            }
        }

        module.get(ContextManager.class).exit();

        final IJadescriptType finalContentType;
        if (wexpNarrowedMessageType instanceof BaseMessageType) {
            final IJadescriptType messageBasedContentType =
                ((BaseMessageType) wexpNarrowedMessageType)
                    .getContentType();
            finalContentType = lattice.getGLB(
                getNarrowedContentErrorMsg(
                    pattNarrowedContentType,
                    wexpNarrowedContentType,
                    messageBasedContentType
                ),
                pattNarrowedContentType,
                wexpNarrowedContentType,
                messageBasedContentType
            );
        } else {
            finalContentType = lattice.getGLB(
                pattNarrowedContentType,
                wexpNarrowedContentType,
                TypeHelper.getNarrowedContentErrorMsg(
                    pattNarrowedContentType,
                    wexpNarrowedContentType
                )
            );
        }


        if (performativeCheck
            && (pattern.isPresent() || whenExpr.isPresent())) {
            module.get(ValidationHelper.class).advice(
                finalContentType.isSendable(),
                "UnexpectedContent",
                "Suspicious content type; values of type '"
                    + finalContentType.getFullJadescriptName() +
                    "' cannot be received as part of messages.",
                eitherGet(eitherGet(pattern, whenExpr), input),
                acceptor
            );
        }

        if (pattern.isPresent() || whenExpr.isPresent()) {
            module.get(ValidationHelper.class).advice(
                comparator.compare(contentUpperBound, finalContentType)
                    .is(superTypeOrEqual()),
                "UnexpectedContent",
                "Suspicious content type; Messages with performative '"
                    + performativeString + "' expect contents of type "
                    + contentUpperBound.getFullJadescriptName()
                    + "; type constrained by pattern/when expression: "
                    + finalContentType.getFullJadescriptName(),
                eitherGet(pattern, whenExpr),
                acceptor
            );
        }

        MessageType finalMessageType = typeSolver.instantiateMessageType(
            input.__(OnMessageHandler::getPerformative),
            finalContentType,
            /*normalizeToUpperBounds=*/ true
        );

        final StaticState preparedState = prepareBodyState.apply(
            afterWhenExprReturnedTrue);

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new OnMessageHandlerContext(
                mod,
                out,
                input.__(OnMessageHandler::getPerformative),
                finalMessageType,
                finalContentType
            ));

        StaticState inBody = StaticState.beginningOfOperation(module)
            .copyInnermostContentFrom(preparedState);

        inBody = inBody.enterScope();

        module.get(BlockSemantics.class)
            .validateOptionalBlock(body, inBody, acceptor);

        module.get(ContextManager.class).exit();

    }


    @Override
    public void validateOnSave(
        Maybe<OnMessageHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

    }


}
