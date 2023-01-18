package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.MessageReceivedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnMessageHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnMessageHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.BaseMessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jadescript.lang.Performative;
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

import static it.unipr.ailab.maybe.Maybe.eitherGet;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 26/10/2018.
 */
@SuppressWarnings("restriction")
@Singleton
public class OnMessageHandlerSemantics
    extends FeatureSemantics<OnMessageHandler> {

    public OnMessageHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnMessageHandler> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared
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

        addEventField(members, inputSafe, eventClass);
    }


    private void addEventField(
        EList<JvmMember> members,
        OnMessageHandler inputSafe,
        JvmGenericType eventClass
    ) {
        members.add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            synthesizeEventVariableName(inputSafe),
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


    private void fillRunMethod(
        Maybe<OnMessageHandler> input,
        SavedContext savedContext,
        SourceCodeBuilder scb
    ) {
//        if ([OUTERCLASS].this.__ignoreMessageHandlers) {
//            this.__eventFired = false;
//            return;
//        }
        w.ifStmnt(
            w.expr(Util.getOuterClassThisReference(input)
                + "." + IGNORE_MSG_HANDLERS_VAR_NAME),
            w.block().addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("false")
                )
            ).addStatement(w.returnStmnt())
        ).writeSonnet(scb);


//        Message __receivedMessage = null;
        w.variable("jadescript.core.message.Message", MESSAGE_VAR_NAME, w.Null);

//        [... message template auxiliary statements...]
//        MessageTemplate _mt = [...]
        final Maybe<String> performativeString =
            input.__(OnMessageHandler::getPerformative);
        Maybe<Performative> performative = performativeString
            .__(Performative.performativeByName::get);

        final Maybe<WhenExpression> whenBody =
            input.__(OnMessageHandler::getWhenBody);
        final Maybe<Pattern> contentPattern =
            input.__(OnMessageHandler::getPattern);

        final Maybe<CodeBlock> body =
            input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);


        module.get(ContextManager.class).restore(savedContext);

        final TypeHelper typeHelper = module.get(TypeHelper.class);


        module.get(ContextManager.class).enterProceduralFeature(
            (m, o) -> new OnMessageHandlerWhenExpressionContext(
                m, performative, o));

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        final IJadescriptType contentUpperBound = performative
            .__(typeHelper::getContentBound)
            .orElseGet(() -> typeHelper.ANY);

        BaseMessageType initialMsgType = typeHelper
            .instantiateMessageType(
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
                pattern
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

            matcher.getWriters().forEach(patternMatchClass::addMember);

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

            part1 = matcher.operationInvocationText(
                initialMsgType.namespace().getContentProperty()
                    .compileRead(MESSAGE_VAR_NAME)
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
                    ed -> ed.equals(
                        new ExpressionDescriptor.PropertyChain(
                            "content", "message"
                        )
                    ),
                    null
                ).findFirst()
                .orElse(contentUpperBound);

            wexpNarrowedMessageType = afterWhenExprRetunedTrue.inferUpperBound(
                    ed -> ed.equals(
                        new ExpressionDescriptor.PropertyChain(
                            "message"
                        )
                    ),
                    null
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
        if (!part1.isBlank() && !part2.isBlank()) { // Both are
            // present...
            // ...infix &&
            compiledExpression = "(" + part1 + ") && (" + part2 + ")";
        } else if (part1.isBlank() && part2.isBlank()) { // Both are
            // absent...
            // ... use true
            compiledExpression = "true";
        } else {
            // ... otherwise return the one present
            compiledExpression = part1 + part2;
        }

        module.get(ContextManager.class).exit();

        final IJadescriptType finalContentType;
        if (wexpNarrowedMessageType instanceof BaseMessageType) {
            finalContentType = typeHelper.getGLB(
                pattNarrowedContentType,
                wexpNarrowedContentType,
                ((BaseMessageType) wexpNarrowedMessageType)
                    .getContentType()
            );
        } else {
            finalContentType = typeHelper.getGLB(
                pattNarrowedContentType,
                wexpNarrowedContentType
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

        // add "Not a percept" constraint
        messageTemplateExpressions.add(
            TemplateCompilationHelper.notPercept()
        );

        if (input.__(OnMessageHandler::isStale).extract(nullAsFalse)) {
            // add staleness constraint
            messageTemplateExpressions.add(
                TemplateCompilationHelper.isStale()
            );
        }


        // if there is a when-exprssion or a pattern,then add
        // the corresponding constraint
        if (whenBody.isPresent() || contentPattern.isPresent()) {
            final String contentTypeCompiled = module.get(TypeHelper.class)
                .noGenericsTypeName(
                    finalContentType.compileToJavaTypeReference()
                );

            messageTemplateExpressions.add(
                TemplateCompilationHelper.customMessage(w.block()
                    .addStatement(w.ifStmnt(
                        w.expr("!jadescript.lang.acl.ContentMessageTemplate" +
                            ".MatchClass(" +
                            THE_AGENT + "().getContentManager(), " +
                            contentTypeCompiled + ".class" +
                            ").match(" + MESSAGE_VAR_NAME + ")"),
                        w.block().addStatement(w.returnStmnt(w.expr("false")))
                    )).addStatement(w.tryCatch(w.block()
                        .addStatement(w.returnStmnt(w.expr(compiledExpression)))
                    ).addCatchBranch("java.lang.Throwable", "_e", w.block()
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


//        if(myAgent!=null) {
//            __receivedMessage = jadescript.core.message.Message.wrap
//                (myAgent.receive(__mt));
//        }
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

        BaseMessageType finalMessageType =
            typeHelper.instantiateMessageType(
                input.__(OnMessageHandler::getPerformative),
                finalContentType,
                /*normalizeToUpperBounds=*/ true
            );


        StaticState inBody = prepareBodyState.apply(afterWhenExprRetunedTrue)
            .assertNamedSymbol(
                MessageReceivedContext.messageContentContextGeneratedReference(
                    finalMessageType,
                    finalContentType
                )
            ).assertNamedSymbol(
                MessageReceivedContext.messageContextGeneratedReference(
                    finalMessageType
                )
            );


        module.get(ContextManager.class).enterProceduralFeature((
            mod,
            out
        ) -> new OnMessageHandlerContext(
            mod,
            out,
            "message",
            input.__(OnMessageHandler::getPerformative),
            finalMessageType,
            finalContentType
        ));

        inBody = inBody.enterScope();
        final PSR<SourceCodeBuilder> bodyPSR =
            module.get(CompilationHelper.class).compileBlockToNewSCB(
                inBody,
                body
            );


        final StatementWriter userBody = encloseInGeneralHandlerTryCatch(
            bodyPSR.result()
        );


        module.get(ContextManager.class).exit();


//        if (__receivedMessage != null) {
//            [OUTERCLASS].this.__ignoreMessageHandlers = true;
//
//            __theAgent().__cleanIgnoredFlagForMessage(__receivedMessage);
//
//            this.__eventFired = true;
//
//            try {

//                try {

//                  [...USERCODE...]

//                } catch (jadescript.core.exception.JadescriptException
//                __throwable) {
//                    __handleJadescriptException(__throwable);
//                } catch (java.lang.Throwable __throwable) {
//                    __handleJadescriptException(jadescript.core.exception
//                    .JadescriptException.wrap(
//                        __throwable));
//                }

//
//                this.__receivedMessage = null;
//            } catch (Exception _e) {
//                _e.printStackTrace();
//            }
//        } else {
//            this.__eventFired = false;
//        }
        w.ifStmnt(
            w.expr(MESSAGE_VAR_NAME + " != null"),
            w.block().addStatement(
                w.assign(Util.getOuterClassThisReference(input) + "."
                    + IGNORE_MSG_HANDLERS_VAR_NAME, w.expr("true"))
            ).addStatement(w.callStmnt(
                    THE_AGENT + "().__cleanIgnoredFlagForMessage",
                    w.expr(MESSAGE_VAR_NAME)
                )
            ).addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("true")
                )
            ).addStatement(
                w.tryCatch(w.block()
                    .addStatement(userBody)
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

                final TypeHelper typeHelper = module.get(TypeHelper.class);

                it.getMembers().add(jvmTypesBuilder.toField(
                    inputSafe,
                    MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    typeHelper.BOOLEAN.asJvmTypeReference(),
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
    public void validateFeature(
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

        final Maybe<CodeBlock> body =
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


        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final IJadescriptType contentUpperBound = performative
            .__(typeHelper::getContentBound)
            .orElseGet(() -> typeHelper.ANY);

        BaseMessageType initialMsgType = typeHelper
            .instantiateMessageType(
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
                pattern
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
                        ed -> ed.equals(
                            new ExpressionDescriptor.PropertyChain(
                                "content", "message"
                            )
                        ),
                        null
                    ).findFirst()
                    .orElse(contentUpperBound);

                wexpNarrowedMessageType = afterWhenExprReturnedTrue
                    .inferUpperBound(
                        ed -> ed.equals(
                            new ExpressionDescriptor.PropertyChain(
                                "message"
                            )
                        ),
                        null
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
            finalContentType = typeHelper.getGLB(
                pattNarrowedContentType,
                wexpNarrowedContentType,
                ((BaseMessageType) wexpNarrowedMessageType)
                    .getContentType()
            );
        } else {
            finalContentType = typeHelper.getGLB(
                pattNarrowedContentType,
                wexpNarrowedContentType
            );
        }


        module.get(ValidationHelper.class).advice(
            finalContentType.isSendable(),
            "UnexpectedContent",
            "Suspicious content type; values of type '"
                + finalContentType.getJadescriptName() +
                "' cannot be received as part of messages.",
            eitherGet(eitherGet(pattern, whenExpr), input),
            acceptor
        );

        if (pattern.isPresent() || whenExpr.isPresent()) {
            module.get(ValidationHelper.class).advice(
                contentUpperBound.isAssignableFrom(finalContentType),
                "UnexpectedContent",
                "Suspicious content type; Messages with performative '"
                    + performativeString + "' expect contents of type "
                    + contentUpperBound.getJadescriptName()
                    + "; type constrained by pattern/when expression: "
                    + finalContentType.getJadescriptName(),
                eitherGet(pattern, whenExpr),
                acceptor
            );
        }

        BaseMessageType finalMessageType = typeHelper.instantiateMessageType(
            input.__(OnMessageHandler::getPerformative),
            finalContentType,
            /*normalizeToUpperBounds=*/ true
        );

        //TODO this overwrites the named symbols;
        // however, it would be useful to have a "refine compilation" symbol
        StaticState inBody = prepareBodyState.apply(afterWhenExprReturnedTrue)
            .assertNamedSymbol(
                MessageReceivedContext.messageContentContextGeneratedReference(
                    finalMessageType,
                    finalContentType
                )
            ).assertNamedSymbol(
                MessageReceivedContext.messageContextGeneratedReference(
                    finalMessageType
                )
            );

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new OnMessageHandlerContext(
            mod,
            out,
            "message",
            input.__(OnMessageHandler::getPerformative),
            finalMessageType,
            finalContentType
        ));

        inBody = inBody.enterScope();

        module.get(BlockSemantics.class).validate(
            body,
            inBody,
            acceptor
        );

        module.get(ContextManager.class).exit();

    }


}
