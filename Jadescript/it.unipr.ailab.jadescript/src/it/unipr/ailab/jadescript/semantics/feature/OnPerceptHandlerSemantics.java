package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnPerceptHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnPerceptHandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.PerceptPerceivedContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
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

import static it.unipr.ailab.maybe.Maybe.*;

public class OnPerceptHandlerSemantics
    extends FeatureSemantics<OnPerceptHandler> {

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

        if (input.isNothing()) {
            return;
        }
        OnPerceptHandler inputSafe = input.toNullable();

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
        OnPerceptHandler inputSafe,
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
        Maybe<OnPerceptHandler> input,
        OnPerceptHandler inputSafe,
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
        Maybe<OnPerceptHandler> input,
        SavedContext savedContext,
        SourceCodeBuilder scb
    ) {
//        if ([OUTERCLASS].this.__ignoreMessageHandlers) {
//            this.__eventFired = false;
//            return;
//        }
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


//        [... message template auxiliary statements...]
//        MessageTemplate _mt = [...]

        final Maybe<WhenExpression> whenBody =
            input.__(OnPerceptHandler::getWhenBody);

        final Maybe<CodeBlock> body =
            input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);

        module.get(ContextManager.class).restore(savedContext);
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        module.get(ContextManager.class).enterProceduralFeature(
            OnPerceptHandlerWhenExpressionContext::new
        );

        StaticState beforePattern = StaticState.beginningOfOperation(module);

        final IJadescriptType contentUpperBound = typeHelper.PROPOSITION;

        IJadescriptType pattNarrowedContentType = contentUpperBound;
        IJadescriptType wexpNarrowedContentType = contentUpperBound;

        final Maybe<LValueExpression> pattern = input
            .__(OnPerceptHandler::getPattern)
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

            part1 = matcher.operationInvocationText(PERCEPT_CONTENT_VAR_NAME);
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
                            "percept"
                        )
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

        // add "is a percept" constraint
        templateExpressions.add(TemplateCompilationHelper.isPercept());

        if (input.__(OnPerceptHandler::isStale).extract(nullAsFalse)) {
            // add staleness constraint
            templateExpressions.add(TemplateCompilationHelper.isStale());
        }


        final BlockWriter tryBlock = w.block();
        tryBlock.addStatement(w.ifStmnt(
            w.expr("!(((jadescript.core.percept.Percept) " + THE_AGENT + "()" +
                ".getContentManager()" +
                ".extractContent(" + PERCEPT_VAR_NAME + "))" +
                ".getContent() instanceof " +
                module.get(TypeHelper.class).noGenericsTypeName(
                    finalContentType.compileToJavaTypeReference()
                ) + ")"),
            w.block().addStatement(w.returnStmnt(w.expr("false")))
        ));

        final VariableDeclarationWriter declarePerceptContentVariable =
            w.variable(
                finalContentType.compileToJavaTypeReference(),
                PERCEPT_CONTENT_VAR_NAME,
                w.expr(finalContentType.compileAsJavaCast() +
                    "((jadescript.core.percept.Percept)"
                    + THE_AGENT + "().getContentManager()" +
                    ".extractContent(" + PERCEPT_VAR_NAME + "))" +
                    ".getContent()"
                )
            );

        templateExpressions.add(TemplateCompilationHelper.customPercept(
            w.block().addStatement(
                w.tryCatch(tryBlock
                    .addStatements(declarePerceptContentVariable)
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

//        if(myAgent!=null) {
//            __receivedPercept = jadescript.core.message.Message.wrap
//                (myAgent.receive(__mt));
//        }
        w.ifStmnt(
            w.expr("myAgent!=null"),
            w.block().addStatement(
                w.assign(
                    PERCEPT_VAR_NAME,
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


        StaticState inBody = prepareBodyState.apply(
            afterWhenExprRetunedTrue
        ).assertNamedSymbol(
            PerceptPerceivedContext.perceptContentContextGeneratedReference(
                finalContentType
            )
        );

        module.get(ContextManager.class).enterProceduralFeature(
            (mod, out) -> new OnPerceptHandlerContext(
                mod,
                out,
                "percept",
                finalContentType
            )
        );

        inBody = inBody.enterScope();
        final PSR<SourceCodeBuilder> bodyPSR =
            module.get(CompilationHelper.class).compileBlockToNewSCB(
                inBody,
                body
            );

        final StatementWriter tryCatchWrappedBody =
            encloseInGeneralHandlerTryCatch(bodyPSR.result());


        module.get(ContextManager.class).exit();

//        if (__receivedPercept != null) {
//            [OUTERCLASS].this.__ignoreMessageHandlers = true;
//
//            __theAgent().__cleanIgnoredFlagForMessage(__receivedPercept);

//            this.__eventFired = true;
//
//            try {
//                [CONTENT_TYPE] __perceptContent = ([CONTENT_TYPE])
//                    ((jadescript.core.percept.Percept)__theAgent()
//                        .getContentManager()
//                        .extractContent(__receivedPercept)
//                    ).getContent();
//                try {
//
//                    [...USERCODE...]

//                } catch (jadescript.core.exception.JadescriptException
//                    __throwable) {
//                    __handleJadescriptException(__throwable);
//                } catch (java.lang.Throwable __throwable) {
//                    __handleJadescriptException(jadescript.core.exception
//                    .JadescriptException.wrap(__throwable));
//                }
//
//                this.__receivedPercept = null;
//            } catch (Exception _e) {
//                _e.printStackTrace();
//            }
//        } else {
//            this.__eventFired = false;
//        }

        w.ifStmnt(
            w.expr(PERCEPT_VAR_NAME + " != null"),
            w.block()
                .addStatement(w.assign(
                    Util.getOuterClassThisReference(input) + "."
                        + IGNORE_MSG_HANDLERS_VAR_NAME,
                    w.expr("true")
                ))
                .addStatement(w.callStmnt(
                    THE_AGENT + "().__cleanIgnoredFlagForMessage",
                    w.expr(PERCEPT_VAR_NAME)
                ))
                .addStatement(w.assign(
                    "this." + MESSAGE_RECEIVED_BOOL_VAR_NAME,
                    w.expr("true")
                ))
                .addStatement(w.tryCatch(w.block()
                        .addStatement(declarePerceptContentVariable)
                        .addStatement(tryCatchWrappedBody)
                        .addStatement(w.assign(
                            "this." + PERCEPT_VAR_NAME,
                            w.expr("null")
                        ))
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
    public void validateFeature(
        Maybe<OnPerceptHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<WhenExpression> whenBody =
            input.__(OnPerceptHandler::getWhenBody);

        final Maybe<CodeBlock> body =
            input.__(FeatureWithBody::getBody);
        final Maybe<RValueExpression> whenExpr =
            whenBody.__(WhenExpression::getExpr);
        final Maybe<LValueExpression> pattern = input
            .__(OnPerceptHandler::getPattern)
            .__(x -> (LValueExpression) x);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final IJadescriptType contentUpperBound = typeHelper.PROPOSITION;

        module.get(ContextManager.class).enterProceduralFeature(
            OnPerceptHandlerWhenExpressionContext::new
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
                            new ExpressionDescriptor.PropertyChain(
                                "percept"
                            )
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

        StaticState inBody = prepareBodyState.apply(afterWhenExprReturnedTrue)
            .assertNamedSymbol(PerceptPerceivedContext
                .perceptContentContextGeneratedReference(
                    finalContentType
                )
            );

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new OnPerceptHandlerContext(
                mod,
                out,
                "percept",
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
