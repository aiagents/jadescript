package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import java.util.function.Function;

public abstract class PatternMatchInput<
        T extends EObject,
        U extends PatternMatchOutput.Unification,
        N extends PatternMatchOutput.TypeNarrowing> {

    protected final SemanticsModule module;
    private final PatternMatchMode patternMatchMode;
    private final Maybe<T> pattern;

    public PatternMatchInput(SemanticsModule module, PatternMatchMode patternMatchMode, Maybe<T> pattern) {
        this.module = module;
        this.patternMatchMode = patternMatchMode;
        this.pattern = pattern;
    }

    public PatternMatchMode getPatternMatchMode() {
        return patternMatchMode;
    }

    public Maybe<T> getPattern() {
        return pattern;
    }

    public abstract <R extends EObject> PatternMatchInput<R, U, N> mapPattern(
            Function<T, R> function
    );

    public abstract IJadescriptType providedInputType();

    public abstract Maybe<String> compiledInput();

    public static class WhenStatementPatternMatchInput<T extends EObject>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public WhenStatementPatternMatchInput(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_FREE_VARS,
                    PatternMatchMode.TypeRelationship.Related.class,
                    PatternMatchMode.Deconstruction.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.STATEMENT_GUARD
            ), pattern);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R extends EObject> WhenStatementPatternMatchInput<R> mapPattern(Function<T, R> function) {
            return new WhenStatementPatternMatchInput<>(
                    module,
                    inputExpr,
                    getPattern().__(function)
            );
        }

        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }

        @Override
        public Maybe<String> compiledInput() {
            return module.get(RValueExpressionSemantics.class).compile(inputExpr);
        }
    }

    public static class HandlerHeaderPatternInput<T extends EObject>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final IJadescriptType contentUpperBound;
        private final String referenceToContent;

        public HandlerHeaderPatternInput(
                SemanticsModule module,
                IJadescriptType contentUpperBound,
                String referenceToContent,
                Maybe<T> pattern
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_FREE_VARS,
                    PatternMatchMode.TypeRelationship.SubtypeOrEqual.class,
                    PatternMatchMode.Deconstruction.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.FEATURE_HEADER
            ), pattern);
            this.referenceToContent = referenceToContent;
            this.contentUpperBound = contentUpperBound;
        }

        @Override
        public <R extends EObject> HandlerHeaderPatternInput<R> mapPattern(Function<T, R> function) {
            return new HandlerHeaderPatternInput<>(
                    module,
                    contentUpperBound,
                    referenceToContent,
                    getPattern().__(function)
            );
        }

        @Override
        public IJadescriptType providedInputType() {
            return contentUpperBound;
        }

        @Override
        public Maybe<String> compiledInput() {
            return Maybe.of(referenceToContent);
        }

    }

    public static class MatchesExpression<T extends EObject>
            extends PatternMatchInput<T, PatternMatchOutput.NoUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public MatchesExpression(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_NONVAR_HOLES_ONLY,
                    PatternMatchMode.TypeRelationship.Related.class,
                    PatternMatchMode.Deconstruction.WITHOUT_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.BOOLEAN_EXPRESSION
            ), pattern);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R extends EObject> MatchesExpression<R> mapPattern(Function<T, R> function) {
            return new MatchesExpression<>(
                    module,
                    inputExpr,
                    getPattern().__(function)
            );
        }

        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }

        @Override
        public Maybe<String> compiledInput() {
            return module.get(RValueExpressionSemantics.class).compile(inputExpr);
        }
    }

    public static class AssignmentDeconstruction<T extends EObject>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.NoNarrowing> {
        private final Maybe<RValueExpression> inputExpr;
        private final PatternMatchMode.PatternLocation.AssignedExpression location;

        public AssignmentDeconstruction(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern,
                PatternMatchMode.PatternLocation.AssignedExpression location
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_NONVAR_HOLES_ONLY,
                    PatternMatchMode.TypeRelationship.SupertypeOrEqual.class,
                    PatternMatchMode.Deconstruction.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.DOES_NOT_NARROW_TYPE,
                    location
            ), pattern);
            this.inputExpr = inputExpr;
            this.location = location;
        }

        @Override
        public <R extends EObject> AssignmentDeconstruction<R> mapPattern(Function<T, R> function) {
            return new AssignmentDeconstruction<>(
                    module,
                    inputExpr,
                    getPattern().__(function),
                    location
            );
        }


        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }

        @Override
        public Maybe<String> compiledInput() {
            return module.get(RValueExpressionSemantics.class).compile(inputExpr);
        }

        public PatternMatchMode.PatternLocation.AssignedExpression getLocation() {
            return location;
        }
    }
}
