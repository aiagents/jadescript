package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.ReturnStatementWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.TryCatchWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Intermediate representation of the compilation of a pattern matching
 * operation.
 * Collects all the information needed to compile a pattern matching into Java
 * code.
 */
public abstract class PatternMatcher {

    private static final WriterFactory w = WriterFactory.getInstance();


    protected final PatternMatchInput<?> patternMatchInput;
    protected final List<PatternMatcher> subResults = new ArrayList<>();


    public PatternMatcher(PatternMatchInput<?> patternMatchInput) {
        this.patternMatchInput = patternMatchInput;
    }


    public static List<StatementWriter> compileAdaptType(
        String adaptType
    ) {
        final ReturnStatementWriter returnFalse = w.returnStmnt(w.False);

        final VariableDeclarationWriter declareX =
            w.variable(adaptType, "__x");//initialized later
        final TryCatchWriter checkXType = w.tryCatch(w.block()
                .addStatement(w.ifStmnt(
                    w.expr("__objx instanceof " + JvmTypeHelper
                        .noGenericsTypeName(adaptType)),
                    w.block().addStatement(w.assign(
                        "__x",
                        w.expr("(" + adaptType + ") __objx")
                    ))
                ).setElseBranch(w.block().addStatement(returnFalse))))
            .addCatchBranch("java.lang.ClassCastException", "ignored", w.block()
                .addStatement(returnFalse));
        return Arrays.asList(declareX, checkXType);
    }


    public Stream<? extends ClassMemberWriter> getAllSubwriters() {
        return subResults.stream().flatMap(PatternMatcher::getAllWriters);
    }


    public abstract Stream<? extends ClassMemberWriter> getDirectWriters();


    public final Stream<? extends ClassMemberWriter> getAllWriters() {
        return Stream.concat(
            getAllSubwriters(),
            getDirectWriters()
        );
    }


    public String compilePatternMatchExpression(String input) {
        return patternMatchInput.getRootPatternMatchVariableName() +
            "." + operationInvocationText(input);
    }


    public abstract String operationInvocationText(String input);


    public static abstract class AsMethod extends PatternMatcher {

        protected final List<StatementWriter> compiledAdaptType;


        public AsMethod(
            PatternMatchInput<?> patternMatchInput,
            IJadescriptType solvedPatternType
        ) {
            super(patternMatchInput);
            compiledAdaptType = compileAdaptType(
                solvedPatternType.compileToJavaTypeReference()
            );
        }


        @Override
        public String operationInvocationText(String input) {
            return patternMatchInput.getTermID() + "(" + input + ")";
        }

    }

    public static class AsCompositeMethod extends AsMethod {

        private final Function<Integer, String> compiledSubInputs;

        private final List<String> additionalPreconditions;

        private final List<StatementWriter> auxiliaryStatements;


        public AsCompositeMethod(
            PatternMatchInput<?> patternMatchInput,
            IJadescriptType solvedPatternType,
            List<String> additionalPreconditions,
            Function<Integer, String> compiledSubInputs,
            List<? extends PatternMatcher> subResults
        ) {
            super(patternMatchInput, solvedPatternType);
            this.compiledSubInputs = compiledSubInputs;
            this.additionalPreconditions = additionalPreconditions;
            this.subResults.addAll(subResults);
            this.auxiliaryStatements = List.of();
        }


        public AsCompositeMethod(
            PatternMatchInput<?> patternMatchInput,
            IJadescriptType solvedPatternType,
            Function<Integer, String> compiledSubInputs,
            List<PatternMatcher> subResults
        ) {
            super(patternMatchInput, solvedPatternType);
            this.compiledSubInputs = compiledSubInputs;
            this.additionalPreconditions = List.of();
            this.subResults.addAll(subResults);
            this.auxiliaryStatements = List.of();
        }


        public AsCompositeMethod(
            PatternMatchInput<?> patternMatchInput,
            List<StatementWriter> auxiliaryStatements,
            IJadescriptType solvedPatternType,
            List<String> additionalPreconditions,
            Function<Integer, String> compiledSubInputs,
            List<PatternMatcher> subResults
        ) {
            super(patternMatchInput, solvedPatternType);
            this.compiledSubInputs = compiledSubInputs;
            this.additionalPreconditions = additionalPreconditions;
            this.subResults.addAll(subResults);
            this.auxiliaryStatements = auxiliaryStatements;
        }


        public MethodWriter generatedMethod() {
            MethodWriter m = w.method(
                    Visibility.PUBLIC,
                    false,
                    false,
                    "boolean",
                    patternMatchInput.getTermID()
                )
                .addParameter(w.param("java.lang.Object", "__objx"));

            m.getBody().addStatements(compiledAdaptType);

            m.getBody().addStatements(auxiliaryStatements);

            StringBuilder sb = new StringBuilder("true");
            for (String additionalPrecondition : additionalPreconditions) {
                sb.append(" && ");
                sb.append(additionalPrecondition);
            }
            for (int i = 0; i < subResults.size(); i++) {
                PatternMatcher subResult = subResults.get(i);
                sb.append(" && ");
                sb.append(subResult.operationInvocationText(
                    compiledSubInputs.apply(i)
                ));
            }
            m.getBody().addStatement(w.returnStmnt(w.expr(sb.toString())));
            return m;
        }


        @Override
        public Stream<? extends ClassMemberWriter> getDirectWriters() {
            return Stream.of(generatedMethod());
        }

    }

    public static class AsSingleConditionMethod extends AsMethod {

        private final String condition;


        public AsSingleConditionMethod(
            PatternMatchInput<?> patternMatchInput,
            IJadescriptType solvedPatternType,
            String condition
        ) {
            super(patternMatchInput, solvedPatternType);
            this.condition = condition;
        }


        public MethodWriter generatedWriter() {
            MethodWriter m = w.method(
                    Visibility.PUBLIC,
                    false,
                    false,
                    "boolean",
                    patternMatchInput.getTermID()
                )
                .addParameter(w.param("java.lang.Object", "__objx"));
            m.getBody().addStatements(compiledAdaptType);
            m.getBody().addStatement(w.returnStmnt(w.expr(condition)));
            return m;
        }


        @Override
        public Stream<? extends ClassMemberWriter> getDirectWriters() {
            return Stream.of(generatedWriter());
        }

    }

    public static abstract class AsInlineCondition extends PatternMatcher {


        public AsInlineCondition(PatternMatchInput<?> patternMatchInput) {
            super(patternMatchInput);
        }


        @Override
        public Stream<? extends ClassMemberWriter> getDirectWriters() {
            return Stream.empty();
        }


        @Override
        public abstract String operationInvocationText(String input);

    }

    public static class AsEmpty extends PatternMatcher {

        public AsEmpty(PatternMatchInput<?> patternMatchInput) {
            super(patternMatchInput);
        }


        @Override
        public Stream<? extends ClassMemberWriter> getDirectWriters() {
            return Stream.empty();
        }


        @Override
        public String operationInvocationText(String input) {
            return input;
        }

    }

    public static class AsFieldAssigningMethod extends AsMethod {

        private final IJadescriptType solvedPatternType;
        private final String name;


        public AsFieldAssigningMethod(
            PatternMatchInput<?> patternMatchInput,
            IJadescriptType solvedPatternType,
            String name
        ) {
            super(patternMatchInput, solvedPatternType);
            this.solvedPatternType = solvedPatternType;
            this.name = name;
        }


        public FieldWriter generatedField() {
            return w.field(
                Visibility.PUBLIC,
                false,
                false,
                solvedPatternType.compileToJavaTypeReference(),
                name
            );
        }


        public MethodWriter generatedMethod() {
            MethodWriter m = w.method(
                    Visibility.PUBLIC,
                    false,
                    false,
                    "boolean",
                    patternMatchInput.getTermID()
                )
                .addParameter(w.param("java.lang.Object", "__objx"));
            m.getBody().addStatements(
                compiledAdaptType
            ).addStatement(
                w.assign(name, w.expr("__x"))
            ).addStatement(
                w.returnStmnt(w.expr("true"))
            );
            return m;
        }


        @Override
        public Stream<? extends ClassMemberWriter> getDirectWriters() {
            return Stream.of(generatedField(), generatedMethod());
        }


    }

    public static class AsReassigningMethod extends AsMethod {

        private final BiConsumer<String, BlockElementAcceptor> write;


        public AsReassigningMethod(
            PatternMatchInput<?> patternMatchInput,
            IJadescriptType solvedPatternType,
            BiConsumer<String, BlockElementAcceptor> write
        ) {
            super(patternMatchInput, solvedPatternType);
            this.write = write;
        }


        private MethodWriter generateMethod() {
            MethodWriter m = w.method(
                    Visibility.PUBLIC,
                    false,
                    false,
                    "boolean",
                    patternMatchInput.getTermID()
                )
                .addParameter(w.param("java.lang.Object", "__objx"));
            final BlockWriter body = m.getBody();
            body.addStatements(
                compiledAdaptType
            );

            this.write.accept("__x", body::add);

            body.addStatement(
                w.returnStmnt(w.expr("true"))
            );
            return m;
        }


        @Override
        public Stream<? extends ClassMemberWriter> getDirectWriters() {
            return Stream.of(generateMethod());
        }

    }

    public static class AsPlaceholderMethod extends AsMethod {

        public AsPlaceholderMethod(
            PatternMatchInput<?> patternMatchInput,
            IJadescriptType solvedPatternType
        ) {
            super(patternMatchInput, solvedPatternType);
        }


        public MethodWriter generatedWriter() {
            MethodWriter m = w.method(
                    Visibility.PUBLIC,
                    false,
                    false,
                    "boolean",
                    patternMatchInput.getTermID()
                )
                .addParameter(w.param("java.lang.Object", "__objx"));
            m.getBody().addStatements(compiledAdaptType);
            m.getBody().addStatement(w.returnStmnt(w.True));
            return m;
        }


        @Override
        public Stream<? extends ClassMemberWriter> getDirectWriters() {
            return Stream.of(generatedWriter());
        }

    }

}
