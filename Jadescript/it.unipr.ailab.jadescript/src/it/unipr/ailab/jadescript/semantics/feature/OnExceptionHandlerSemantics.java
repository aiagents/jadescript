package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExceptionHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExceptionHandlerWhenExpressionContext;
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
import static it.unipr.ailab.maybe.Maybe.some;

public class OnExceptionHandlerSemantics extends FeatureSemantics<OnExceptionHandler> {
    public OnExceptionHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(
            Maybe<OnExceptionHandler> input,
            Maybe<FeatureContainer> featureContainer,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(inputSafe -> {
            JvmGenericType eventClass = module.get(JvmTypesBuilder.class).toClass(inputSafe, synthesizeExceptionEventClassName(inputSafe), it -> {
                it.setVisibility(JvmVisibility.PRIVATE);

                it.getMembers().add(module.get(JvmTypesBuilder.class).toField(
                        inputSafe,
                        EXCEPTION_MATCHED_BOOL_VAR_NAME,
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
                                    w.assign(EXCEPTION_MATCHED_BOOL_VAR_NAME, w.False)::writeSonnet
                            );
                        }
                ));

                Maybe<WhenExpression> whenBodyX = input.__(OnExceptionHandler::getWhenBody);
                Maybe<Pattern> contentPattern = input.__(OnExceptionHandler::getPattern);
                Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                PatternMatchData pmData = generatePatternMatchData(
                        featureContainer,
                        some(savedContext),
                        input,
                        contentPattern,
                        whenBodyX.__(WhenExpression::getExpr),
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

                IJadescriptType computedContentType = inferContentType(
                        input.__(OnExceptionHandler::getPattern),
                        input.__(OnExceptionHandler::getWhenBody).__(WhenExpression::getExpr)
                );
                it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                        inputSafe,
                        "handle",
                        module.get(TypeHelper.class).typeRef(void.class),
                        itMethod -> {
                            itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                    inputSafe,
                                    "__inputException",
                                    module.get(TypeHelper.class).typeRef(jadescript.core.exception.JadescriptException.class)
                            ));
                            itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                    inputSafe,
                                    EXCEPTION_THROWER_NAME,
                                    module.get(TypeHelper.class).typeRef(jadescript.core.exception.ExceptionThrower.class)
                            ));


                            module.get(ContextManager.class).restore(savedContext);
                            module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                    new OnExceptionHandlerContext(
                                            mod,
                                            out,
                                            "exception",
                                            pmData.getAutoDeclaredVariables(),
                                            contentType
                                    )
                            );
                            final SavedContext saved = module.get(ContextManager.class).save();
                            module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                module.get(ContextManager.class).restore(saved);

                                scb.open("if (__inputException.getReason() instanceof "
                                        + module.get(TypeHelper.class).noGenericsTypeName(
                                        computedContentType.compileToJavaTypeReference()) + ") {");

                                w.variable(
                                        contentType.compileToJavaTypeReference(),
                                        EXCEPTION_REASON_VAR_NAME,
                                        w.expr("(" + contentType.compileAsJavaCast() + " __inputException.getReason())")
                                ).writeSonnet(scb);

                                String compiledPatternExpr = pmData.getCompiledExpression();
                                if (!compiledPatternExpr.isBlank()) {
                                    scb.open("if (" + compiledPatternExpr + ") {");
                                }

                                w.assign(EXCEPTION_MATCHED_BOOL_VAR_NAME, w.True).writeSonnet(scb);

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
                    synthesizeExceptionEventVariableName(inputSafe),
                    module.get(TypeHelper.class).typeRef(eventClass), it -> {
                        it.setVisibility(JvmVisibility.PRIVATE);
                        module.get(CompilationHelper.class).createAndSetInitializer(it, scb -> {
                            scb.add("new " + eventClass.getQualifiedName('.') + "()");
                        });
                    }
            ));
        });
    }

    public IJadescriptType inferContentType(Maybe<Pattern> pattern, Maybe<RValueExpression> expr) {

        IJadescriptType type = module.get(TypeHelper.class).PROPOSITION;

        if (expr.isPresent()) {
            Optional<FlowTypeInferringTerm> content = module.get(RValueExpressionSemantics.class)
                    .extractFlowTypeTruths(expr)
                    .query("exception");
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

    @Override
    public void validateFeature(
            Maybe<OnExceptionHandler> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    ) {

        IJadescriptType contentType = module.get(TypeHelper.class).PROPOSITION;
        Maybe<WhenExpression> whenBodyX = input.__(OnExceptionHandler::getWhenBody);
        Maybe<Pattern> contentPattern = input.__(OnExceptionHandler::getPattern);
        List<NamedSymbol> patternMatchDeclaredVariables = new ArrayList<>();

        if (whenBodyX.isPresent() || contentPattern.isPresent()) {
            final Maybe<RValueExpression> expr = whenBodyX.__(WhenExpression::getExpr);
            final IJadescriptType contentTypeFromHeader = inferContentType(contentPattern, expr);
            contentType = module.get(TypeHelper.class).getGLB(contentType, contentTypeFromHeader);

            patternMatchDeclaredVariables.addAll(
                    generatePatternMatchData(container, nothing(), input, contentPattern, expr, true)
                            .getAutoDeclaredVariables()
            );

            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(RValueExpressionSemantics.class).validateUsageAsHandlerCondition(expr, expr, , interceptAcceptor);
            if (!interceptAcceptor.thereAreErrors()) {
                Maybe<PatternMatchRequest> patternMatchRequest = createPatternMatchRequest(
                        contentPattern, input
                );

                if (expr.isPresent() || patternMatchRequest.isPresent()) {
                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                            new OnExceptionHandlerWhenExpressionContext(
                                    mod,
                                    out,
                                    contentTypeFromHeader
                            )
                    );

                    if (patternMatchRequest.isPresent()) {
                        module.get(PatternMatchingSemantics.class).validate(patternMatchRequest, acceptor);
                    }

                    if (expr.isPresent()) {
                        module.get(RValueExpressionSemantics.class).validate(expr, , acceptor);
                    }

                    module.get(ContextManager.class).exit();
                }

            }
        }

        final IJadescriptType finalContentType = contentType;
        input.safeDo(inputSafe -> {
            module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                    new OnExceptionHandlerContext(mod, out,
                            "exception", patternMatchDeclaredVariables, finalContentType
                    ));

            module.get(BlockSemantics.class).validate(input.__(FeatureWithBody::getBody), acceptor);

            module.get(ContextManager.class).exit();
        });
    }

    private Maybe<PatternMatchRequest> createPatternMatchRequest(
            Maybe<Pattern> contentPattern,
            Maybe<OnExceptionHandler> input
    ) {
        Maybe<PatternMatchRequest> patternMatchRequest;
        if (contentPattern.isPresent()) {
            final JadescriptFactory f = JadescriptFactory.eINSTANCE;
            final UnaryPrefix up = f.createUnaryPrefix();
            final OfNotation of = f.createOfNotation();
            final AidLiteral aid = f.createAidLiteral();
            final TypeCast tcast = f.createTypeCast();
            final AtomExpr atomExpr = f.createAtomExpr();
            final Primary pr = f.createPrimary();

            pr.setException("exception");
            atomExpr.setAtom(pr);
            tcast.setAtomExpr(atomExpr);
            aid.setTypeCast(tcast);
            aid.setIsAidExpr(false);
            of.setAidLiteral(aid);
            up.setDebugScope(false);
            up.setDebugType(false);
            up.setOfNotation(of);

            patternMatchRequest = PatternMatchRequest.patternMatchRequest(
                    input,
                    contentPattern,
                    some(up),
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
            Maybe<OnExceptionHandler> input,
            Maybe<Pattern> pattern,
            Maybe<RValueExpression> expr,
            boolean isValidationOnly
    ) {
        savedContext.safeDo(it -> module.get(ContextManager.class).restore(it));

        IJadescriptType computedContentType = inferContentType(pattern, expr);

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                new OnExceptionHandlerWhenExpressionContext(
                        mod,
                        out,
                        computedContentType
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
                    .compile(expr, , )
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
                    module
            );
        }

        List<JvmField> patternMatcherFields;
        if (isValidationOnly) {
            patternMatcherFields = new ArrayList<>();
        } else {
            patternMatcherFields = PatternMatchingSemantics.getPatternMatcherFieldDeclarations(
                    auxiliaryStatements,
                    containerEObject,
                    module
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
}
