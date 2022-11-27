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
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TemplateCompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.BaseMessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.PatternMatchRequest;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 26/10/2018.
 */
@SuppressWarnings("restriction")
@Singleton
public class OnMessageHandlerSemantics extends FeatureSemantics<OnMessageHandler> {

    public OnMessageHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
            Maybe<OnMessageHandler> input, Maybe<FeatureContainer> container, EList<JvmMember> members, JvmDeclaredType beingDeclared
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(inputSafe -> {
            JvmGenericType eventClass = module.get(JvmTypesBuilder.class).toClass(
                    inputSafe,
                    synthesizeBehaviourEventClassName(inputSafe),
                    it -> {
                        it.setVisibility(JvmVisibility.PRIVATE);
                        it.getMembers().add(module.get(JvmTypesBuilder.class).toField(
                                inputSafe,
                                MESSAGE_VAR_NAME,
                                module.get(TypeHelper.class).typeRef(jadescript.core.message.Message.class),
                                itField -> itField.setVisibility(JvmVisibility.PRIVATE)
                        ));
                        it.getMembers().add(module.get(JvmTypesBuilder.class).toField(
                                inputSafe,
                                MESSAGE_RECEIVED_BOOL_VAR_NAME,
                                module.get(TypeHelper.class).typeRef(Boolean.class),
                                itField -> itField.setVisibility(JvmVisibility.DEFAULT)
                        ));
                        it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                                inputSafe,
                                "run",
                                module.get(TypeHelper.class).typeRef(void.class),
                                itMethod -> module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                    generateRunMethod(input, scb);
                                })
                        ));

                        Maybe<WhenExpression> whenBodyX = input.__(OnMessageHandler::getWhenBody);
                        Maybe<Pattern> contentPattern = input.__(OnMessageHandler::getPattern);
                        Maybe<String> performative = input.__(OnMessageHandler::getPerformative);
                        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);


                        String messageTemplateName = synthesizeMessageTemplateName(inputSafe);

                        PatternMatchData pmData = generatePatternMatchData(
                                input,
                                of(savedContext),
                                input,
                                false
                        );

                        IJadescriptType contentType = pmData.getInferredContentType();

                        for (JvmDeclaredType patternMatchClass : pmData.getPatternMatchClasses()) {
                            it.getMembers().add(patternMatchClass);
                        }
                        for (JvmField patternMatchField : pmData.getPatternMatchFields()) {
                            it.getMembers().add(patternMatchField);
                        }
                        module.get(BlockSemantics.class).addInjectedVariables(body, pmData.getAutoDeclaredVariables());


                        List<ExpressionWriter> messageTemplateExpressions = new ArrayList<>();
                        // add performative constraint (if there is one)
                        performative.safeDo(performativeSafe -> {
                            messageTemplateExpressions.add(TemplateCompilationHelper.performative(performativeSafe));
                        });

                        // add "Not a percept" constraint
                        messageTemplateExpressions.add(TemplateCompilationHelper.notPercept());

                        // add staleness constraint
                        if (input.__(OnMessageHandler::isStale).extract(nullAsFalse)) {
                            messageTemplateExpressions.add(TemplateCompilationHelper.isStale());
                        }

                        IJadescriptType computedContentType = inferContentType(input);

                        // if there is a when-exprssion or a pattern, then add the corresponding constraint
                        if (whenBodyX.isPresent() || contentPattern.isPresent()) {

                            messageTemplateExpressions.add(TemplateCompilationHelper.customMessage(
                                    w.block().addStatement(
                                            w.ifStmnt(
                                                    w.expr("!jadescript.lang.acl.ContentMessageTemplate.MatchClass(" +
                                                            THE_AGENT + "().getContentManager(), " +
                                                            module.get(TypeHelper.class).noGenericsTypeName(
                                                                    computedContentType.compileToJavaTypeReference()
                                                            ) + ".class).match(" + MESSAGE_VAR_NAME + ")"),
                                                    w.block()
                                                            .addStatement(w.returnStmnt(w.expr("false")))
                                            )
                                    ).addStatement(
                                            w.tryCatch(w.block()
                                                    .addStatement(w.returnStmnt(w.expr(pmData.getCompiledExpression())))
                                            ).addCatchBranch("java.lang.Throwable", "_e", w.block()
                                                    .addStatement(w.callStmnt("_e.printStackTrace"))
                                                    .addStatement(w.returnStmnt(w.expr("false"))))
                                    )
                            ));
                        }

                        // Put all constraints in a MessageTemplate.and(..., MessageTemplate.and(..., ...)) chain.
                        final ExpressionWriter composedMT = messageTemplateExpressions.stream().reduce(
                                TemplateCompilationHelper.True(),
                                TemplateCompilationHelper::and
                        );


                        it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                                inputSafe,
                                messageTemplateName,
                                module.get(TypeHelper.class).typeRef(MessageTemplate.class),
                                itMethod -> {
                                    itMethod.setVisibility(JvmVisibility.PRIVATE);
                                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> w.returnStmnt(composedMT).writeSonnet(scb));
                                }
                        ));


                        it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                                inputSafe,
                                "receive",
                                module.get(TypeHelper.class).typeRef(void.class),
                                itMethod -> module.get(CompilationHelper.class)
                                        .createAndSetBody(
                                                itMethod,
                                                scb2 -> w.ifStmnt(
                                                        w.expr("myAgent!=null"),
                                                        w.block().addStatement(
                                                                w.assign(
                                                                        MESSAGE_VAR_NAME,
                                                                        w.callExpr(
                                                                                "jadescript.core.message.Message.wrap",
                                                                                w.callExpr(
                                                                                        "myAgent.receive",
                                                                                        w.expr(messageTemplateName + "()")
                                                                                )
                                                                        )
                                                                )
                                                        ).addStatement(
                                                                w.ifStmnt(
                                                                        w.expr(MESSAGE_VAR_NAME + " != null"),
                                                                        w.block().addStatement(
                                                                                w.callStmnt(
                                                                                        THE_AGENT + "().__cleanIgnoredFlagForMessage",
                                                                                        w.expr(MESSAGE_VAR_NAME)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                ).writeSonnet(scb2)
                                        )
                        ));


                        prepareBlockWithAutoExtraction(input, body, contentType);


                        it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                                inputSafe,
                                "doBody",
                                module.get(TypeHelper.class).typeRef(void.class),
                                itMethod -> {
                                    itMethod.getExceptions().add(module.get(TypeHelper.class).typeRef(Exception.class));

                                    module.get(ContextManager.class).restore(savedContext);
                                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                            new MessageHandlerContext(
                                                    mod,
                                                    out,
                                                    "message",
                                                    input.__(OnMessageHandler::getPerformative),
                                                    pmData.getAutoDeclaredVariables(),
                                                    mod.get(TypeHelper.class).instantiateMessageType(
                                                            performative,
                                                            contentType,
                                                            /*normalizeToUpperBounds=*/ true
                                                    ),
                                                    contentType
                                            )
                                    );

                                    createAndSetHandlerBody(body, itMethod);

                                    module.get(ContextManager.class).exit();
                                }
                        ));


                    }
            );

            members.add(eventClass);
            members.add(module.get(JvmTypesBuilder.class).toField(
                    inputSafe,
                    synthesizeEventVariableName(inputSafe),
                    module.get(TypeHelper.class).typeRef(eventClass), it -> {
                        it.setVisibility(JvmVisibility.PRIVATE);
                        module.get(JvmTypesBuilder.class).setInitializer(it, new StringConcatenationClient() {
                            @Override
                            protected void appendTo(TargetStringConcatenation target) {
                                target.append(" new ");
                                target.append(module.get(TypeHelper.class).typeRef(eventClass));
                                target.append("()");
                            }
                        });
                    }
            ));


        });

    }


    private void prepareBlockWithAutoExtraction(
            Maybe<OnMessageHandler> input, Maybe<CodeBlock> codeBlock, IJadescriptType contentType
    ) {


        BaseMessageType messageType = module.get(TypeHelper.class).instantiateMessageType(
                input.__(OnMessageHandler::getPerformative),
                contentType,
                /*normalizeToUpperBounds=*/ true
        );

        module.get(BlockSemantics.class).addInjectedVariable(
                codeBlock,
                MessageReceivedContext.messageContentContextGeneratedReference(messageType, contentType)
        );

        module.get(BlockSemantics.class).addInjectedVariable(
                codeBlock,
                new ContextGeneratedReference(MESSAGE_VAR_NAME, messageType,
                        (__) -> "(" + messageType.compileAsJavaCast() + " " + MESSAGE_VAR_NAME + ")"
                )
        );
    }


    @Override
    public void validateFeature(
            Maybe<OnMessageHandler> input, Maybe<FeatureContainer> container, ValidationMessageAcceptor acceptor
    ) {

        IJadescriptType contentType = module.get(TypeHelper.class).ANY;

        Maybe<WhenExpression> whenBodyX = input.__(OnMessageHandler::getWhenBody);
        Maybe<Pattern> contentPattern = input.__(OnMessageHandler::getPattern);
        Maybe<String> performative = input.__(OnMessageHandler::getPerformative);


        List<NamedSymbol> patternMatchDeclaredVariables = new ArrayList<>();

        if (whenBodyX.isPresent() || contentPattern.isPresent()) {

            contentType = inferContentType(input);

            patternMatchDeclaredVariables.addAll(
                    generatePatternMatchData(input, nothing(), input, true).getAutoDeclaredVariables()
            );


            final Maybe<RValueExpression> expr = whenBodyX.__(WhenExpression::getExpr);
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(RValueExpressionSemantics.class).validateUsageAsHandlerCondition(expr, expr, interceptAcceptor);
            if (!interceptAcceptor.thereAreErrors()) {


                IJadescriptType computedContentType = inferContentType(input);

                Maybe<PatternMatchRequest> patternMatchRequest;
                patternMatchRequest = generatePatternMatchRequest(input);

                if (expr.isPresent() || patternMatchRequest.isPresent()) {


                    module.get(ValidationHelper.class).assertSupportedPerformative(performative, input, acceptor);

                    if (contentPattern.isPresent()) {
                        //The type of the content has to be "sendable" (i.e., should not contain Agents, Behaviours...)
                        final boolean sendable = computedContentType.isSendable();
                        module.get(ValidationHelper.class).advice(
                                sendable,
                                "UnexpectedContent",
                                "Suspicious content type; values of type '" + computedContentType.getJadescriptName() +
                                        "' cannot be sent as part of messages.",
                                contentPattern,
                                acceptor
                        );

                        if (performative.isPresent()) {
                            final String performativeSafe = performative.toNullable();
                            final IJadescriptType contentBound = module.get(TypeHelper.class).getContentBound(
                                    Performative.performativeByName.get(performativeSafe)
                            );

                            //The type of the content has to be within the bounds of the performative expected types
                            final boolean assignableFrom = contentBound.isAssignableFrom(computedContentType);
                            module.get(ValidationHelper.class).advice(
                                    assignableFrom,
                                    "UnexpectedContent",
                                    "Suspicious content type; Messages with performative '" + performativeSafe
                                            + "' expect contents of type "
                                            + contentBound.getJadescriptName()
                                            + "; type constrained by pattern/when expression: "
                                            + computedContentType.getJadescriptName(),
                                    contentPattern,
                                    acceptor
                            );
                        }
                    }


                    BaseMessageType messageType = module.get(TypeHelper.class).instantiateMessageType(
                            performative,
                            computedContentType,
                            /*normalizeToUpperBounds=*/ true
                    );
                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                            new MessageHandlerWhenExpressionContext(
                                    mod,
                                    out,
                                    messageType,
                                    computedContentType
                            )
                    );

                    if (patternMatchRequest.isPresent()) {
                        module.get(PatternMatchingSemantics.class).validate(patternMatchRequest, acceptor);

                    }
                    if (expr.isPresent()) {
                        module.get(RValueExpressionSemantics.class).validate(expr, acceptor);
                    }
                    module.get(ContextManager.class).exit();

                }
            }


        }

        final IJadescriptType finalContentType = contentType;
        input.safeDo(inputSafe -> {

            BaseMessageType messageType = module.get(TypeHelper.class).instantiateMessageType(
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
                            messageType,
                            finalContentType
                    )
            );

            prepareBlockWithAutoExtraction(input, input.__(FeatureWithBody::getBody), finalContentType);

            module.get(BlockSemantics.class).validate(input.__(FeatureWithBody::getBody), acceptor);

            module.get(ContextManager.class).exit();

        });
    }

    private Maybe<PatternMatchRequest> generatePatternMatchRequest(Maybe<OnMessageHandler> input) {
        Maybe<PatternMatchRequest> patternMatchRequest;
        final Maybe<Pattern> pattern = input.__(OnMessageHandler::getPattern);
        if (pattern.isPresent()) {
            final JadescriptFactory f = JadescriptFactory.eINSTANCE;
            final UnaryPrefix up = f.createUnaryPrefix();
            final OfNotation of = f.createOfNotation();
            final AidLiteral aid = f.createAidLiteral();
            final TypeCast tcast = f.createTypeCast();
            final AtomExpr atomExpr = f.createAtomExpr();
            final Primary pr = f.createPrimary();


            pr.setMessage("message");
            atomExpr.setAtom(pr);
            tcast.setAtomExpr(atomExpr);
            aid.setTypeCast(tcast);
            aid.setIsAidExpr(false);
            of.setTypeCast(aid);
            of.getProperties().add("content");

            up.setDebugScope(false);
            up.setDebugType(false);
            up.setOfNotation(of);

            patternMatchRequest = PatternMatchRequest.patternMatchRequest(
                    input,
                    pattern,
                    of(up),
                    true
            );
        } else {
            patternMatchRequest = nothing();
        }
        return patternMatchRequest;
    }

    public PatternMatchData generatePatternMatchData(
            Maybe<? extends EObject> containerEObject,
            Maybe<SavedContext> savedContext,
            Maybe<OnMessageHandler> input,
            boolean isValidation
    ) {

        savedContext.safeDo(it -> module.get(ContextManager.class).restore(it));

        IJadescriptType computedContentType = inferContentType(input);
        BaseMessageType messageType = module.get(TypeHelper.class).instantiateMessageType(
                input.__(OnMessageHandler::getPerformative),
                computedContentType,
                //Not normalizing to upper bounds when validating, in order to not interfere with the
                // type-inferring-from-pattern-match-and-when-expression system
                /*normalizeToUpperBounds=*/ !isValidation
        );

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                new MessageHandlerWhenExpressionContext(
                        mod,
                        out,
                        messageType,
                        computedContentType
                )
        );


        final Maybe<RValueExpression> expr = input.__(OnMessageHandler::getWhenBody).__(WhenExpression::getExpr);

        final Maybe<PatternMatchRequest> patternMatchRequest = generatePatternMatchRequest(input);

        List<StatementWriter> auxiliaryStatements = new ArrayList<>();
        if (patternMatchRequest.isPresent()) {
            auxiliaryStatements.addAll(module.get(PatternMatchingSemantics.class)
                    .generateAuxiliaryStatements(patternMatchRequest));
        }
        auxiliaryStatements.addAll(
                module.get(RValueExpressionSemantics.class).generateAuxiliaryStatements(expr)
        );


        String compiledExpression;
        if (isValidation) {
            compiledExpression = "";
        } else {
            String x1 = module.get(PatternMatchingSemantics.class)
                    .compileMatchesExpression(patternMatchRequest)
                    .orElse("");
            String x2 = module.get(RValueExpressionSemantics.class)
                    .compile(expr)
                    .orElse("");
            if (!x1.isBlank() && !x2.isBlank()) { // Both are present...
                compiledExpression = "(" + x1 + ") && (" + x2 + ")"; // ...infix &&
            } else if (x1.isBlank() && x2.isBlank()) { // Both are absent...
                compiledExpression = "true";  // ... use true
            } else {
                compiledExpression = x1 + x2; // ... otherwise return the one present
            }
        }

        final List<NamedSymbol> autoDeclaredVars = module.get(ContextManager.class).currentContext()
                .searchAs(
                        NamedSymbol.Searcher.class,
                        s -> s.searchName((Predicate<String>) null, null, null)
                                .filter(ne -> ne instanceof PatternMatchAutoDeclaredVariable)
                ).collect(Collectors.toList());


        module.get(ContextManager.class).exit();

        List<JvmDeclaredType> patternMatcherClasses;
        if (isValidation) {
            patternMatcherClasses = new ArrayList<>();
        } else {
            patternMatcherClasses = PatternMatchingSemantics.getPatternMatcherClasses(
                    auxiliaryStatements,
                    containerEObject,
                    module.get(JvmTypesBuilder.class),
                    module.get(TypeHelper.class),
                    module.get(CompilationHelper.class)
            );
        }

        List<JvmField> patternMatcherFields;
        if (isValidation) {
            patternMatcherFields = new ArrayList<>();
        } else {
            patternMatcherFields = PatternMatchingSemantics.getPatternMatcherFieldDeclarations(
                    auxiliaryStatements,
                    containerEObject,
                    module.get(JvmTypesBuilder.class),
                    module.get(TypeHelper.class),
                    module.get(CompilationHelper.class)
            );
        }

        return new PatternMatchData(
                compiledExpression,
                patternMatcherClasses,
                patternMatcherFields,
                autoDeclaredVars,
                computedContentType
        );
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

    public IJadescriptType inferContentType(Maybe<OnMessageHandler> input) {
        final Maybe<RValueExpression> expr = input.__(OnMessageHandler::getWhenBody)
                .__(WhenExpression::getExpr);

        final Maybe<Pattern> pattern = input.__(OnMessageHandler::getPattern);

        Optional<FlowTypeInferringTerm> contentOfMessage;
        IJadescriptType type = module.get(TypeHelper.class).ANY;

        if (expr.isPresent()) {
            contentOfMessage = module.get(RValueExpressionSemantics.class)
                    .extractFlowTypeTruths(expr)
                    .query("content", "message");
            if (contentOfMessage.isPresent()) {
                type = contentOfMessage.get().getType();
            }
            Optional<FlowTypeInferringTerm> content = module.get(RValueExpressionSemantics.class)
                    .extractFlowTypeTruths(expr)
                    .query("content");
            if (content.isPresent()) {
                type = content.get().getType();
            }
        }
        if (pattern.isPresent()) {
            type = module.get(TypeHelper.class).getGLB(
                    module.get(PatternMatchingSemantics.class).inferPatternType(pattern.toNullable())
                            .orElse(module.get(TypeHelper.class).NOTHING),
                    type
            );
        }

        return type;
    }

    private void generateRunMethod(Maybe<? extends EObject> source, SourceCodeBuilder scb) {
        w.ifStmnt(
                w.expr(Util.getOuterClassThisReference(source)
                        + "." + IGNORE_MSG_HANDLERS_VAR_NAME),
                w.block().addStatement(
                        w.assign("this." + MESSAGE_RECEIVED_BOOL_VAR_NAME, w.expr("false"))
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
                                w.block().addStatement(w.callStmnt("_e.printStackTrace"))
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
