package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static it.unipr.ailab.maybe.Maybe.some;

public abstract class PatternMatchInput<T> implements SemanticsConsts {

    protected final SemanticsModule module;
    private final PatternMatchMode patternMatchMode;
    private final Maybe<T> pattern;
    private final String termID;
    private final String rootPatternMatchVariableName;
    private final Maybe<ExpressionDescriptor> descriptorMaybe;



    public PatternMatchInput(
        SemanticsModule module,
        PatternMatchMode patternMatchMode,
        Maybe<T> pattern,
        String termID,
        String rootPatternMatchVariableName,
        Maybe<ExpressionDescriptor> descriptorMaybe
    ) {
        this.module = module;
        this.patternMatchMode = patternMatchMode;
        this.pattern = pattern;
        this.termID = termID;
        this.rootPatternMatchVariableName = rootPatternMatchVariableName;
        this.descriptorMaybe = descriptorMaybe;
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

    public Maybe<ExpressionDescriptor> getInputDescriptor(){
        return descriptorMaybe;
    }


    public abstract <R> PatternMatchInput<R> mapPattern(
        Function<T, R> function
    );


    public <R> PatternMatchInput<R> replacePattern(
        Maybe<R> pattern
    ) {
        return this.mapPattern(__ -> pattern.toNullable());
    }


    public abstract IJadescriptType getProvidedInputType();


    public <T2> SubPattern<T2, T> subPattern(
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
            (__) -> Maybe.nothing()
        );
    }

    public <T2> SubPattern<T2, T> subPatternForProperty(
        IJadescriptType providedInputType,
        Function<T, T2> extractSubpattern,
        String idSuffix,
        String propertyName
    ) {
        return new SubPattern<>(
            module,
            providedInputType,
            this,
            pattern.__(extractSubpattern),
            idSuffix,
            (owner) -> some(owner.descriptorOfMemberProperty(propertyName))
        );
    }


    public <T2> SubPattern<T2, T> subPatternGroundTerm(
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
            PatternMatchMode.HolesAndGroundness.DOES_NOT_ACCEPT_HOLES,
            (__) -> Maybe.nothing()
        );
    }




    public PatternMatcher createEmptyCompileOutput() {
        return new PatternMatcher.AsEmpty(this);
    }


    public PatternMatcher.AsCompositeMethod createCompositeMethodOutput(
        IJadescriptType solvedPatternType,
        List<String> additionalPreconditions,
        Function<Integer, String> compiledSubInputs,
        List<PatternMatcher> subResults
    ) {
        return new PatternMatcher.AsCompositeMethod(
            this,
            solvedPatternType,
            additionalPreconditions,
            compiledSubInputs,
            subResults
        );
    }


    public PatternMatcher.AsCompositeMethod createCompositeMethodOutput(
        IJadescriptType solvedPatternType,
        Function<Integer, String> compiledSubInputs,
        List<PatternMatcher> subResults
    ) {
        return new PatternMatcher.AsCompositeMethod(
            this,
            solvedPatternType,
            compiledSubInputs,
            subResults
        );
    }


    public PatternMatcher.AsCompositeMethod createCompositeMethodOutput(
        List<StatementWriter> auxiliaryStatements,
        IJadescriptType solvedPatternType,
        List<String> additionalPreconditions,
        Function<Integer, String> compiledSubInputs,
        List<PatternMatcher> subResults
    ) {
        return new PatternMatcher.AsCompositeMethod(
            this,
            auxiliaryStatements,
            solvedPatternType,
            additionalPreconditions,
            compiledSubInputs,
            subResults
        );
    }


    public PatternMatcher.AsSingleConditionMethod
    createSingleConditionMethodOutput(
        IJadescriptType solvedPatternType,
        String condition
    ) {
        return new PatternMatcher.AsSingleConditionMethod(
            this,
            solvedPatternType,
            condition
        );
    }


    public PatternMatcher.AsPlaceholderMethod createPlaceholderMethodOutput(
        IJadescriptType solvedPatternType
    ) {
        return new PatternMatcher.AsPlaceholderMethod(this, solvedPatternType);
    }


    public PatternMatcher.AsInlineCondition createInlineConditionOutput(
        Function<String, String> generateCondition
    ) {
        return new PatternMatcher.AsInlineCondition(this) {
            @Override
            public String operationInvocationText(String input) {
                return generateCondition.apply(input);
            }
        };
    }


    public PatternMatcher.AsFieldAssigningMethod
    createFieldAssigningMethodOutput(
        IJadescriptType solvedPatternType,
        String fieldName
    ) {
        return new PatternMatcher.AsFieldAssigningMethod(
            this,
            solvedPatternType,
            fieldName
        );
    }

    public PatternMatcher.AsReassigningMethod
    createReassigningMethodOutput(
        IJadescriptType solvedPatternType,
        BiConsumer<String, BlockElementAcceptor> write
    ) {
        return new PatternMatcher.AsReassigningMethod(
            this,
            solvedPatternType,
            write
        );
    }


    public static class WhenMatchesStatement<T> extends PatternMatchInput<T> {

        public static final PatternMatchMode MODE = new PatternMatchMode(
            PatternMatchMode.HolesAndGroundness.ACCEPTS_ANY_HOLE,
            TypeRelationshipQuery.related(),
            PatternMatchMode.RequiresSuccessfulMatch.CAN_FAIL,
            PatternMatchMode.PatternApplicationSideEffects
                .CAN_HAVE_SIDE_EFFECTS,
            PatternMatchMode.Unification.WITH_VAR_DECLARATION,
            PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
            PatternMatchMode.PatternLocation.STATEMENT_GUARD
        );
        private final IJadescriptType inputExprType;


        public WhenMatchesStatement(
            SemanticsModule module,
            IJadescriptType inputExprType,
            Maybe<T> pattern,
            String termID,
            String rootPatternMatchVariableName,
            Maybe<ExpressionDescriptor> descriptorMaybe
        ) {
            super(
                module,
                MODE,
                pattern,
                termID,
                rootPatternMatchVariableName,
                descriptorMaybe
            );
            this.inputExprType = inputExprType;
        }


        @Override
        public <R> WhenMatchesStatement<R> mapPattern(Function<T, R> function) {
            return new WhenMatchesStatement<>(
                module,
                getProvidedInputType(),
                getPattern().__(function),
                getTermID(),
                getRootPatternMatchVariableName(),
                getInputDescriptor()
            );
        }


        @Override
        public IJadescriptType getProvidedInputType() {
            return inputExprType;
        }


    }

    public static class HandlerHeader<T> extends PatternMatchInput<T> {

        public static final PatternMatchMode MODE = new PatternMatchMode(
            PatternMatchMode.HolesAndGroundness.ACCEPTS_ANY_HOLE,
            TypeRelationshipQuery.subTypeOrEqual(),
            PatternMatchMode.RequiresSuccessfulMatch.CAN_FAIL,
            PatternMatchMode.PatternApplicationSideEffects
                .HAS_TO_BE_WITHOUT_SIDE_EFFECTS,
            PatternMatchMode.Unification.WITH_VAR_DECLARATION,
            PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
            PatternMatchMode.PatternLocation.FEATURE_HEADER
        );
        private final IJadescriptType contentUpperBound;


        public HandlerHeader(
            SemanticsModule module,
            IJadescriptType contentUpperBound,
            Maybe<T> pattern,
            String termID,
            String rootPatternMatchVariableName,
            Maybe<ExpressionDescriptor> descriptorMaybe
        ) {
            super(
                module,
                MODE,
                pattern,
                termID,
                rootPatternMatchVariableName,
                descriptorMaybe
            );
            this.contentUpperBound = contentUpperBound;
        }


        @Override
        public <R> HandlerHeader<R> mapPattern(Function<T, R> function) {
            return new HandlerHeader<>(
                module,
                contentUpperBound,
                getPattern().__(function),
                getTermID(),
                getRootPatternMatchVariableName(),
                getInputDescriptor()
            );
        }


        @Override
        public IJadescriptType getProvidedInputType() {
            return contentUpperBound;
        }


    }


    public static class MatchesExpression<T> extends PatternMatchInput<T> {

        public static final PatternMatchMode MODE = new PatternMatchMode(
            PatternMatchMode.HolesAndGroundness.ACCEPTS_ANY_HOLE,
            TypeRelationshipQuery.related(),
            PatternMatchMode.RequiresSuccessfulMatch.CAN_FAIL,
            PatternMatchMode.PatternApplicationSideEffects
                .CAN_HAVE_SIDE_EFFECTS,
            PatternMatchMode.Unification.WITH_VAR_DECLARATION,
            PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
            PatternMatchMode.PatternLocation.BOOLEAN_EXPRESSION
        );
        private final IJadescriptType inputExprType;


        public MatchesExpression(
            SemanticsModule module,
            IJadescriptType inputExprType,
            Maybe<T> pattern,
            String termID,
            String rootPatternMatchVariableName,
            Maybe<ExpressionDescriptor> descriptorMaybe
        ) {
            super(
                module,
                MODE,
                pattern,
                termID,
                rootPatternMatchVariableName,
                descriptorMaybe
            );
            this.inputExprType = inputExprType;
        }


        @Override
        public <R> MatchesExpression<R> mapPattern(Function<T, R> function) {
            return new MatchesExpression<>(
                module,
                inputExprType,
                getPattern().__(function),
                getTermID(),
                getRootPatternMatchVariableName(),
                getInputDescriptor()
            );
        }


        @Override
        public IJadescriptType getProvidedInputType() {
            return inputExprType;
        }


    }

    public static class AssignmentDeconstruction<T>
        extends PatternMatchInput<T> {

        public static final PatternMatchMode MODE = new PatternMatchMode(
            PatternMatchMode.HolesAndGroundness.REQUIRES_FREE_VARS,
            TypeRelationshipQuery.superTypeOrEqual(),
            PatternMatchMode.RequiresSuccessfulMatch.REQUIRES_SUCCESSFUL_MATCH,
            PatternMatchMode.PatternApplicationSideEffects
                .CAN_HAVE_SIDE_EFFECTS,
            PatternMatchMode.Unification.WITH_VAR_DECLARATION,
            PatternMatchMode.NarrowsTypeOfInput.DOES_NOT_NARROW_TYPE,
            PatternMatchMode.PatternLocation.ROOT_OF_ASSIGNED_EXPRESSION
        );
        private final IJadescriptType rightType;


        public AssignmentDeconstruction(
            SemanticsModule module,
            IJadescriptType rightType,
            Maybe<T> leftPattern,
            String termID,
            String rootPatternMatchVariableName,
            Maybe<ExpressionDescriptor> descriptorMaybe
        ) {
            super(
                module,
                MODE,
                leftPattern,
                termID,
                rootPatternMatchVariableName,
                descriptorMaybe
            );
            this.rightType = rightType;
        }


        @Override
        public <R> AssignmentDeconstruction<R>
        mapPattern(Function<T, R> function) {
            return new AssignmentDeconstruction<>(
                module,
                rightType,
                getPattern().__(function),
                getTermID(),
                getRootPatternMatchVariableName(),
                getInputDescriptor()
            );
        }


        @Override
        public IJadescriptType getProvidedInputType() {
            return rightType;
        }


    }


    public static class SubPattern<T, RT> extends PatternMatchInput<T> {

        private final PatternMatchInput<RT> rootInput;
        private final String suffixID;


        private final IJadescriptType inputInfo;
        private final
        Function<ExpressionDescriptor, Maybe<ExpressionDescriptor>>
            generateSubDescriptor;


        private SubPattern(
            SemanticsModule module,
            IJadescriptType inputInfo,
            PatternMatchInput<RT> rootInput,
            Maybe<T> pattern,
            String suffixID,
            Function<ExpressionDescriptor, Maybe<ExpressionDescriptor>>
                generateSubDescriptor
        ) {
            this(
                module,
                inputInfo,
                rootInput,
                pattern,
                suffixID,
                null,
                generateSubDescriptor
            );
        }


        private SubPattern(
            SemanticsModule module,
            IJadescriptType inputInfo,
            PatternMatchInput<RT> rootInput,
            Maybe<T> pattern,
            String suffixID,
            PatternMatchMode.HolesAndGroundness holesAndGroundnessRequirement,
            Function<ExpressionDescriptor, Maybe<ExpressionDescriptor>>
                generateSubDescriptor
        ) {
            super(
                module,
                subPatternMode(rootInput, holesAndGroundnessRequirement),
                pattern,
                rootInput.termID + suffixID,
                rootInput.getRootPatternMatchVariableName(),
                rootInput.getInputDescriptor().flatApp(generateSubDescriptor)
            );
            this.rootInput = rootInput;
            this.suffixID = suffixID;
            this.inputInfo = inputInfo;
            this.generateSubDescriptor = generateSubDescriptor;

        }


        @NotNull
        private static <RT> PatternMatchMode subPatternMode(
            PatternMatchInput<RT> rootInput,
            PatternMatchMode.HolesAndGroundness holesAndGroundnessRequirement
        ) {
            PatternMatchMode.HolesAndGroundness holesAndGroundness;

            if (holesAndGroundnessRequirement == null) {
                holesAndGroundness =
                    rootInput.getMode().getHolesAndGroundness();
                if(holesAndGroundness ==
                    PatternMatchMode.HolesAndGroundness.REQUIRES_FREE_VARS){
                    holesAndGroundness =
                        PatternMatchMode.HolesAndGroundness.ACCEPTS_ANY_HOLE;
                }
            } else {
                holesAndGroundness = holesAndGroundnessRequirement;
            }
            return new PatternMatchMode(
                holesAndGroundness,
                // Subpatterns always have a "related" requirement,
                // except when in a successful match is required.
                rootInput.getMode().getRequiresSuccessfulMatch()
                    == PatternMatchMode.RequiresSuccessfulMatch
                    .REQUIRES_SUCCESSFUL_MATCH
                    ? TypeRelationshipQuery.superTypeOrEqual()
                    : TypeRelationshipQuery.related(),
                rootInput.getMode().getRequiresSuccessfulMatch(),
                rootInput.getMode().getPatternApplicationPurity(),
                rootInput.getMode().getUnification(),
                rootInput.getMode().getNarrowsTypeOfInput(),
                PatternMatchMode.PatternLocation.SUB_PATTERN
            );
        }


        public PatternMatchInput<RT> getRootInput() {
            return rootInput;
        }


        @Override
        public <R> SubPattern<R, RT> mapPattern(Function<T, R> function) {
            return new SubPattern<>(
                module,
                inputInfo,
                rootInput,
                getPattern().__(function),
                suffixID,
                generateSubDescriptor
            );
        }


        @Override
        public IJadescriptType getProvidedInputType() {
            return inputInfo;
        }


    }


}
