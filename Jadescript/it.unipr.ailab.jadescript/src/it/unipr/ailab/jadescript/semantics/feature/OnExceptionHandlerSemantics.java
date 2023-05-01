package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ExceptionHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExceptionHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExceptionHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeLatticeComputer;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import jadescript.core.exception.ExceptionThrower;
import jadescript.core.exception.JadescriptException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.function.Function;

public class OnExceptionHandlerSemantics
    extends DeclarationMemberSemantics<OnExceptionHandler> {

    public OnExceptionHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnExceptionHandler> input,
        Maybe<FeatureContainer> featureContainer,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        if (input.isNothing()) {
            return;
        }
        OnExceptionHandler inputSafe = input.toNullable();

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

        members.add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            synthesizeExceptionEventVariableName(inputSafe),
            module.get(JvmTypeHelper.class).typeRef(eventClass), it -> {
                it.setVisibility(JvmVisibility.PRIVATE);
                module.get(CompilationHelper.class).createAndSetInitializer(
                    it,
                    scb -> {
                        scb.add("new " + eventClass.getQualifiedName('.')
                            + "()");
                    }
                );
            }
        ));
    }


    private JvmGenericType createEventClass(
        Maybe<OnExceptionHandler> input,
        OnExceptionHandler inputSafe,
        SavedContext savedContext,
        String className
    ) {
        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        return jvmTB.toClass(
            inputSafe,
            className,
            it -> {
                it.setVisibility(JvmVisibility.PRIVATE);

                it.getMembers().add(jvmTB.toField(
                    inputSafe,
                    EXCEPTION_MATCHED_BOOL_VAR_NAME,
                    builtins.boolean_().asJvmTypeReference(),
                    itField -> {
                        itField.setVisibility(JvmVisibility.PUBLIC);
                        compilationHelper.createAndSetInitializer(
                            itField,
                            w.False::writeSonnet
                        );
                    }
                ));

                it.getMembers().add(jvmTB.toMethod(
                    inputSafe,
                    EVENT_HANDLER_STATE_RESET_METHOD_NAME,
                    builtins.javaVoid().asJvmTypeReference(),
                    itMethod -> {
                        itMethod.setVisibility(JvmVisibility.PUBLIC);
                        compilationHelper.createAndSetBody(
                            itMethod,
                            w.assign(
                                EXCEPTION_MATCHED_BOOL_VAR_NAME,
                                w.False
                            )::writeSonnet
                        );
                    }
                ));

                it.getMembers().add(jvmTB.toMethod(
                    inputSafe,
                    "handle",
                    jvm.typeRef(void.class),
                    itMethod -> {
                        itMethod.getParameters().add(jvmTB.toParameter(
                            inputSafe,
                            "__inputException",
                            jvm.typeRef(JadescriptException.class)
                        ));

                        itMethod.getParameters().add(jvmTB.toParameter(
                            inputSafe,
                            EXCEPTION_THROWER_NAME,
                            jvm.typeRef(ExceptionThrower.class)
                        ));


                        compilationHelper.createAndSetBody(
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
        Maybe<OnExceptionHandler> input,
        SavedContext saved,
        SourceCodeBuilder scb
    ) {


        module.get(ContextManager.class).restore(saved);

        final Maybe<WhenExpression> whenBody =
            input.__(OnExceptionHandler::getWhenBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);
        final Maybe<Pattern> contentPattern =
            input.__(OnExceptionHandler::getPattern);
        final Maybe<LValueExpression> pattern = contentPattern
            .__(x -> (LValueExpression) x);
        final Maybe<OptionalBlock> body = input.__(FeatureWithBody::getBody);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);


        module.get(ContextManager.class).enterProceduralFeature(
            OnExceptionHandlerWhenExpressionContext::new
        );

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        final IJadescriptType propositionUpperBound = builtins.proposition();


        IJadescriptType pattNarrowedContentType = propositionUpperBound;
        IJadescriptType wexpNarrowedContentType = propositionUpperBound;


        Function<StaticState, StaticState> prepareBodyState;
        final StaticState afterPatternDidMatch;
        String part1;
        if (pattern.isPresent()) {
            final PatternMatchHelper patternMatchHelper =
                module.get(PatternMatchHelper.class);

            PatternMatchInput<LValueExpression> patternMatchInput
                = patternMatchHelper.handlerHeader(
                propositionUpperBound,
                pattern,
                Maybe.some(ExpressionDescriptor.exceptionReference)
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
                EXCEPTION_REASON_VAR_NAME
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
                    ExpressionDescriptor.exceptionReference
                ).findFirst()
                .orElse(propositionUpperBound);

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

        final IJadescriptType finalContentType = lattice.getGLB(
            pattNarrowedContentType,
            wexpNarrowedContentType,
            TypeHelper.getNarrowedContentErrorMsg(
                pattNarrowedContentType,
                wexpNarrowedContentType
            )
        );


        scb.open(
            "if (__inputException.getReason() instanceof " +
                JvmTypeHelper.noGenericsTypeName(
                    finalContentType.compileToJavaTypeReference()
                ) + ") {");

        w.variable(
            finalContentType.compileToJavaTypeReference(),
            EXCEPTION_REASON_VAR_NAME,
            w.expr("(" + finalContentType.compileAsJavaCast() +
                " __inputException.getReason())")
        ).writeSonnet(scb);


        scb.open("if (" + compiledExpression + ") {");


        w.assign(
            EXCEPTION_MATCHED_BOOL_VAR_NAME,
            w.True
        ).writeSonnet(scb);


        StaticState preparedState = prepareBodyState.apply(
            afterWhenExprReturnedTrue
        );

        module.get(ContextManager.class).enterProceduralFeature(
            (mod, out) ->
                new OnExceptionHandlerContext(
                    mod,
                    out,
                    finalContentType
                )
        );

        StaticState inBody = StaticState.beginningOfOperation(module)
            .copyInnermostContentFrom(preparedState);

        inBody = inBody.enterScope();

        PSR<SourceCodeBuilder> bodyPSR = module.get(CompilationHelper.class)
            .compileBlockToNewSCB(inBody, body);


        scb.add(encloseInGeneralHandlerTryCatch(bodyPSR.result()));

        module.get(ContextManager.class).exit();

        scb.close("}");

        scb.close("}");

    }


    @Override
    public void validateOnEdit(
        Maybe<OnExceptionHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<WhenExpression> whenBody =
            input.__(OnExceptionHandler::getWhenBody);

        final Maybe<OptionalBlock> body = input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);
        final Maybe<LValueExpression> pattern = input
            .__(OnExceptionHandler::getPattern)
            .__(x -> (LValueExpression) x);


        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        final IJadescriptType contentUpperBound = builtins.proposition();

        module.get(ContextManager.class).enterProceduralFeature(
            OnExceptionHandlerWhenExpressionContext::new
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
                Maybe.some(ExpressionDescriptor.exceptionReference)
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
                        new ExpressionDescriptor.PropertyChain(
                            "exception"
                        )
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

        final IJadescriptType finalContentType = lattice.getGLB(
            pattNarrowedContentType,
            wexpNarrowedContentType,
            TypeHelper.getNarrowedContentErrorMsg(
                pattNarrowedContentType,
                wexpNarrowedContentType
            )
        );

        StaticState preparedState =
            prepareBodyState.apply(afterWhenExprReturnedTrue).declareName(
                ExceptionHandledContext.reasonContextGeneratedName(
                    finalContentType
                )
            );

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new OnExceptionHandlerContext(
                mod,
                out,
                finalContentType
            )
        );

        StaticState inBody = StaticState.beginningOfOperation(module)
            .copyInnermostContentFrom(preparedState);

        inBody = inBody.enterScope();

        module.get(BlockSemantics.class)
            .validateOptionalBlock(body, inBody, acceptor);

        module.get(ContextManager.class).exit();

    }


    @Override
    public void validateOnSave(
        Maybe<OnExceptionHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

    }

}
