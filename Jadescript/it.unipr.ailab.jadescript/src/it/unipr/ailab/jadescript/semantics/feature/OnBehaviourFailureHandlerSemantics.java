package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.BehaviourFailureHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnBehaviourFailureHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnBehaviourFailureHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.function.Function;

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
        if (input.isNothing()) {
            return;
        }

        OnBehaviourFailureHandler inputSafe = input.toNullable();


        final SavedContext savedContext =
            module.get(ContextManager.class).save();

        final String className =
            synthesizeBehaviourFailureEventClassName(inputSafe);

        JvmGenericType eventClass = createEventClass(
            input,
            inputSafe,
            savedContext,
            className
        );

        members.add(eventClass);

        members.add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            synthesizeBehaviourFailureEventVariableName(inputSafe),
            module.get(TypeHelper.class).typeRef(eventClass), it -> {
                it.setVisibility(JvmVisibility.PRIVATE);
                module.get(CompilationHelper.class).createAndSetInitializer(
                    it,
                    scb -> {
                        scb.add("new " + eventClass.getQualifiedName('.') +
                            "()");
                    }
                );
            }
        ));
    }


    private JvmGenericType createEventClass(
        Maybe<OnBehaviourFailureHandler> input,
        OnBehaviourFailureHandler inputSafe,
        SavedContext savedContext,
        String className
    ) {
        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);


        return jvmTB.toClass(
            inputSafe,
            className,
            it -> {
                it.setVisibility(JvmVisibility.PRIVATE);

                it.getMembers().add(jvmTB.toField(
                    inputSafe,
                    FAILURE_MATCHED_BOOL_VAR_NAME,
                    module.get(TypeHelper.class).BOOLEAN.asJvmTypeReference(),
                    itField -> {
                        itField.setVisibility(JvmVisibility.PUBLIC);
                        module.get(CompilationHelper.class)
                            .createAndSetInitializer(
                                itField,
                                w.False::writeSonnet
                            );
                    }
                ));

                it.getMembers().add(jvmTB.toMethod(
                    inputSafe,
                    EVENT_HANDLER_STATE_RESET_METHOD_NAME,
                    module.get(TypeHelper.class).VOID.asJvmTypeReference(),
                    itMethod -> {
                        itMethod.setVisibility(JvmVisibility.PUBLIC);
                        module.get(CompilationHelper.class).createAndSetBody(
                            itMethod,
                            w.assign(
                                FAILURE_MATCHED_BOOL_VAR_NAME,
                                w.False
                            )::writeSonnet
                        );
                    }
                ));


                it.getMembers().add(jvmTB.toMethod(
                    inputSafe,
                    "handle",
                    module.get(TypeHelper.class).typeRef(void.class),
                    itMethod -> {
                        itMethod.getParameters().add(jvmTB.toParameter(
                            inputSafe,
                            "__failedBehaviour",
                            module.get(TypeHelper.class)
                                .typeRef(
                                    "jadescript.core.behaviours.Behaviour<?>")
                        ));
                        itMethod.getParameters().add(jvmTB.toParameter(
                            inputSafe,
                            "__failureReason",
                            module.get(TypeHelper.class)
                                .PROPOSITION.asJvmTypeReference()
                        ));


                        module.get(CompilationHelper.class).createAndSetBody(
                            itMethod,
                            scb -> {
                                fillHandleMethod(
                                    input,
                                    savedContext,
                                    scb
                                );
                            }
                        );

                    }
                ));
            }
        );
    }


    private void fillHandleMethod(
        Maybe<OnBehaviourFailureHandler> input,
        SavedContext savedContext,
        SourceCodeBuilder scb
    ) {
        module.get(ContextManager.class).restore(savedContext);

        final Maybe<WhenExpression> whenBody =
            input.__(OnBehaviourFailureHandler::getWhenBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);
        final Maybe<Pattern> contentPattern =
            input.__(OnBehaviourFailureHandler::getPattern);
        final Maybe<LValueExpression> pattern = contentPattern
            .__(x -> (LValueExpression) x);
        final Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        module.get(ContextManager.class).enterProceduralFeature(
            OnBehaviourFailureHandlerWhenExpressionContext::new
        );

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        final IJadescriptType propositionUpperBound = typeHelper.PROPOSITION;
        final IJadescriptType behaviourUpperBound = typeHelper.ANYBEHAVIOUR;

        IJadescriptType pattNarrowedContentType = propositionUpperBound;
        IJadescriptType wexpNarrowedContentType = propositionUpperBound;
        IJadescriptType wexpNarrowedBehaviourType = behaviourUpperBound;

        Function<StaticState, StaticState> prepareBodyState;
        final StaticState afterPatternDidMatch;
        String part1;
        if (pattern.isPresent()) {
            final PatternMatchHelper patternMatchHelper =
                module.get(PatternMatchHelper.class);

            PatternMatchInput<LValueExpression> patternMatchInput
                = patternMatchHelper.handlerHeader(
                propositionUpperBound,
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
            ).solve(propositionUpperBound);

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
                FAILURE_REASON_VAR_NAME
            );
        } else {
            prepareBodyState = Function.identity();
            afterPatternDidMatch = beforePattern;
            part1 = "";
        }

        String part2;
        StaticState afterWhenExprReturnedTrue;
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

            afterWhenExprReturnedTrue = rves.assertReturnedTrue(
                whenExpr,
                afterWhenExpr
            );

            wexpNarrowedContentType = afterWhenExprReturnedTrue.inferUpperBound(
                    ed -> ed.equals(
                        new ExpressionDescriptor.PropertyChain(
                            "failureReason"
                        )
                    ),
                    null
                ).findFirst()
                .orElse(propositionUpperBound);

            wexpNarrowedBehaviourType = afterWhenExprReturnedTrue
                .inferUpperBound(
                    ed -> ed.equals(
                        new ExpressionDescriptor.PropertyChain(
                            "behaviour"
                        )
                    ),
                    null
                ).findFirst()
                .orElse(behaviourUpperBound);

            prepareBodyState = prepareBodyState.andThen(
                s -> rves.assertReturnedTrue(
                    whenExpr,
                    s
                )
            );
        } else {
            afterWhenExprReturnedTrue = afterPatternDidMatch;
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

        final IJadescriptType finalBehaviourType = wexpNarrowedBehaviourType;

        scb.open("if (__failureReason instanceof "
            + module.get(TypeHelper.class).noGenericsTypeName(
            finalContentType.compileToJavaTypeReference()
        ) + "&&" +
            "__failedBehaviour instanceof "
            + module.get(TypeHelper.class).noGenericsTypeName(
            finalBehaviourType.compileToJavaTypeReference()
        ) + ") {");


        scb.open("if (" + compiledExpression + ") {");


        w.assign(
            FAILURE_MATCHED_BOOL_VAR_NAME,
            w.True
        ).writeSonnet(scb);

        StaticState inBody = prepareBodyState.apply(
            afterWhenExprReturnedTrue
        ).assertNamedSymbol(BehaviourFailureHandledContext
            .failureReasonContextGeneratedReference(
                finalContentType
            )
        ).assertNamedSymbol(BehaviourFailureHandledContext
            .behaviourContextGeneratedReference(
                finalBehaviourType
            )
        );

        module.get(ContextManager.class).enterProceduralFeature(
            (mod, out) -> new OnBehaviourFailureHandlerContext(
                mod,
                out,
                "behaviour failure",
                finalBehaviourType,
                finalContentType
            )
        );

        inBody = inBody.enterScope();
        PSR<SourceCodeBuilder> bodyPSR = module.get(CompilationHelper.class)
            .compileBlockToNewSCB(inBody, body);

        scb.add(encloseInGeneralHandlerTryCatch(bodyPSR.result()));

        module.get(ContextManager.class).exit();

        scb.close("}");

        scb.close("}");

    }


    @Override
    public void validateFeature(
        Maybe<OnBehaviourFailureHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<WhenExpression> whenBody =
            input.__(OnBehaviourFailureHandler::getWhenBody);

        final Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);
        final Maybe<LValueExpression> pattern = input
            .__(OnBehaviourFailureHandler::getPattern)
            .__(x -> (LValueExpression) x);


        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final IJadescriptType propositionUpperBound = typeHelper.PROPOSITION;
        final IJadescriptType behaviourUpperBound = typeHelper.ANYBEHAVIOUR;

        IJadescriptType pattNarrowedContentType = propositionUpperBound;
        IJadescriptType wexpNarrowedContentType = propositionUpperBound;
        IJadescriptType wexpNarrowedBehaviourType = behaviourUpperBound;


        module.get(ContextManager.class).enterProceduralFeature(
            OnBehaviourFailureHandlerWhenExpressionContext::new
        );

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        Function<StaticState, StaticState> prepareBodyState =
            Function.identity();

        StaticState afterPatternDidMatch = beforePattern;

        if(pattern.isPresent()){
            final PatternMatchHelper patternMatchHelper
                = module.get(PatternMatchHelper.class);

            PatternMatchInput<LValueExpression> patternMatchInput
                = patternMatchHelper.handlerHeader(
                propositionUpperBound,
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
                ).solve(propositionUpperBound);

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
        if(whenExpr.isPresent()){
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
                                "failureReason"
                            )
                        ),
                        null
                    ).findFirst()
                    .orElse(propositionUpperBound);

                wexpNarrowedBehaviourType = afterWhenExprReturnedTrue
                    .inferUpperBound(
                        ed -> ed.equals(
                            new ExpressionDescriptor.PropertyChain(
                                "behaviour"
                            )
                        ),
                        null
                    ).findFirst()
                    .orElse(behaviourUpperBound);


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

        final IJadescriptType finalBehaviourType = wexpNarrowedBehaviourType;

        StaticState inBody = prepareBodyState.apply(afterWhenExprReturnedTrue)
            .assertNamedSymbol(BehaviourFailureHandledContext
                .failureReasonContextGeneratedReference(
                    finalContentType
                )
            ).assertNamedSymbol(BehaviourFailureHandledContext
                .behaviourContextGeneratedReference(
                    finalBehaviourType
                )
            );

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new OnBehaviourFailureHandlerContext(
                mod,
                out,
                "behaviour failre",
                finalBehaviourType,
                finalContentType
            )
        );

        inBody = inBody.enterScope();

        module.get(BlockSemantics.class).validate(
            body,
            inBody,
            acceptor
        );

        module.get(ContextManager.class).exit();

    }

}
