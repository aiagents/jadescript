package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.PerceptHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.PerceptHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.PerceptPerceivedContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TemplateCompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.PatternMatchRequest;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jade.lang.acl.MessageTemplate;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

public class OnPerceptHandlerSemantics extends FeatureSemantics<OnPerceptHandler> {
    public OnPerceptHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(
            Maybe<OnPerceptHandler> input,
            Maybe<FeatureContainer> featureContainer,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(inputSafe -> {
            JvmGenericType eventClass = module.get(JvmTypesBuilder.class).toClass(inputSafe, synthesizeBehaviourEventClassName(inputSafe), it -> {
                it.setVisibility(JvmVisibility.PRIVATE);
                it.getMembers().add(module.get(JvmTypesBuilder.class).toField(
                        inputSafe,
                        PERCEPT_VAR_NAME,
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

                            w.ifStmnt(w.expr(Util.getOuterClassThisReference(input) + "."
                                    + IGNORE_MSG_HANDLERS_VAR_NAME), w.block()
                                    .addStatement(w.assign("this." + MESSAGE_RECEIVED_BOOL_VAR_NAME, w.expr("false")))
                                    .addStatement(w.returnStmnt())
                            ).writeSonnet(scb);
                            w.callStmnt("receive").writeSonnet(scb);
                            w.ifStmnt(w.expr(PERCEPT_VAR_NAME + " != null"), w.block()
                                    .addStatement(w.assign(Util.getOuterClassThisReference(input) + "."
                                            + IGNORE_MSG_HANDLERS_VAR_NAME, w.expr("true")))
                                    .addStatement(w.assign("this." + MESSAGE_RECEIVED_BOOL_VAR_NAME, w.expr("true")))
                                    .addStatement(w.tryCatch(w.block()
                                                    .addStatement(w.callStmnt("doBody"))
                                                    .addStatement(w.assign("this." + PERCEPT_VAR_NAME, w.expr("null")))
                                            ).addCatchBranch("Exception", "_e", w.block()
                                                    .addStatement(w.callStmnt("_e.printStackTrace"))
                                            )
                                    )
                            ).setElseBranch(w.block()
                                    .addStatement(w.assign("this." + MESSAGE_RECEIVED_BOOL_VAR_NAME, w.expr("false")))
                            ).writeSonnet(scb);

                        })
                ));


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


                List<ExpressionWriter> templateExpressions = new ArrayList<>();
                templateExpressions.add(TemplateCompilationHelper.isPercept());

                if(input.__(OnPerceptHandler::isStale).extract(nullAsFalse)){
                    templateExpressions.add(TemplateCompilationHelper.isStale());
                }

                IJadescriptType computedContentType = inferContentType(input);

                List<StatementWriter> extractStatements = generateExtractStatements(
                        input,
                        computedContentType,
                        module
                );

                final BlockWriter tryBlock = w.block();
                tryBlock.addStatement(w.ifStmnt(
                        w.expr("!(((jadescript.core.percept.Percept) " + THE_AGENT + "().getContentManager()" +
                                ".extractContent(" + PERCEPT_VAR_NAME + ")).getContent() instanceof " +
                                module.get(TypeHelper.class).noGenericsTypeName(
                                        computedContentType.compileToJavaTypeReference()
                                ) + ")"),
                        w.block().addStatement(w.returnStmnt(w.expr("false")))
                ));


                templateExpressions.add(TemplateCompilationHelper.customPercept(
                        w.block().addStatement(
                                w.tryCatch(tryBlock
                                        .addStatements(extractStatements)
                                        .addStatement(w.returnStmnt(w.expr(pmData.getCompiledExpression())))
                                ).addCatchBranch("java.lang.Throwable", "_e", w.block()
                                        .addStatement(w.callStmnt("_e.printStackTrace"))
                                        .addStatement(w.returnStmnt(w.expr("false")))
                                )
                        )
                ));


                final ExpressionWriter composedTemplateExpression = templateExpressions.stream().reduce(
                        TemplateCompilationHelper.True(),
                        TemplateCompilationHelper::and
                );


                it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(inputSafe, messageTemplateName,
                                module.get(TypeHelper.class).typeRef(MessageTemplate.class), itMethod -> {
                                    itMethod.setVisibility(JvmVisibility.PRIVATE);
                                    module.get(CompilationHelper.class).createAndSetBody(
                                            itMethod,
                                            scb -> w.returnStmnt(composedTemplateExpression).writeSonnet(scb)
                                    );
                                }
                        )
                );


                it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                        inputSafe,
                        "receive",
                        module.get(TypeHelper.class).typeRef(void.class),
                        itMethod ->
                                module.get(CompilationHelper.class).createAndSetBody(itMethod, scb2 ->
                                        w.ifStmnt(w.expr("myAgent!=null"), w.block()
                                                .addStatement(w.assign(
                                                        PERCEPT_VAR_NAME,
                                                        w.callExpr(
                                                                "jadescript.core.message.Message.wrap",
                                                                w.callExpr(
                                                                        "myAgent.receive",
                                                                        w.expr(messageTemplateName + "()")
                                                                )
                                                        )
                                                )).addStatement(
                                                        w.ifStmnt(
                                                                w.expr(PERCEPT_VAR_NAME + " != null"),
                                                                w.block().addStatement(
                                                                        w.callStmnt(
                                                                                THE_AGENT + "().__cleanIgnoredFlagForMessage",
                                                                                w.expr(PERCEPT_VAR_NAME)
                                                                        )
                                                                )
                                                        )
                                                )
                                        ).writeSonnet(scb2)
                                )
                ));


                module.get(BlockSemantics.class).addInjectedStatements(body, extractStatements);

                module.get(BlockSemantics.class).addInjectedVariable(
                        body,
                        new ContextGeneratedReference(PERCEPT_CONTENT_VAR_NAME, contentType)
                );


                it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                        inputSafe,
                        "doBody",
                        module.get(TypeHelper.class).typeRef(void.class),
                        itMethod -> {
                            itMethod.getExceptions().add(module.get(TypeHelper.class).typeRef(Exception.class));
                            module.get(ContextManager.class).restore(savedContext);
                            module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                    new PerceptHandlerContext(
                                            mod,
                                            out,
                                            "percept",
                                            pmData.getAutoDeclaredVariables(),
                                            contentType
                                    )
                            );

                            final SavedContext saved = module.get(ContextManager.class).save();
                            module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                module.get(ContextManager.class).restore(saved);
                                scb.add(encloseInGeneralHandlerTryCatch(
                                        module.get(CompilationHelper.class).compileBlockToNewSCB(body)
                                ));
                            });

                            module.get(ContextManager.class).exit();
                        }
                ));
            });

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

    private static List<StatementWriter> generateExtractStatements(
            Maybe<? extends OnPerceptHandler> container,
            IJadescriptType contentType,
            SemanticsModule module
    ) {
        List<StatementWriter> result = new ArrayList<>();

        Maybe<LightweightTypeReference> type = container.__(containerSafe -> {
            return module.get(CompilationHelper.class).toLightweightTypeReference(contentType, containerSafe);
        });


        type.safeDo(typeSafe -> {
            result.add(w.variable(
                    typeSafe.toTypeReference().getQualifiedName('.'),
                    PERCEPT_CONTENT_VAR_NAME,
                    w.expr("(" + typeSafe.toTypeReference().getQualifiedName('.') + ") " +
                            "((jadescript.core.percept.Percept)"
                            + THE_AGENT + "().getContentManager().extractContent(" + PERCEPT_VAR_NAME + "))" +
                            ".getContent()"
                    )
            ));
        });
        return result;
    }


    @Override
    public void validateFeature(
            Maybe<OnPerceptHandler> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    ) {
        IJadescriptType contentType = module.get(TypeHelper.class).ANY;

        Maybe<WhenExpression> whenBodyX = input.__(OnPerceptHandler::getWhenBody);
        Maybe<Pattern> contentPattern = input.__(OnPerceptHandler::getPattern);
        Maybe<CodeBlock> codeBlock = input.__(OnPerceptHandler::getBody);
        Maybe<RValueExpression> expr = whenBodyX.__(WhenExpression::getExpr);

        List<NamedSymbol> patternMatchDeclaredVariables = new ArrayList<>();

        if (whenBodyX.isPresent() || contentPattern.isPresent()) {


            contentType = inferContentType(input);

            patternMatchDeclaredVariables.addAll(
                    generatePatternMatchData(input, nothing(), input, true).getAutoDeclaredVariables()
            );


            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(RValueExpressionSemantics.class).validateUsageAsHandlerCondition(expr, expr, interceptAcceptor);
            if (!interceptAcceptor.thereAreErrors()) {
                IJadescriptType computedContentType = inferContentType(input);

                final Maybe<PatternMatchRequest> patternMatchRequest = createPatternMatchRequest(input);
                if (expr.isPresent() || patternMatchRequest.isPresent()) {
                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                            new PerceptHandlerWhenExpressionContext(
                                    mod,
                                    out,
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

            module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                    new PerceptHandlerContext(mod, out,
                            "percept", patternMatchDeclaredVariables, finalContentType
                    ));

            List<StatementWriter> result = new ArrayList<>();


            Maybe<LightweightTypeReference> type = input.__(containerSafe -> {
                return module.get(CompilationHelper.class).toLightweightTypeReference(finalContentType, containerSafe);
            });


            type.safeDo(typeSafe -> {
                result.add(w.variable(
                        typeSafe.toTypeReference().getQualifiedName('.'),
                        PERCEPT_CONTENT_VAR_NAME,
                        w.expr("(" + typeSafe.toTypeReference().getQualifiedName('.') + ") " +
                                "((jadescript.core.percept.Percept)"
                                + THE_AGENT + "().getContentManager().extractContent(" + PERCEPT_VAR_NAME + "))" +
                                ".getContent()"
                        )
                ));
            });
            module.get(BlockSemantics.class).addInjectedStatements(codeBlock, result);

            module.get(BlockSemantics.class).addInjectedVariable(
                    codeBlock,
                    PerceptPerceivedContext.perceptContentContextGeneratedReference(finalContentType)
            );

            module.get(BlockSemantics.class).validate(input.__(FeatureWithBody::getBody), acceptor);

            module.get(ContextManager.class).exit();

        });

    }

    public IJadescriptType inferContentType(Maybe<OnPerceptHandler> input) {
        final Maybe<RValueExpression> expr = input.__(OnPerceptHandler::getWhenBody)
                .__(WhenExpression::getExpr);

        final Maybe<Pattern> pattern = input.__(OnPerceptHandler::getPattern);

        Optional<FlowTypeInferringTerm> contentOfMessage;

        contentOfMessage = module.get(RValueExpressionSemantics.class)
                .extractFlowTypeTruths(expr)
                .query("percept");

        IJadescriptType type;
        if (contentOfMessage.isPresent()) {
            type = contentOfMessage.get().getType();
        } else {
            type = module.get(TypeHelper.class).PREDICATE;
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

    public PatternMatchData generatePatternMatchData(
            Maybe<? extends EObject> containerEObject,
            Maybe<SavedContext> savedContext,
            Maybe<OnPerceptHandler> input,
            boolean isValidationOnly
    ) {

        savedContext.safeDo(it -> module.get(ContextManager.class).restore(it));

        IJadescriptType computedContentType = inferContentType(input);

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                new PerceptHandlerWhenExpressionContext(
                        mod,
                        out,
                        computedContentType
                )
        );


        final Maybe<RValueExpression> expr = input.__(OnPerceptHandler::getWhenBody).__(WhenExpression::getExpr);

        final Maybe<PatternMatchRequest> patternMatchRequest = createPatternMatchRequest(input);

        List<StatementWriter> auxiliaryStatements = new ArrayList<>();
        if (patternMatchRequest.isPresent()) {
            auxiliaryStatements.addAll(module.get(PatternMatchingSemantics.class)
                    .generateAuxiliaryStatements(patternMatchRequest));
        }
        auxiliaryStatements.addAll(
                module.get(RValueExpressionSemantics.class).generateAuxiliaryStatements(expr)
        );


        String compiledExpression;
        if (isValidationOnly) {
            compiledExpression = "";
        } else {
            String x1 = module.get(PatternMatchingSemantics.class)
                    .compileMatchesExpression(patternMatchRequest)
                    .orElse("");
            String x2 = module.get(RValueExpressionSemantics.class)
                    .compile(expr)
                    .orElse("");
            if (!x1.isBlank() && !x2.isBlank()) {
                compiledExpression = "(" + x1 + ") && (" + x2 + ")";
            } else {
                compiledExpression = x1 + x2;
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
        if (isValidationOnly) {
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
        if (isValidationOnly) {
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


    public static Maybe<PatternMatchRequest> createPatternMatchRequest(
            Maybe<OnPerceptHandler> input
    ) {
        final Maybe<Pattern> pattern = input.__(OnPerceptHandler::getPattern);
        if (pattern.isPresent()) {
            final JadescriptFactory f = JadescriptFactory.eINSTANCE;
            final UnaryPrefix up = f.createUnaryPrefix();
            final OfNotation of = f.createOfNotation();
            final AidLiteral aid = f.createAidLiteral();
            final TypeCast tcast = f.createTypeCast();
            final AtomExpr atomExpr = f.createAtomExpr();
            final Primary pr = f.createPrimary();

            pr.setPercept("percept");
            atomExpr.setAtom(pr);
            tcast.setAtomExpr(atomExpr);
            aid.setTypeCast(tcast);
            aid.setIsAidExpr(false);
            of.setTypeCast(aid);
            up.setDebugScope(false);
            up.setDebugType(false);
            up.setOfNotation(of);

            return PatternMatchRequest.patternMatchRequest(
                    input,
                    pattern,
                    of(up),
                    true
            );
        } else {
            return nothing();
        }
    }

}
