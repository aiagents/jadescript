package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnBehaviourFailureHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnBehaviourFailureHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.PatternMatchRequest;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.of;

public class OnBehaviourFailureHandlerSemantics
        extends FeatureSemantics<OnBehaviourFailureHandler> {

    public OnBehaviourFailureHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(
            Maybe<OnBehaviourFailureHandler> input,
            Maybe<FeatureContainer> featureContainer,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(inputSafe -> {
            JvmGenericType eventClass = module.get(JvmTypesBuilder.class).toClass(inputSafe, synthesizeBehaviourFailureEventClassName(inputSafe), it -> {
                it.setVisibility(JvmVisibility.PRIVATE);

                it.getMembers().add(module.get(JvmTypesBuilder.class).toField(
                        inputSafe,
                        FAILURE_MATCHED_BOOL_VAR_NAME,
                        module.get(TypeHelper.class).BOOLEAN.asJvmTypeReference(),
                        itField -> {
                            itField.setVisibility(JvmVisibility.PUBLIC);
                            module.get(CompilationHelper.class).createAndSetInitializer(
                                    itField,
                                    w.False::writeSonnet
                            );
                        }
                ));

                it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                        inputSafe,
                        EVENT_HANDLER_STATE_RESET_METHOD_NAME,
                        module.get(TypeHelper.class).VOID.asJvmTypeReference(),
                        itMethod -> {
                            itMethod.setVisibility(JvmVisibility.PUBLIC);
                            module.get(CompilationHelper.class).createAndSetBody(
                                    itMethod,
                                    w.assign(FAILURE_MATCHED_BOOL_VAR_NAME, w.False)::writeSonnet
                            );
                        }
                ));

                Maybe<WhenExpression> whenBodyX = input.__(OnBehaviourFailureHandler::getWhenBody);
                Maybe<Pattern> contentPattern = input.__(OnBehaviourFailureHandler::getPattern);
                Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                PatternMatchData pmData = generatePatternMatchData(
                        featureContainer,
                        of(savedContext),
                        input,
                        contentPattern,
                        whenBodyX.__(WhenExpression::getExpr),
                        false
                );

                IJadescriptType reasonType = pmData.getInferredContentType();
                for (JvmDeclaredType patternMatchClass : pmData.getPatternMatchClasses()) {
                    it.getMembers().add(patternMatchClass);
                }
                for (JvmField patternMatchField : pmData.getPatternMatchFields()) {
                    it.getMembers().add(patternMatchField);
                }
                module.get(BlockSemantics.class).addInjectedVariables(body, pmData.getAutoDeclaredVariables());

                IJadescriptType computedReasonType = inferReasonType(
                        input.__(OnBehaviourFailureHandler::getPattern),
                        input.__(OnBehaviourFailureHandler::getWhenBody).__(WhenExpression::getExpr)
                );
                IJadescriptType computedBehaviourType = inferBehaviourType(
                        input.__(OnBehaviourFailureHandler::getWhenBody).__(WhenExpression::getExpr)
                );
                it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                        inputSafe,
                        "handle",
                        module.get(TypeHelper.class).typeRef(void.class),
                        itMethod -> {
                            itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                    inputSafe,
                                    "__failedBehaviour",
                                    module.get(TypeHelper.class).typeRef("jadescript.core.behaviours.Behaviour<?>")
                            ));
                            itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                    inputSafe,
                                    "__failureReason",
                                    module.get(TypeHelper.class).PROPOSITION.asJvmTypeReference()
                            ));

                            module.get(ContextManager.class).restore(savedContext);
                            module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                    new OnBehaviourFailureHandlerContext(
                                            mod,
                                            out,
                                            "behaviour failure",
                                            pmData.getAutoDeclaredVariables(),
                                            computedBehaviourType,
                                            computedReasonType
                                    )
                            );
                            final SavedContext saved = module.get(ContextManager.class).save();
                            module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                module.get(ContextManager.class).restore(saved);

                                scb.open("if (__failureReason instanceof "
                                        + module.get(TypeHelper.class).noGenericsTypeName(
                                        reasonType.compileToJavaTypeReference()
                                ) + "&&" +
                                        "__failedBehaviour instanceof "
                                        + module.get(TypeHelper.class).noGenericsTypeName(
                                        computedBehaviourType.compileToJavaTypeReference()
                                ) + ") {");


                                String compiledPatternExpr = pmData.getCompiledExpression();
                                if (!compiledPatternExpr.isBlank()) {
                                    scb.open("if (" + compiledPatternExpr + ") {");
                                }

                                w.assign(FAILURE_MATCHED_BOOL_VAR_NAME, w.True).writeSonnet(scb);

                                scb.add(encloseInGeneralHandlerTryCatch(
                                        module.get(CompilationHelper.class).compileBlockToNewSCB(body)
                                ));

                                if (!compiledPatternExpr.isBlank()) {
                                    scb.close("}");
                                }

                                scb.close("}");


                            });

                            module.get(ContextManager.class).exit();
                        }
                ));
            });
            members.add(eventClass);
            members.add(module.get(JvmTypesBuilder.class).toField(
                    inputSafe,
                    synthesizeBehaviourFailureEventVariableName(inputSafe),
                    module.get(TypeHelper.class).typeRef(eventClass), it -> {
                        it.setVisibility(JvmVisibility.PRIVATE);
                        module.get(CompilationHelper.class).createAndSetInitializer(it, scb -> {
                            scb.add("new " + eventClass.getQualifiedName('.') + "()");
                        });
                    }
            ));
        });
    }

    private PatternMatchData generatePatternMatchData(
            Maybe<? extends EObject> containerEObject,
            Maybe<SavedContext> savedContext,
            Maybe<OnBehaviourFailureHandler> input,
            Maybe<Pattern> pattern,
            Maybe<RValueExpression> expr,
            boolean isValidationOnly
    ) {
        savedContext.safeDo(it -> module.get(ContextManager.class).restore(it));

        IJadescriptType computedBehaviourType = inferBehaviourType(expr);
        IJadescriptType computedReasonType = inferReasonType(pattern, expr);

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                new OnBehaviourFailureHandlerWhenExpressionContext(
                        mod,
                        out,
                        computedBehaviourType,
                        computedReasonType
                )
        );

        final Maybe<PatternMatchRequest> patternMatchRequest = createPatternMatchRequest(pattern, input);

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
                computedReasonType
        );
    }

    private Maybe<PatternMatchRequest> createPatternMatchRequest(
            Maybe<Pattern> p,
            Maybe<OnBehaviourFailureHandler> input
    ) {
        Maybe<PatternMatchRequest> patternMatchRequest;
        if (p.isPresent()) {
            final JadescriptFactory f = JadescriptFactory.eINSTANCE;
            final UnaryPrefix up = f.createUnaryPrefix();
            final OfNotation of = f.createOfNotation();
            final AidLiteral aid = f.createAidLiteral();
            final TypeCast tcast = f.createTypeCast();
            final AtomExpr atomExpr = f.createAtomExpr();
            final Primary pr = f.createPrimary();

            pr.setIdentifier("failureReason");
            atomExpr.setAtom(pr);
            tcast.setAtomExpr(atomExpr);
            aid.setTypeCast(tcast);
            aid.setIsAidExpr(false);
            of.setTypeCast(aid);
            up.setDebugScope(false);
            up.setDebugType(false);
            up.setOfNotation(of);

            patternMatchRequest = PatternMatchRequest.patternMatchRequest(
                    input,
                    p,
                    of(up),
                    true
            );
        } else {
            patternMatchRequest = nothing();
        }

        return patternMatchRequest;
    }

    public IJadescriptType inferReasonType(Maybe<Pattern> pattern, Maybe<RValueExpression> expr) {
        IJadescriptType type = module.get(TypeHelper.class).PROPOSITION;
        if (expr.isPresent()) {
            Optional<FlowTypeInferringTerm> content = module.get(RValueExpressionSemantics.class)
                    .extractFlowTypeTruths(expr)
                    .query("failureReason");
            if (content.isPresent()) {
                type = content.get().getType();
            }
        }
        if (pattern.isPresent()) {
            type = module.get(TypeHelper.class).getGLB(
                    module.get(PatternMatchingSemantics.class)
                            .inferPatternType(pattern.toNullable())
                            .orElse(module.get(TypeHelper.class).NOTHING),
                    type
            );
        }

        return type;
    }

    public IJadescriptType inferBehaviourType(Maybe<RValueExpression> expr) {
        IJadescriptType type = module.get(TypeHelper.class).ANYBEHAVIOUR;
        if (expr.isPresent()) {
            Optional<FlowTypeInferringTerm> content = module.get(RValueExpressionSemantics.class)
                    .extractFlowTypeTruths(expr)
                    .query("behaviour");
            if (content.isPresent()) {
                type = content.get().getType();
            }
        }
        return type;
    }

    @Override
    public void validateFeature(
            Maybe<OnBehaviourFailureHandler> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    ) {
        IJadescriptType reasonType = module.get(TypeHelper.class).PROPOSITION;
        IJadescriptType behaviourType = module.get(TypeHelper.class).ANYBEHAVIOUR;
        Maybe<WhenExpression> whenBodyX = input.__(OnBehaviourFailureHandler::getWhenBody);
        Maybe<Pattern> reasonPattern = input.__(OnBehaviourFailureHandler::getPattern);
        List<NamedSymbol> patternMatchDeclaredVariables = new ArrayList<>();

        if (whenBodyX.isPresent() || reasonPattern.isPresent()) {
            final Maybe<RValueExpression> expr = whenBodyX.__(WhenExpression::getExpr);
            final IJadescriptType reasonTypeFromHeader = inferReasonType(reasonPattern, expr);
            final IJadescriptType behaviourTypeFromHeader = inferBehaviourType(expr);
            reasonType = module.get(TypeHelper.class).getGLB(reasonType, reasonTypeFromHeader);
            behaviourType = module.get(TypeHelper.class).getGLB(behaviourType, behaviourTypeFromHeader);

            patternMatchDeclaredVariables.addAll(
                    generatePatternMatchData(container, nothing(), input, reasonPattern, expr, true)
                            .getAutoDeclaredVariables()
            );

            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(RValueExpressionSemantics.class).validateUsageAsWhenExpression(expr, expr, interceptAcceptor);
            if (!interceptAcceptor.thereAreErrors()) {
                Maybe<PatternMatchRequest> patternMatchRequest = createPatternMatchRequest(reasonPattern, input);
                if(expr.isPresent() || patternMatchRequest.isPresent()){
                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                            new OnBehaviourFailureHandlerWhenExpressionContext(
                                    mod,
                                    out,
                                    behaviourTypeFromHeader,
                                    reasonTypeFromHeader
                            )
                    );

                    if(patternMatchRequest.isPresent()){
                        module.get(PatternMatchingSemantics.class).validate(patternMatchRequest, acceptor);
                    }

                    if(expr.isPresent()){
                        module.get(RValueExpressionSemantics.class).validate(expr, acceptor);
                    }

                    module.get(ContextManager.class).exit();
                }
            }
        }

        final IJadescriptType finalReasonType = reasonType;
        final IJadescriptType finalBehaviourType = behaviourType;
        input.safeDo(inputSafe ->{
            module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                    new OnBehaviourFailureHandlerContext(mod, out,
                            "behaviour failure", patternMatchDeclaredVariables,
                            finalBehaviourType, finalReasonType
                    ));

            module.get(BlockSemantics.class).validate(input.__(FeatureWithBody::getBody), acceptor);

            module.get(ContextManager.class).exit();
        });


    }
}
