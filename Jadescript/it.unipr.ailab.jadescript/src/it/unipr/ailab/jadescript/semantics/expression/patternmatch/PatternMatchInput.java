package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeRelationship;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class PatternMatchInput<
        T,
        U extends PatternMatchOutput.Unification,
        N extends PatternMatchOutput.TypeNarrowing> {

    protected final SemanticsModule module;
    private final PatternMatchMode patternMatchMode;
    private final Maybe<T> pattern;
    private final String termID;

    private final String rootPatternMatchVariableName;

    public PatternMatchInput(
            SemanticsModule module,
            PatternMatchMode patternMatchMode,
            Maybe<T> pattern,
            String termID,
            String rootPatternMatchVariableName
    ) {
        this.module = module;
        this.patternMatchMode = patternMatchMode;
        this.pattern = pattern;
        this.termID = termID;
        this.rootPatternMatchVariableName = rootPatternMatchVariableName;
    }

    public PatternMatchMode getMode() {
        return patternMatchMode;
    }

    public String getRootPatternMatchVariableName() {
        return rootPatternMatchVariableName;
    }

    public String getTermID() {
        return termID;
    }

    public Maybe<T> getPattern() {
        return pattern;
    }

    public abstract <R> PatternMatchInput<R, U, N> mapPattern(
            Function<T, R> function
    );


    public abstract IJadescriptType providedInputType();


    public <T2> SubPattern<T2, T, U, N> subPattern(
            IJadescriptType providedInputType,
            Function<T, T2> extractSubpattern,
            String idSuffix
    ) {
        return new SubPattern<>(
                module,
                providedInputType,
                this,
                pattern.__(extractSubpattern),
                idSuffix
        );
    }

    public <T2> SubPattern<T2, T, U, N> subPatternGroundTerm(
            IJadescriptType providedInputType,
            Function<T, T2> extractSubpattern,
            String idSuffix
    ) {
        return new SubPattern<>(
                module,
                providedInputType,
                this,
                pattern.__(extractSubpattern),
                idSuffix,
                PatternMatchMode.HolesAndGroundness.DOES_NOT_ACCEPT_HOLES
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, ?, ?> createEmptyCompileOutput() {
        return new PatternMatchOutput<>(
                new PatternMatchSemanticsProcess.IsCompilation.AsEmpty(this),
                getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                        ? PatternMatchOutput.EMPTY_UNIFICATION
                        : PatternMatchOutput.NoUnification.INSTANCE,
                getMode().getNarrowsTypeOfInput() == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                        ? new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).ANY)
                        : PatternMatchOutput.NoNarrowing.INSTANCE
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> createEmptyValidationOutput() {
        return new PatternMatchOutput<>(
                PatternMatchSemanticsProcess.IsValidation.INSTANCE,
                getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                        ? PatternMatchOutput.EMPTY_UNIFICATION
                        : PatternMatchOutput.NoUnification.INSTANCE,
                getMode().getNarrowsTypeOfInput() == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                        ? new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).ANY)
                        : PatternMatchOutput.NoNarrowing.INSTANCE
        );
    }

    public <P extends PatternMatchSemanticsProcess>
    PatternMatchOutput<P, ?, ?> createOutput(
            P processInfo,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {


        return new PatternMatchOutput<>(
                processInfo,
                this.getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                        ? getUnificationInfo.get()
                        : PatternMatchOutput.NoUnification.INSTANCE,
                this.getMode().getNarrowsTypeOfInput() == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                        ? getNarrowingInfo.get()
                        : PatternMatchOutput.NoNarrowing.INSTANCE
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            IJadescriptType solvedPatternType,
            List<String> additionalPreconditions,
            Function<Integer, String> compiledSubInputs,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        solvedPatternType,
                        additionalPreconditions,
                        compiledSubInputs
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            IJadescriptType solvedPatternType,
            Function<Integer, String> compiledSubInputs,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        solvedPatternType,
                        compiledSubInputs
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            IJadescriptType solvedPatternType,
            List<String> additionalPreconditions,
            Function<Integer, String> compiledSubInputs,
            List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        solvedPatternType,
                        additionalPreconditions,
                        compiledSubInputs,
                        subResults
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            IJadescriptType solvedPatternType,
            Function<Integer, String> compiledSubInputs,
            List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        solvedPatternType,
                        compiledSubInputs,
                        subResults
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }
    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            List<StatementWriter> auxiliaryStatements,
            IJadescriptType solvedPatternType,
            List<String> additionalPreconditions,
            Function<Integer, String> compiledSubInputs,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        auxiliaryStatements,
                        solvedPatternType,
                        additionalPreconditions,
                        compiledSubInputs
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            List<StatementWriter> auxiliaryStatements,
            IJadescriptType solvedPatternType,
            Function<Integer, String> compiledSubInputs,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        auxiliaryStatements,
                        solvedPatternType,
                        compiledSubInputs
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            List<StatementWriter> auxiliaryStatements,
            IJadescriptType solvedPatternType,
            List<String> additionalPreconditions,
            Function<Integer, String> compiledSubInputs,
            List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        auxiliaryStatements,
                        solvedPatternType,
                        additionalPreconditions,
                        compiledSubInputs,
                        subResults
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod, ?, ?>
    createCompositeMethodOutput(
            List<StatementWriter> auxiliaryStatements,
            IJadescriptType solvedPatternType,
            Function<Integer, String> compiledSubInputs,
            List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                        this,
                        auxiliaryStatements,
                        solvedPatternType,
                        compiledSubInputs,
                        subResults
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsSingleConditionMethod, ?, ?>
    createSingleConditionMethodOutput(
            IJadescriptType solvedPatternType,
            String condition,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsSingleConditionMethod(
                        this,
                        solvedPatternType,
                        condition
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsInlineCondition, ?, ?>
    createInlineConditionOutput(
            Function<String, String> generateCondition,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsInlineCondition(this) {
                    @Override
                    public String compileOperationInvocation(String input) {
                        return generateCondition.apply(input);
                    }
                },
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation.AsFieldAssigningMethod, ?, ?>
    createFieldAssigningMethodOutput(
            IJadescriptType solvedPatternType,
            String fieldName,
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                new PatternMatchSemanticsProcess.IsCompilation.AsFieldAssigningMethod(
                        this,
                        solvedPatternType,
                        fieldName
                ),
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?>
    createValidationOutput(
            Supplier<? extends PatternMatchOutput.Unification> getUnificationInfo,
            Supplier<? extends PatternMatchOutput.TypeNarrowing> getNarrowingInfo
    ) {
        return createOutput(
                PatternMatchSemanticsProcess.IsValidation.INSTANCE,
                getUnificationInfo,
                getNarrowingInfo
        );
    }

    public static class WhenMatchesStatement<T>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public WhenMatchesStatement(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_ANY_HOLE,
                    TypeRelationship.Related.class,
                    PatternMatchMode.PatternApplicationPurity.IMPURE_OK,
                    PatternMatchMode.Unification.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.STATEMENT_GUARD
            ), pattern, termID, rootPatternMatchVariableName);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R> WhenMatchesStatement<R> mapPattern(Function<T, R> function) {
            return new WhenMatchesStatement<>(
                    module,
                    inputExpr,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }


        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }


    }

    public static class HandlerHeader<T>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final IJadescriptType contentUpperBound;
        private final String referenceToContent;

        public HandlerHeader(
                SemanticsModule module,
                IJadescriptType contentUpperBound,
                String referenceToContent,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_ANY_HOLE,
                    TypeRelationship.SubtypeOrEqual.class,
                    PatternMatchMode.PatternApplicationPurity.HAS_TO_BE_PURE,
                    PatternMatchMode.Unification.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.FEATURE_HEADER
            ), pattern, termID, rootPatternMatchVariableName);
            this.referenceToContent = referenceToContent;
            this.contentUpperBound = contentUpperBound;
        }

        @Override
        public <R> HandlerHeader<R> mapPattern(Function<T, R> function) {
            return new HandlerHeader<>(
                    module,
                    contentUpperBound,
                    referenceToContent,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }

        @Override
        public IJadescriptType providedInputType() {
            return contentUpperBound;
        }


    }

    public static class MatchesExpression<T>
            extends PatternMatchInput<T, PatternMatchOutput.NoUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public MatchesExpression(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_NONVAR_HOLES_ONLY,
                    TypeRelationship.Related.class,
                    PatternMatchMode.PatternApplicationPurity.IMPURE_OK,
                    PatternMatchMode.Unification.WITHOUT_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.BOOLEAN_EXPRESSION
            ), pattern, termID, rootPatternMatchVariableName);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R> MatchesExpression<R> mapPattern(Function<T, R> function) {
            return new MatchesExpression<>(
                    module,
                    inputExpr,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }

        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }


    }

    public static class AssignmentDeconstruction<T>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.NoNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public AssignmentDeconstruction(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.REQUIRES_FREE_VARS,
                    TypeRelationship.SupertypeOrEqual.class,
                    PatternMatchMode.PatternApplicationPurity.IMPURE_OK,
                    PatternMatchMode.Unification.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.DOES_NOT_NARROW_TYPE,
                    PatternMatchMode.PatternLocation.ROOT_OF_ASSIGNED_EXPRESSION
            ), pattern, termID, rootPatternMatchVariableName);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R> AssignmentDeconstruction<R> mapPattern(Function<T, R> function) {
            return new AssignmentDeconstruction<>(
                    module,
                    inputExpr,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }


        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }


    }


    public static class SubPattern<T, RT,
            U extends PatternMatchOutput.Unification,
            N extends PatternMatchOutput.TypeNarrowing> extends PatternMatchInput<T, U, N> {

        private final PatternMatchInput<RT, U, N> rootInput;
        private final String suffixID;

        private final IJadescriptType inputInfo;

        private SubPattern(
                SemanticsModule module,
                IJadescriptType inputInfo,
                PatternMatchInput<RT, U, N> rootInput,
                Maybe<T> pattern,
                String suffixID
        ) {
            this(module, inputInfo, rootInput, pattern, suffixID, null);
        }

        private SubPattern(
                SemanticsModule module,
                IJadescriptType inputInfo,
                PatternMatchInput<RT, U, N> rootInput,
                Maybe<T> pattern,
                String suffixID,
                PatternMatchMode.HolesAndGroundness holesAndGroundnessRequirement
        ) {
            super(module, new PatternMatchMode(
                    holesAndGroundnessRequirement == null
                            ? rootInput.getMode().getHolesAndGroundness()
                            : holesAndGroundnessRequirement,
                    // Subpatterns always have a "related" requirement, except when in assignment/declarations.
                    rootInput.getMode().getPatternLocation() == PatternMatchMode.PatternLocation.ROOT_OF_ASSIGNED_EXPRESSION
                            ? TypeRelationship.SupertypeOrEqual.class
                            : TypeRelationship.Related.class,
                    rootInput.getMode().getPatternApplicationPurity(),
                    rootInput.getMode().getUnification(),
                    rootInput.getMode().getNarrowsTypeOfInput(),
                    PatternMatchMode.PatternLocation.SUB_PATTERN
            ), pattern, rootInput.termID + suffixID, rootInput.getRootPatternMatchVariableName());
            this.rootInput = rootInput;
            this.suffixID = suffixID;
            this.inputInfo = inputInfo;
        }


        public PatternMatchInput<RT, U, N> getRootInput() {
            return rootInput;
        }

        @Override
        public <R> SubPattern<R, RT, U, N> mapPattern(Function<T, R> function) {
            return new SubPattern<>(module, inputInfo, rootInput, getPattern().__(function), suffixID);
        }

        @Override
        public IJadescriptType providedInputType() {
            return inputInfo;
        }


    }


}
