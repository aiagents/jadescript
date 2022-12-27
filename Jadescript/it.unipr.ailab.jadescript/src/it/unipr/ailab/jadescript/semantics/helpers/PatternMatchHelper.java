package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.UnaryPrefixExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public class PatternMatchHelper implements SemanticsConsts {
    public static WriterFactory w = WriterFactory.getInstance();

    private final SemanticsModule module;

    public PatternMatchHelper(SemanticsModule module) {
        this.module = module;
    }

    public PatternMatchOutput<
            ? extends PatternMatchSemanticsProcess.IsCompilation,
            PatternMatchOutput.DoesUnification,
            PatternMatchOutput.WithTypeNarrowing>
    compileWhenMatchesStatementPatternMatching(
            Maybe<RValueExpression> inputExpr,
            Maybe<LValueExpression> pattern,
            CompilationOutputAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.WhenMatchesStatement<LValueExpression> patternMatchInput =
                new PatternMatchInput.WhenMatchesStatement<>(
                        module,
                        inputExpr,
                        pattern,
                        "__",
                        variableName
                );
        final PatternMatchOutput<
                ? extends PatternMatchSemanticsProcess.IsCompilation,
                PatternMatchOutput.DoesUnification,
                PatternMatchOutput.WithTypeNarrowing> output =
                module.get(LValueExpressionSemantics.class).compilePatternMatch(
                        patternMatchInput,
                        acceptor
                );


        final LocalClassStatementWriter localClass = w.localClass(localClassName);

        output.getProcessInfo().getWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(w.variable(localClassName, variableName, w.expr("new " + localClassName + "()")));
        return output;
    }

    public PatternMatchOutput<
            ? extends PatternMatchSemanticsProcess.IsCompilation,
            PatternMatchOutput.NoUnification,
            PatternMatchOutput.WithTypeNarrowing>
    compileMatchesExpressionPatternMatching(
            Maybe<UnaryPrefix> inputExpr,
            Maybe<LValueExpression> pattern,
            CompilationOutputAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.MatchesExpression<LValueExpression> patternMatchInput =
                new PatternMatchInput.MatchesExpression<>(
                        module,
                        inputExpr,
                        pattern,
                        "__",
                        variableName
                );
        final PatternMatchOutput<
                ? extends PatternMatchSemanticsProcess.IsCompilation,
                PatternMatchOutput.NoUnification,
                PatternMatchOutput.WithTypeNarrowing> output =
                module.get(LValueExpressionSemantics.class).compilePatternMatch(
                        patternMatchInput,
                        acceptor
                );


        final LocalClassStatementWriter localClass = w.localClass(localClassName);

        output.getProcessInfo().getWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(w.variable(localClassName, variableName, w.expr("new " + localClassName + "()")));
        return output;
    }

    public PatternMatchOutput<
            ? extends PatternMatchSemanticsProcess.IsCompilation,
            PatternMatchOutput.DoesUnification,
            PatternMatchOutput.WithTypeNarrowing>
    compileHeaderPatternMatching(
            IJadescriptType contentUpperBound,
            String referenceToContent,
            Maybe<LValueExpression> pattern,
            CompilationOutputAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.HandlerHeader<LValueExpression> patternMatchInput =
                new PatternMatchInput.HandlerHeader<>(
                        module,
                        contentUpperBound,
                        referenceToContent,
                        pattern,
                        "__",
                        variableName
                );
        final PatternMatchOutput<
                ? extends PatternMatchSemanticsProcess.IsCompilation,
                PatternMatchOutput.DoesUnification,
                PatternMatchOutput.WithTypeNarrowing> output =
                module.get(LValueExpressionSemantics.class).compilePatternMatch(
                        patternMatchInput,
                        acceptor
                );


        final LocalClassStatementWriter localClass = w.localClass(localClassName);

        output.getProcessInfo().getWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(w.variable(localClassName, variableName, w.expr("new " + localClassName + "()")));
        return output;
    }



    public PatternMatchOutput<
            ? extends PatternMatchSemanticsProcess.IsValidation,
            PatternMatchOutput.DoesUnification,
            PatternMatchOutput.WithTypeNarrowing>
    validateWhenMatchesStatementPatternMatching(
            Maybe<RValueExpression> inputExpr,
            Maybe<LValueExpression> pattern,
            ValidationMessageAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.WhenMatchesStatement<LValueExpression> patternMatchInput =
                new PatternMatchInput.WhenMatchesStatement<>(
                        module,
                        inputExpr,
                        pattern,
                        "__",
                        variableName
                );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
                patternMatchInput,
                acceptor
        );
    }

    public PatternMatchOutput<
            ? extends PatternMatchSemanticsProcess.IsValidation,
            PatternMatchOutput.NoUnification,
            PatternMatchOutput.WithTypeNarrowing>
    validateMatchesExpressionPatternMatching(
            Maybe<UnaryPrefix> inputExpr,
            Maybe<LValueExpression> pattern,
            ValidationMessageAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.MatchesExpression<LValueExpression> patternMatchInput =
                new PatternMatchInput.MatchesExpression<>(
                        module,
                        inputExpr,
                        pattern,
                        "__",
                        variableName
                );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
                patternMatchInput,
                acceptor
        );
    }

    public PatternMatchOutput<
            ? extends PatternMatchSemanticsProcess.IsValidation,
            PatternMatchOutput.DoesUnification,
            PatternMatchOutput.WithTypeNarrowing>
    validateHeaderPatternMatching(
            IJadescriptType contentUpperBound,
            String referenceToContent,
            Maybe<LValueExpression> pattern,
            ValidationMessageAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.HandlerHeader<LValueExpression> patternMatchInput =
                new PatternMatchInput.HandlerHeader<>(
                        module,
                        contentUpperBound,
                        referenceToContent,
                        pattern,
                        "__",
                        variableName
                );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
                patternMatchInput,
                acceptor
        );
    }


    public IJadescriptType inferMatchesExpressionPatternType(
            Maybe<LValueExpression> pattern,
            Maybe<UnaryPrefix> unary
    ) {
        return module.get(LValueExpressionSemantics.class).inferPatternType(
                pattern,
                PatternMatchInput.MatchesExpression.MODE
        ).solve(module.get(UnaryPrefixExpressionSemantics.class).inferType(unary));
    }
}
