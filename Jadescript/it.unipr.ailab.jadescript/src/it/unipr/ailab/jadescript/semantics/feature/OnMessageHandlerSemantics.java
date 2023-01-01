package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.MessageHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.MessageHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.MessageReceivedContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.BaseMessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.AssignmentWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import jade.lang.acl.MessageTemplate;
import jadescript.lang.Performative;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

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
        final String messageTemplateName =
            synthesizeMessageTemplateName(inputSafe);

        JvmGenericType eventClass = createEventClass(
            input,
            inputSafe,
            savedContext,
            eventClassName,
            messageTemplateName
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

    private JvmGenericType createEventClass(
        Maybe<OnMessageHandler> input, OnMessageHandler inputSafe,
        SavedContext savedContext,
        String cn,
        String messageTemplateName
    ) {
        return module.get(JvmTypesBuilder.class).toClass(inputSafe, cn, it -> {
            it.setVisibility(JvmVisibility.PRIVATE);
            addMessageField(inputSafe, it);
            addMessageReceivedBooleanField(inputSafe, it);
            addRunMethod(input, inputSafe, it);

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

            final IJadescriptType contentUpperBound = performative
                .__(typeHelper::getContentBound)
                .orElseGet(() -> typeHelper.ANY);

            BaseMessageType initialMsgType = typeHelper
                .instantiateMessageType(
                    input.__(OnMessageHandler::getPerformative),
                    contentUpperBound,
                    /*normalizeToUpperBounds=*/ true
                );

            module.get(ContextManager.class).enterProceduralFeature(
                (m, o) -> new MessageHandlerWhenExpressionContext(
                    m, performative, o));

            final List<BlockWriterElement> auxiliaryStatements =
                new ArrayList<>();

            String compiledExpression;
            IJadescriptType pattNarrowedContentType = contentUpperBound;
            IJadescriptType wexpNarrowedContentType = contentUpperBound;
            IJadescriptType wexpNarrowedMessageType = initialMsgType;

            final Maybe<LValueExpression> pattern = input
                .__(OnMessageHandler::getPattern)
                .__(x -> (LValueExpression) x);

            String part1 = "";
            if (pattern.isPresent()) {
                PatternMatcher o = module.get(PatternMatchHelper.class)
                    .compileHeaderPatternMatching(
                        contentUpperBound,
                        pattern,
                        auxiliaryStatements::add
                    );
                pattNarrowedContentType =
                    module.get(PatternMatchHelper.class)
                        .inferHandlerHeaderPatternType(
                            pattern,
                            contentUpperBound
                        );
                part1 = o.operationInvocationText(
                    initialMsgType.namespace().getContentProperty()
                        .compileRead(MESSAGE_VAR_NAME)
                );
            }

            String part2 = "";
            if (whenExpr.isPresent()) {
                //TODO we need a transactional (or patch-based, or
                // immutable) evaluation context

                //TODO the problem is that this compile here might
                // declare (correctly) new variables, changing the context
                part2 = module.get(RValueExpressionSemantics.class).compile(
                    whenExpr, ,
                    auxiliaryStatements::add
                );
                // TODO but then the same expression is re-evaluated in
                //  the (already changed from the compile) new context to
                //  compute the KB
                wexpNarrowedContentType =
                    module.get(RValueExpressionSemantics.class)
                        .advance(whenExpr, )
                        .query("content", "message")
                        .orElseGet(() -> FlowTypeInferringTerm.of(
                            typeHelper.ANY
                        ))
                        .getType();
                // TODO  ...and then again
                wexpNarrowedMessageType =
                    module.get(RValueExpressionSemantics.class)
                        .advance(whenExpr, )
                        .query("message")
                        .orElseGet(() -> FlowTypeInferringTerm.of(
                            typeHelper.ANYMESSAGE
                        ))
                        .getType();
            }

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

            //TODO split "context modification logic"
            final List<NamedSymbol> autoDeclaredVars =
                module.get(ContextManager.class).currentContext()
                    .searchAs(
                        NamedSymbol.Searcher.class,
                        s -> s.searchName((Predicate<String>) null, null, null)
                    ).filter(
                        ne -> ne instanceof PatternMatchAutoDeclaredVariable
                    ).collect(Collectors.toList());


            module.get(ContextManager.class).exit();

            //TODO consider using abstract representations of
            // matcher-classes/fields instead of reconverting
            List<JvmDeclaredType> patternMatcherClasses =
                PatternMatchHelper.getPatternMatcherClasses(
                    auxiliaryStatements,
                    input,
                    module
                );

            //TODO consider using abstract representations of
            // matcher-classes/fields instead of reconverting
            List<JvmField> patternMatcherFields =
                PatternMatchHelper.getPatternMatcherFieldDeclarations(
                    auxiliaryStatements,
                    input,
                    module
                );

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


            it.getMembers().addAll(patternMatcherClasses);
            it.getMembers().addAll(patternMatcherFields);




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
                messageTemplateExpressions.add(
                    customMessageTemplateExpression(
                        compiledExpression,
                        finalContentType
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


            //The method returning the generated message template
            addMessageTemplateMethod(
                inputSafe,
                messageTemplateName,
                it,
                composedMT
            );

            // The receive() method of the event
            addReceiveMethod(inputSafe, messageTemplateName, it);
            BaseMessageType finalMessageType =
                typeHelper.instantiateMessageType(
                    input.__(OnMessageHandler::getPerformative),
                    finalContentType,
                    /*normalizeToUpperBounds=*/ true
                );


            prepareBlockWithAutoExtraction(
                body,
                finalContentType,
                finalMessageType,
                autoDeclaredVars
            );

            // The body of the event handler
            addDoBodyMethod(
                input,
                inputSafe,
                savedContext,
                it,
                performativeString,
                body,
                autoDeclaredVars,
                finalContentType
            );


        });
    }

    private boolean addDoBodyMethod(
        Maybe<OnMessageHandler> input,
        OnMessageHandler inputSafe,
        SavedContext savedContext, JvmGenericType it,
        Maybe<String> performativeString,
        Maybe<CodeBlock> body,
        List<NamedSymbol> autoDeclaredVars,
        IJadescriptType finalContentType
    ) {
        return it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
            inputSafe,
            "doBody",
            module.get(TypeHelper.class).typeRef(void.class),
            itMethod -> {
                itMethod.getExceptions().add(
                    module.get(TypeHelper.class).typeRef(Exception.class)
                );

                module.get(ContextManager.class).restore(savedContext);
                module.get(ContextManager.class).enterProceduralFeature((
                        mod,
                        out
                    ) ->
                        new MessageHandlerContext(
                            mod,
                            out,
                            "message",
                            input.__(OnMessageHandler::getPerformative),
                            autoDeclaredVars,
                            mod.get(TypeHelper.class).instantiateMessageType(
                                performativeString,
                                finalContentType,
                                /*normalizeToUpperBounds=*/ true
                            ),
                            finalContentType
                        )
                );

                createAndSetHandlerBody(body, itMethod);

                module.get(ContextManager.class).exit();
            }
        ));
    }

    private boolean addReceiveMethod(
        OnMessageHandler inputSafe,
        String messageTemplateName, JvmGenericType it
    ) {
        return it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
            inputSafe,
            "receive",
            module.get(TypeHelper.class).VOID.asJvmTypeReference(),
            itMethod -> module.get(CompilationHelper.class)
                .createAndSetBody(itMethod, scb -> {
                        final AssignmentWriter receiveAssign = w.assign(
                            MESSAGE_VAR_NAME,
                            w.callExpr(
                                "jadescript.core.message.Message.wrap",
                                w.callExpr(
                                    "myAgent.receive",
                                    w.expr(messageTemplateName + "()")
                                )
                            )
                        );
                        final IfStatementWriter ifMsgIsNotNull = w.ifStmnt(
                            w.expr(MESSAGE_VAR_NAME + " != null"),
                            w.block().addStatement(w.callStmnt(
                                THE_AGENT + "().__cleanIgnoredFlagForMessage",
                                w.expr(MESSAGE_VAR_NAME)
                            ))
                        );
                        w.ifStmnt(w.expr("myAgent!=null"), w.block()
                            .addStatement(receiveAssign)
                            .addStatement(ifMsgIsNotNull)
                        ).writeSonnet(scb);
                    }
                )
        ));
    }

    private void addMessageTemplateMethod(
        OnMessageHandler inputSafe,
        String messageTemplateName,
        JvmGenericType it,
        ExpressionWriter composedMT
    ) {
        it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
            inputSafe,
            messageTemplateName,
            module.get(TypeHelper.class).typeRef(MessageTemplate.class),
            itMethod -> {
                itMethod.setVisibility(JvmVisibility.PRIVATE);
                module.get(CompilationHelper.class).createAndSetBody(
                    itMethod,
                    scb -> w.returnStmnt(composedMT).writeSonnet(scb)
                );
            }
        ));
    }

    private ExpressionWriter customMessageTemplateExpression(
        String compiledExpression,
        IJadescriptType computedContentType
    ) {
        final String contentTypeCompiled = module.get(TypeHelper.class)
            .noGenericsTypeName(computedContentType
                .compileToJavaTypeReference());

        return TemplateCompilationHelper.customMessage(w.block()
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
        );
    }

    private void addRunMethod(
        Maybe<OnMessageHandler> input,
        OnMessageHandler inputSafe, JvmGenericType itClass
    ) {
        itClass.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
            inputSafe,
            "run",
            module.get(TypeHelper.class).typeRef(void.class),
            itMethod -> module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                generateRunMethod(input, scb);
            })
        ));
    }

    private void addMessageReceivedBooleanField(
        OnMessageHandler inputSafe,
        JvmGenericType itClass
    ) {
        itClass.getMembers().add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            MESSAGE_RECEIVED_BOOL_VAR_NAME,
            module.get(TypeHelper.class).typeRef(Boolean.class),
            itField -> itField.setVisibility(JvmVisibility.DEFAULT)
        ));
    }

    private void addMessageField(
        OnMessageHandler inputSafe,
        JvmGenericType itClass
    ) {
        itClass.getMembers().add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            MESSAGE_VAR_NAME,
            module.get(TypeHelper.class).typeRef(jadescript.core.message.Message.class),
            itField -> itField.setVisibility(JvmVisibility.PRIVATE)
        ));
    }


    private void prepareBlockWithAutoExtraction(
        Maybe<CodeBlock> codeBlock,
        IJadescriptType contentType,
        BaseMessageType messageType,
        List<NamedSymbol> autoDeclaredVars
    ) {
        module.get(BlockSemantics.class)
                .addInjectedVariables(codeBlock, autoDeclaredVars);

        module.get(BlockSemantics.class).addInjectedVariable(
            codeBlock,
            MessageReceivedContext.messageContentContextGeneratedReference(
                messageType,
                contentType
            )
        );

        module.get(BlockSemantics.class).addInjectedVariable(
            codeBlock,
            new ContextGeneratedReference(
                MESSAGE_VAR_NAME,
                messageType,
                (__) -> "(" + messageType.compileAsJavaCast() +
                    " " + MESSAGE_VAR_NAME + ")"
            )
        );
    }


    @Override
    public void validateFeature(
        Maybe<OnMessageHandler> input, Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<WhenExpression> whenBody =
            input.__(OnMessageHandler::getWhenBody);

        Maybe<String> performativeString =
            input.__(OnMessageHandler::getPerformative);
        Maybe<Performative> performative = performativeString
            .__(Performative.performativeByName::get);

        final Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);

        boolean performativeCheck = module.get(ValidationHelper.class)
            .assertSupportedPerformative(performativeString, input, acceptor);

        List<NamedSymbol> patternMatchDeclaredVariables = new ArrayList<>();

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
            (m, o) -> new MessageHandlerWhenExpressionContext(
                m, performative, o)); //TODO exit

        IJadescriptType pattNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedMessageType = initialMsgType;

        final Maybe<LValueExpression> pattern = input
            .__(OnMessageHandler::getPattern)
            .__(x -> (LValueExpression) x);

        if (pattern.isPresent()) {
            boolean patternMatchingCheck = module.get(PatternMatchHelper.class)
                .validateHeaderPatternMatching(
                    contentUpperBound,
                    pattern,
                    acceptor
                );
            if (patternMatchingCheck == VALID) {
                pattNarrowedContentType = module.get(PatternMatchHelper.class)
                    .inferHandlerHeaderPatternType(
                        pattern,
                        contentUpperBound
                    );
            }
        }

        if (whenExpr.isPresent()) {
            boolean whenExprCheck = module.get(RValueExpressionSemantics.class)
                .validate(whenExpr, , acceptor);
            whenExprCheck = whenExprCheck &&
                module.get(RValueExpressionSemantics.class)
                    .validateUsageAsHandlerCondition(
                        whenExpr,
                        whenExpr, ,
                        acceptor
                    );
            if (whenExprCheck == VALID) {
                wexpNarrowedContentType =
                    module.get(RValueExpressionSemantics.class)
                        .advance(whenExpr, )
                        .query("content", "message")
                        .orElseGet(() -> FlowTypeInferringTerm.of(
                            typeHelper.ANY
                        ))
                        .getType();
                wexpNarrowedMessageType =
                    module.get(RValueExpressionSemantics.class)
                        .advance(whenExpr, )
                        .query("message")
                        .orElseGet(() -> FlowTypeInferringTerm.of(
                            typeHelper.ANYMESSAGE
                        ))
                        .getType();
            }
        }

        final List<NamedSymbol> autoDeclaredVars =
            module.get(ContextManager.class).currentContext()
                .searchAs(
                    NamedSymbol.Searcher.class,
                    s -> s.searchName((Predicate<String>) null, null,
                        null
                    )
                ).filter(ne -> ne instanceof PatternMatchAutoDeclaredVariable)
                .collect(Collectors.toList());

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


        final boolean sendable = finalContentType.isSendable();
        module.get(ValidationHelper.class).advice(
            sendable,
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

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new MessageHandlerContext(
                mod,
                out,
                "message",
                input.__(OnMessageHandler::getPerformative),
                patternMatchDeclaredVariables,
                finalMessageType,
                finalContentType
            )
        );

        prepareBlockWithAutoExtraction(
            body,
            finalContentType,
            finalMessageType,
            autoDeclaredVars
        );

        module.get(BlockSemantics.class).validate(
            input.__(FeatureWithBody::getBody), acceptor
        );

        module.get(ContextManager.class).exit();

    }

    private void createAndSetHandlerBody(
        Maybe<CodeBlock> body, JvmOperation itMethod
    ) {
        final SavedContext saved = module.get(ContextManager.class).save();
        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
            module.get(ContextManager.class).restore(saved);
            scb.add(encloseInGeneralHandlerTryCatch(module.get(CompilationHelper.class).compileBlockToNewSCB(body)));
        });
    }

    private void generateRunMethod(
        Maybe<? extends EObject> source,
        SourceCodeBuilder scb
    ) {
        w.ifStmnt(
            w.expr(Util.getOuterClassThisReference(source)
                + "." + IGNORE_MSG_HANDLERS_VAR_NAME),
            w.block().addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("false")
                )
            ).addStatement(w.returnStmnt())
        ).writeSonnet(scb);

        w.callStmnt("receive").writeSonnet(scb);

        w.ifStmnt(
            w.expr(MESSAGE_VAR_NAME + " != null"),
            w.block().addStatement(
                w.assign(Util.getOuterClassThisReference(source) + "."
                    + IGNORE_MSG_HANDLERS_VAR_NAME, w.expr("true"))
            ).addStatement(
                w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("true")
                )
            ).addStatement(
                w.tryCatch(
                    w.block()
                        .addStatement(w.callStmnt("doBody"))
                        .addStatement(
                            w.assign(
                                "this." + MESSAGE_VAR_NAME,
                                w.expr("null")
                            ))
                ).addCatchBranch(
                    "Exception",
                    "_e",
                    w.block().addStatement(w.callStmnt("_e" +
                        ".printStackTrace"))
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

}
