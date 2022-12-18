package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.InvokeExpression;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.empty;
import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.result;
import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 2019-05-20.
 */
@Singleton
public class InvokeExpressionSemantics extends AssignableExpressionSemantics<InvokeExpression> {


    public InvokeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(Maybe<InvokeExpression> input) {
        final Maybe<RValueExpression> expr = input.__(InvokeExpression::getExpr);
        final List<Maybe<RValueExpression>> argValues = toListOfMaybes(input.__(InvokeExpression::getArgumentValues));

        List<ExpressionSemantics.SemanticsBoundToExpression<?>> result = new ArrayList<>();
        result.add(new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), expr));
        argValues.stream()
                .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
                .forEach(result::add);
        return result;
    }

    @Override
    public void compileAssignment(
            Maybe<InvokeExpression> input,
            String compiledExpression,
            IJadescriptType exprType,
            StatementCompilationOutputAcceptor acceptor
    ) {
        //CANNOT ASSIGN TO AN INVOKE EXPRESSION
    }

    @Override
    public void validateAssignment(
            Maybe<InvokeExpression> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        //CANNOT ASSIGN TO AN INVOKE-EXPRESSION
        //this is never called because of prior check via syntacticValidateLValue(...)
        errorNotLvalue(input, acceptor);
    }

    @Override
    public void syntacticValidateLValue(Maybe<InvokeExpression> input, ValidationMessageAcceptor acceptor) {
        errorNotLvalue(input, acceptor);
    }

    @Override
    public boolean isValidLExpr(Maybe<InvokeExpression> input) {
        //CANNOT ASSIGN TO AN INVOKE-EXPRESSION
        return false;
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<InvokeExpression> input) {
        return false;
    }

    @Override
    public ExpressionCompilationResult compile(
            Maybe<InvokeExpression> input,
            StatementCompilationOutputAcceptor acceptor
    ) {
        if (input == null) {
            return empty();
        }
        final Maybe<String> name = input.__(InvokeExpression::getName);
        final boolean isStatic = input.__(InvokeExpression::isStatic).extract(nullAsFalse);
        final Maybe<String> className = input.__(InvokeExpression::getClassName);
        final Maybe<RValueExpression> expr = input.__(InvokeExpression::getExpr);
        final boolean isArgs = input.__(InvokeExpression::isArgs).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> argValues = toListOfMaybes(input.__(InvokeExpression::getArgumentValues));
        if (name.isNothing()) {
            return empty();
        }
        if (isStatic && className.isNothing()) {
            return empty();
        }
        if (!isStatic && expr.isNothing()) {
            return empty();
        }
        if (isArgs && argValues.isEmpty()) {
            return empty();
        }

        // inside jadescript.core.Agent:
        //public Object invokeStatic(String className, String methodName, List<Class<?>> argsTypes, Object... args)...
        //public Object invokeOnInstance(Object instance, String methodName, List<Class<?>> argsTypes, Object... args)...

        StringBuilder sb = new StringBuilder("jadescript.util.InvokeUtils.");
        if (isStatic) {
            sb.append("invokeStatic(").append(className);
        } else {
            sb.append("invokeOnInstance(").append(module.get(RValueExpressionSemantics.class).compile(expr, acceptor));
        }
        sb.append(", \"").append(name).append("\", java.util.Arrays.asList(");
        for (int i = 0; i < argValues.size(); i++) {
            Maybe<RValueExpression> argumentValue = argValues.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(debox(module.get(RValueExpressionSemantics.class)
                    .inferType(argumentValue).compileToJavaTypeReference())).append(".class");
        }
        sb.append(")");
        for (Maybe<RValueExpression> argumentValue : argValues) {
            sb.append(",").append(module.get(RValueExpressionSemantics.class).compile(argumentValue, acceptor));
        }
        sb.append(")");


        return result(sb.toString());
    }

    @Override
    public IJadescriptType inferType(Maybe<InvokeExpression> input) {
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
    public boolean mustTraverse(Maybe<InvokeExpression> input) {
        return false;
    }

    @Override
    public Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<InvokeExpression> input) {
        return Optional.empty();
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<InvokeExpression, ?, ?> input, StatementCompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<InvokeExpression> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<InvokeExpression, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }

    @Override
    public void validate(Maybe<InvokeExpression> input, ValidationMessageAcceptor acceptor) {
        if (input == null) {
            return;
        }
        final boolean isArgs = input.__(InvokeExpression::isArgs).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> argValues = toListOfMaybes(input.__(InvokeExpression::getArgumentValues));

        if (isArgs) {
            for (Maybe<RValueExpression> argumentValue : argValues) {
                module.get(RValueExpressionSemantics.class).validate(argumentValue, acceptor);
            }
        }
    }

    @Override
    public boolean isAlwaysPure(Maybe<InvokeExpression> input) {
        return false; //procedures are ALWAYS impure by definition
    }
}
