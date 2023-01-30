package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.InvokeExpression;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

/**
 * Created on 2019-05-20.
 */
@Singleton
//TODO ICAART23 updates
public class InvokeExpressionSemantics
    extends AssignableExpressionSemantics<InvokeExpression> {


    public InvokeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<InvokeExpression> input
    ) {
        final Maybe<RValueExpression> expr =
            input.__(InvokeExpression::getExpr);

        final List<Maybe<RValueExpression>> argValues =
            toListOfMaybes(input.__(InvokeExpression::getArgumentValues));

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        return Stream.concat(Stream.of(expr), argValues.stream())
            .filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(rves, i));

    }


    @Override
    public void compileAssignmentInternal(
        Maybe<InvokeExpression> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        //CANNOT ASSIGN TO AN INVOKE EXPRESSION
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<InvokeExpression> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        //CANNOT ASSIGN TO AN INVOKE EXPRESSION
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<InvokeExpression> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        //CANNOT ASSIGN TO AN INVOKE-EXPRESSION
        //this is never called because of prior check via
        // syntacticValidateLValue(...)
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<InvokeExpression> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<InvokeExpression> input) {
        //CANNOT ASSIGN TO AN INVOKE-EXPRESSION
        return false;
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<InvokeExpression> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<InvokeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        final Maybe<String> name = input.__(InvokeExpression::getName);
        final boolean isStatic = input.__(InvokeExpression::isStatic)
            .extract(nullAsFalse);
        final Maybe<String> className = input
            .__(InvokeExpression::getClassName);
        final Maybe<RValueExpression> expr = input
            .__(InvokeExpression::getExpr);
        final boolean isArgs = input.__(InvokeExpression::isArgs)
            .extract(nullAsFalse);
        final List<Maybe<RValueExpression>> argValues = toListOfMaybes(
            input.__(InvokeExpression::getArgumentValues)
        );
        if (name.isNothing()
            || isStatic && className.isNothing()
            || !isStatic && expr.isNothing()
            || isArgs && argValues.isEmpty()) {
            return state;
        }
        StaticState newState;
        if (isStatic) {
            newState = state;
        } else {
            newState = module.get(RValueExpressionSemantics.class)
                .advance(expr, state);
        }

        for (Maybe<RValueExpression> argValue : argValues) {
            newState = module.get(RValueExpressionSemantics.class).advance(
                argValue,
                newState
            );
        }

        return newState;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<InvokeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected String compileInternal(
        Maybe<InvokeExpression> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final Maybe<String> name = input.__(InvokeExpression::getName);
        final boolean isStatic = input.__(InvokeExpression::isStatic)
            .extract(nullAsFalse);
        final Maybe<String> className = input
            .__(InvokeExpression::getClassName);
        final Maybe<RValueExpression> expr = input
            .__(InvokeExpression::getExpr);
        final boolean isArgs = input.__(InvokeExpression::isArgs)
            .extract(nullAsFalse);
        final List<Maybe<RValueExpression>> argValues = toListOfMaybes(
            input.__(InvokeExpression::getArgumentValues)
        );
        if (name.isNothing()
            || isStatic && className.isNothing()
            || !isStatic && expr.isNothing()
            || isArgs && argValues.isEmpty()) {
            return "";
        }

        // inside jadescript.core.Agent:
        //public Object invokeStatic(
        //    String className,
        //    String methodName,
        //    List<Class<?>> argsTypes,
        //    Object... args
        //)...
        //public Object invokeOnInstance(
        //    Object instance,
        //    String methodName,
        //    List<Class<?>> argsTypes,
        //    Object... args
        //)...

        StringBuilder sb = new StringBuilder("jadescript.util.InvokeUtils.");
        StaticState newState;
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        if (isStatic) {
            sb.append("invokeStatic(").append(className);
            newState = state;
        } else {
            final String instanceCompiled = rves.compile(
                expr,
                state,
                acceptor
            );
            newState = rves.advance(
                expr,
                state
            );
            sb.append("invokeOnInstance(").append(instanceCompiled);
        }
        sb.append(", \"").append(name).append("\", java.util.Arrays.asList(");

        List<String> argClasses = new ArrayList<>();
        List<String> compiledArgs = new ArrayList<>();

        for (int i = 0; i < argValues.size(); i++) {
            Maybe<RValueExpression> argumentValue = argValues.get(i);

            final IJadescriptType argType =
                rves.inferType(argumentValue, newState);

            final String argClass = debox(argType.compileToJavaTypeReference())
                + ".class";

            argClasses.add(argClass);

            final String argCompiled = rves.compile(
                argumentValue,
                newState,
                acceptor
            );

            compiledArgs.add(argCompiled);

            if (i < argValues.size() - 1) { //Excluding last
                newState = rves.advance(
                    argumentValue,
                    newState
                );
            }
        }
        sb.append(String.join(", ", argClasses)).append(")");

        if(!compiledArgs.isEmpty()){
            sb.append(",");
        }

        sb.append(String.join(", ", compiledArgs));

        sb.append(")");

        return sb.toString();
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<InvokeExpression> input
        , StaticState state
    ) {
        return module.get(TypeHelper.class).ANY;
    }


    public String debox(String typeName) {
        switch (typeName) {
            case "java.lang.Integer":
                return "int";
            case "java.lang.Boolean":
                return "boolean";
            case "java.lang.Double":
                return "double";
            case "java.lang.Byte":
                return "byte";
            case "java.lang.Float":
                return "float";
            case "java.lang.Character":
                return "char";
            case "java.lang.Long":
                return "long";
            case "java.lang.Short":
                return "short";

            default:
                return typeName;
        }
    }


    @Override
    protected boolean mustTraverse(Maybe<InvokeExpression> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<InvokeExpression> input) {
        return Optional.empty();
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<InvokeExpression> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<InvokeExpression> input
        , StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<InvokeExpression> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean validateInternal(
        Maybe<InvokeExpression> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }
        final boolean isStatic = input.__(InvokeExpression::isStatic)
            .extract(nullAsFalse);

        final Maybe<RValueExpression> expr = input
            .__(InvokeExpression::getExpr);

        StaticState runningState;
        boolean result = VALID;
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        if (isStatic) {
            runningState = state;
        } else {
            result = rves.validate(expr, state, acceptor);
            runningState = rves.advance(expr, state);
        }

        final boolean isArgs =
            input.__(InvokeExpression::isArgs).extract(nullAsFalse);

        if (isArgs) {
            final List<Maybe<RValueExpression>> argValues =
                toListOfMaybes(input.__(InvokeExpression::getArgumentValues));
            for (Maybe<RValueExpression> argumentValue : argValues) {
                final boolean argCheck = rves.validate(
                    argumentValue,
                    runningState,
                    acceptor
                );
                result = result && argCheck;
                runningState = rves.advance(expr, runningState);
            }
        }
        return result;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        //procedures are always IMPURE by definition
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<InvokeExpression> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<InvokeExpression> input) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<InvokeExpression> input,
        StaticState state
    ) {
        return false;
    }


}
