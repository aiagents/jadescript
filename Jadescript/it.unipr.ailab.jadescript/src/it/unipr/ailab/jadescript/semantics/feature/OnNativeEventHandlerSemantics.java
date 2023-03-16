package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnNativeEventHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnNativeEventHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TemplateCompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.some;

public class OnNativeEventHandlerSemantics
    extends DeclarationMemberSemantics<OnNativeEventHandler> {

    public OnNativeEventHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnNativeEventHandler> input,
        Maybe<FeatureContainer> featureContainer,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {

        if (input.isNothing()) {
            return;
        }
        OnNativeEventHandler inputSafe = input.toNullable();

        final SavedContext savedContext =
            module.get(ContextManager.class).save();


        final String eventClassName = synthesizeBehaviourEventClassName(
            inputSafe);

        JvmGenericType eventClass = createEventClass(
            input,
            inputSafe,
            savedContext,
            eventClassName
        );

        members.add(eventClass);


        addEventField(members, inputSafe, eventClass);

    }


    private void addEventField(
        EList<JvmMember> members,
        OnNativeEventHandler inputSafe,
        JvmGenericType eventClass
    ) {
        members.add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            synthesizeEventFieldName(inputSafe),
            module.get(TypeHelper.class).typeRef(eventClass), it -> {
                it.setVisibility(JvmVisibility.PRIVATE);
                module.get(JvmTypesBuilder.class).setInitializer(
                    it,
                    new StringConcatenationClient() {
                        @Override
                        protected void appendTo(
                            TargetStringConcatenation target
                        ) {
                            target.append(" new ");
                            target.append(module.get(TypeHelper.class)
                                .typeRef(eventClass));
                            target.append("()");
                        }
                    }
                );
            }
        ));
    }


    private JvmGenericType createEventClass(
        Maybe<OnNativeEventHandler> input,
        OnNativeEventHandler inputSafe,
        SavedContext savedContext,
        String eventClassName
    ) {
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        return jvmTypesBuilder.toClass(inputSafe,
            eventClassName, it -> {
                it.setVisibility(JvmVisibility.PRIVATE);

                final CompilationHelper compilationHelper = module.get(
                    CompilationHelper.class);
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                it.getMembers().add(jvmTypesBuilder.toField(
                    inputSafe,
                    MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    typeHelper.typeRef(Boolean.class),
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
                    typeHelper.typeRef(void.class),
                    itMethod -> compilationHelper.createAndSetBody(
                        itMethod,
                        scb -> {
                            fillRunMethod(input, savedContext, scb);
                        }
                    )
                ));
            }
        );
    }


    private void fillRunMethod(
        Maybe<OnNativeEventHandler> input,
        SavedContext savedContext,
        SourceCodeBuilder scb
    ) {
//generating => if ([OUTERCLASS].this.__ignoreMessageHandlers) {
//generating =>     this.__eventFired = false;
//generating =>     return;
//generating => }
        w.ifStmnt(
            w.expr(Util.getOuterClassThisReference(input) +
                "." + IGNORE_MSG_HANDLERS_VAR_NAME),
            w.block().addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("false")
                )
            ).addStatement(w.returnStmnt())
        ).writeSonnet(scb);


//generating => [... message template auxiliary statements...]
//generating => MessageTemplate _mt = [...]

        final Maybe<WhenExpression> whenBody =
            input.__(OnNativeEventHandler::getWhenBody);

        final Maybe<CodeBlock> body =
            input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);

        module.get(ContextManager.class).restore(savedContext);
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        module.get(ContextManager.class).enterProceduralFeature(
            OnNativeEventHandlerWhenExpressionContext::new
        );

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        final IJadescriptType contentUpperBound = typeHelper.PROPOSITION;

        IJadescriptType pattNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedContentType = contentUpperBound;

        final Maybe<LValueExpression> pattern = input
            .__(OnNativeEventHandler::getPattern)
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
                some(ExpressionDescriptor.nativeEventReference)
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

            part1 = matcher.rootInvocationText(NATIVE_EVENT_CONTENT_VAR_NAME);
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
                    ed -> ed.equals(ExpressionDescriptor.nativeEventReference),
                    null
                ).findFirst()
                .orElse(contentUpperBound);

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


        final IJadescriptType finalContentType = typeHelper.getGLB(
            pattNarrowedContentType,
            wexpNarrowedContentType
        );

        //Building the message template
        List<ExpressionWriter> templateExpressions = new ArrayList<>();

        // add "is a native event" constraint
        templateExpressions.add(TemplateCompilationHelper.isNative());

        if (input.__(OnNativeEventHandler::isStale).extract(nullAsFalse)) {
            // add staleness constraint
            templateExpressions.add(TemplateCompilationHelper.isStale());
        }


        final BlockWriter tryBlock = w.block();
        tryBlock.addStatement(w.ifStmnt(
            w.expr("!(((jadescript.core.nativeevent.NativeEvent) " +
                CompilationHelper.compileAgentReference() +
                ".getContentManager()" +
                ".extractContent(" + NATIVE_EVENT_VAR_NAME + "))" +
                ".getContent() instanceof " +
                module.get(TypeHelper.class).noGenericsTypeName(
                    finalContentType.compileToJavaTypeReference()
                ) + ")"),
            w.block().addStatement(w.returnStmnt(w.expr("false")))
        ));

        final VariableDeclarationWriter declareNativeEventContentVariable =
            w.variable(
                finalContentType.compileToJavaTypeReference(),
                NATIVE_EVENT_CONTENT_VAR_NAME,
                w.expr(finalContentType.compileAsJavaCast() +
                    "((jadescript.core.nativeevent.NativeEvent)" +
                    CompilationHelper.compileAgentReference() +
                    ".getContentManager()" +
                    ".extractContent(" + NATIVE_EVENT_VAR_NAME + "))" +
                    ".getContent()"
                )
            );

        templateExpressions.add(TemplateCompilationHelper.customNativeEvent(
            w.block().addStatement(
                w.tryCatch(tryBlock
                    .addStatements(declareNativeEventContentVariable)
                    .addStatement(w.returnStmnt(w.expr(compiledExpression)))
                ).addCatchBranch("java.lang.Throwable", "_e", w.block()
                    .addStatement(w.callStmnt("_e.printStackTrace"))
                    .addStatement(w.returnStmnt(w.expr("false")))
                )
            )
        ));

        // Put all constraints in a
        // MessageTemplate.and(..., MessageTemplate.and(..., ...))
        // chain.
        final ExpressionWriter composedMT =
            templateExpressions.stream().reduce(
                TemplateCompilationHelper.True(),
                TemplateCompilationHelper::and
            );

        w.variable(
            "jade.lang.acl.MessageTemplate",
            MESSAGE_TEMPLATE_NAME,
            composedMT
        ).writeSonnet(scb);

//generating => Message __receivedNativeEvent = null;
        w.variable(
            "jadescript.core.message.Message",
            NATIVE_EVENT_VAR_NAME,
            w.Null
        ).writeSonnet(scb);

//generating => if(myAgent!=null) {
//generating =>     __receivedNativeEvent = jadescript.core.message.Message.wrap
//generating =>         (myAgent.receive(__mt));
//generating => }
        w.ifStmnt(
            w.expr("myAgent!=null"),
            w.block().addStatement(
                w.assign(
                    NATIVE_EVENT_VAR_NAME,
                    w.callExpr(
                        "jadescript.core.message.Message.wrap",
                        w.callExpr(
                            "myAgent.receive",
                            w.expr(MESSAGE_TEMPLATE_NAME)
                        )
                    )
                )
            )
        ).writeSonnet(scb);


        StaticState preparedState = prepareBodyState.apply(
            afterWhenExprRetunedTrue
        );

        module.get(ContextManager.class).enterProceduralFeature(
            (mod, out) -> new OnNativeEventHandlerContext(
                mod,
                out,
                finalContentType
            )
        );

        StaticState inBody = StaticState.beginningOfOperation(module)
            .copyInnermostContentFrom(preparedState);

        inBody = inBody.enterScope();

        final PSR<SourceCodeBuilder> bodyPSR =
            module.get(CompilationHelper.class).compileBlockToNewSCB(
                inBody,
                body
            );

        final StatementWriter tryCatchWrappedBody =
            encloseInGeneralHandlerTryCatch(bodyPSR.result());


        module.get(ContextManager.class).exit();

//generating => if (__receivedNativeEvent != null) {
//generating =>     [OUTERCLASS].this.__ignoreMessageHandlers = true;
//generating =>
//generating =>    __theAgent().__cleanIgnoredFlagForMessage(
//generating =>       __receivedNativeEvent);
//generating =>     this.__eventFired = true;
//generating =>
//generating =>     try {
//generating =>         [CONTENT_TYPE] __nativeEvent = ([CONTENT_TYPE])
//generating =>             ((jadescript.core.nativeevent.NativeEvent)
//generating =>                 __theAgent()
//generating =>                 .getContentManager()
//generating =>                 .extractContent(__receivedNativeEvent)
//generating =>             ).getContent();
//generating =>         try {
//generating =>
//generating =>             [...USERCODE...]
//generating =>         } catch (jadescript.core.exception.JadescriptException
//generating =>             __throwable) {
//generating =>             __handleJadescriptException(__throwable);
//generating =>         } catch (java.lang.Throwable __throwable) {
//generating =>            __handleJadescriptException(jadescript.core.exception
//generating =>             .JadescriptException.wrap(__throwable));
//generating =>         }
//generating =>
//generating =>         __receivedNativeEvent = null;
//generating =>     } catch (Exception _e) {
//generating =>         _e.printStackTrace();
//generating =>     }
//generating => } else {
//generating =>     this.__eventFired = false;
//generating => }

        w.ifStmnt(
            w.expr(NATIVE_EVENT_VAR_NAME + " != null"),
            w.block()
                .addStatement(w.assign(
                    Util.getOuterClassThisReference(input) + "."
                        + IGNORE_MSG_HANDLERS_VAR_NAME,
                    w.expr("true")
                ))
                .addStatement(w.callStmnt(
                    CompilationHelper.compileAgentReference() +
                        ".__cleanIgnoredFlagForMessage",
                    w.expr(NATIVE_EVENT_VAR_NAME)
                ))
                .addStatement(w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("true")
                ))
                .addStatement(w.tryCatch(w.block()
                        .addStatement(declareNativeEventContentVariable)
                        .addStatement(tryCatchWrappedBody)
                        .addStatement(
                            w.assign(NATIVE_EVENT_VAR_NAME, w.expr("null"))
                        )
                    ).addCatchBranch(
                        "Exception",
                        "_e",
                        w.block().addStatement(
                            w.callStmnt("_e.printStackTrace"))
                    )
                )
        ).setElseBranch(w.block()
            .addStatement(w.assign(
                "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                w.expr("false")
            ))
        ).writeSonnet(scb);
    }


    @Override
    public void validateOnEdit(
        Maybe<OnNativeEventHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<WhenExpression> whenBody =
            input.__(OnNativeEventHandler::getWhenBody);

        final Maybe<CodeBlock> body =
            input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);
        final Maybe<LValueExpression> pattern = input
            .__(OnNativeEventHandler::getPattern)
            .__(x -> (LValueExpression) x);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final IJadescriptType contentUpperBound = typeHelper.PROPOSITION;

        module.get(ContextManager.class).enterProceduralFeature(
            OnNativeEventHandlerWhenExpressionContext::new
        );

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        IJadescriptType pattNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedContentType = contentUpperBound;


        Function<StaticState, StaticState> prepareBodyState =
            Function.identity();
        StaticState afterPatternDidMatch = beforePattern;

        if (pattern.isPresent()) {
            final PatternMatchHelper patternMatchHelper =
                module.get(PatternMatchHelper.class);

            PatternMatchInput<LValueExpression> patternMatchInput
                = patternMatchHelper.handlerHeader(
                contentUpperBound,
                pattern,
                some(ExpressionDescriptor.nativeEventReference)
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
        if (whenExpr.isPresent()) {
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
                        ed -> ed.equals(
                            ExpressionDescriptor.nativeEventReference
                        ),
                        null
                    ).findFirst()
                    .orElse(contentUpperBound);

                prepareBodyState = prepareBodyState.andThen(
                    s -> rves.assertReturnedTrue(
                        whenExpr,
                        s
                    )
                );
            }
        }

        module.get(ContextManager.class).exit();

        final IJadescriptType finalContentType = typeHelper.getGLB(
            pattNarrowedContentType,
            wexpNarrowedContentType
        );

        StaticState preparedState = prepareBodyState.apply(
            afterWhenExprReturnedTrue);
        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new OnNativeEventHandlerContext(
                mod,
                out,
                finalContentType
            )
        );


        StaticState inBody = StaticState.beginningOfOperation(module)
            .copyInnermostContentFrom(preparedState);

        inBody = inBody.enterScope();
        module.get(BlockSemantics.class).validate(
            body,
            inBody,
            acceptor
        );


        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateOnSave(
        Maybe<OnNativeEventHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

    }

}
