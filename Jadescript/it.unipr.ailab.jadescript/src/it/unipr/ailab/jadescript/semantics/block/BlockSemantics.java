package it.unipr.ailab.jadescript.semantics.block;

import it.unipr.ailab.jadescript.jadescript.CodeBlock;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.Statement;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsDispatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsClassState;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/12/16.
 */
public class BlockSemantics extends Semantics {

    public static final BlockSemantics EMPTY_BLOCK = new BlockSemantics(null) {
        @Override
        public PSR<BlockWriter> compile(
            Maybe<CodeBlock> input,
            StaticState state
        ) {
            final BlockWriter blockWriter = w.block().addStatement(
                w.simpleStmt("//do nothing")
            );

            return PSR.psr(blockWriter, state);
        }


        @Override
        public StaticState validate(
            Maybe<CodeBlock> input,
            StaticState state,
            ValidationMessageAcceptor acceptor
        ) {
            return state;
        }
    };


    private final SemanticsClassState<CodeBlock, List<StatementWriter>>
        injectedStatements = new SemanticsClassState<>(ArrayList::new);


    public BlockSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public PSR<BlockWriter> compileOptionalBlock(
        Maybe<OptionalBlock> input,
        StaticState state
    ) {
        if (input.isPresent() && input.toNullable().isNothing()) {
            return EMPTY_BLOCK.compile(
                input.__(OptionalBlock::getBlock),
                state
            );
        } else {
            return compile(input.__(OptionalBlock::getBlock), state);
        }
    }


    public StaticState validateOptionalBlock(
        Maybe<OptionalBlock> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (!input.isPresent() || !input.toNullable().isNothing()) {
            return validate(
                input.__(OptionalBlock::getBlock),
                state,
                acceptor
            );
        } else {
            return state;
        }
    }


    public PSR<BlockWriter> compile(
        Maybe<CodeBlock> input,
        StaticState state
    ) {
        BlockWriter bp = w.block();

        final AtomicReference<StaticState> runningState =
            new AtomicReference<>(state);


        try {
            Maybe<EList<Statement>> statements =
                input.__(CodeBlock::getStatements);

            final List<StatementWriter> injectedStatements =
                this.injectedStatements.getOrNew(input);


            for (StatementWriter injectedStatement : injectedStatements) {
                bp.addStatement(injectedStatement);
            }


            final SemanticsDispatchHelper dispatchHelper =
                module.get(SemanticsDispatchHelper.class);


            for (Maybe<Statement> statement : iterate(statements)) {

                if (statement.isNothing()) {
                    continue;
                }

                dispatchHelper.dispatchStatementSemantics(
                    statement,
                    sem -> {
                        StaticState afterStatement = sem.compileStatement(
                            wrappedSubCast(statement),
                            runningState.get(),
                            element -> {
                                bp.add(
                                    CompilationHelper.inputStatementComment(
                                        statement,
                                        "Compiled from source statement"
                                    )
                                );
                                bp.add(element);
                            }
                        );

                        runningState.set(afterStatement);

                    }
                );

            }

        } catch (Throwable e) {
            bp.addStatement(new GenerationError(e).buildThrowStatementWriter());
            if (GenerationParameters.DEBUG) {
                throw e;
            }
        }
        return PSR.psr(bp, runningState.get());
    }


    public StaticState validate(
        Maybe<CodeBlock> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input.isNothing()) {
            return state;
        }

        List<Maybe<Statement>> statements = toListOfMaybes(
            input.__(CodeBlock::getStatements)
        );


        final AtomicReference<StaticState> runningState =
            new AtomicReference<>(state);


        final SemanticsDispatchHelper semanticsDispatchHelper =
            module.get(SemanticsDispatchHelper.class);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        try {
            for (Maybe<Statement> statement : statements) {

                if (statement.isNothing()) {
                    continue;
                }


                semanticsDispatchHelper.dispatchStatementSemantics(
                    statement,
                    (sem) -> {

                        if (!runningState.get().isValid()) {
                            validationHelper.emitError(
                                "UnreacheableStatement",
                                "Unreachable statement.",
                                statement,
                                acceptor
                            );
                            return; //from lambda
                        }

                        InterceptAcceptor interceptAcceptor =
                            new InterceptAcceptor(acceptor);

                        StaticState afterStatement = sem.validateStatement(
                            wrappedSubCast(statement),
                            runningState.get(),
                            interceptAcceptor
                        );

                        if (interceptAcceptor.thereAreErrors()) {
                            return; //from lambda (not advancing state)
                        }

                        runningState.set(afterStatement);
                    }
                );

            }
        } catch (Throwable e) {
            validationHelper.emitError(
                SemanticsConsts.ISSUE_CODE_PREFIX + "InternalError",
                "Internal error: " + e.getMessage() + "\n" +
                    GenerationError.stackTraceToString(e),
                input,
                acceptor
            );

            if (GenerationParameters.DEBUG) {
                throw e;
            }
        }
        return runningState.get();


    }
}
