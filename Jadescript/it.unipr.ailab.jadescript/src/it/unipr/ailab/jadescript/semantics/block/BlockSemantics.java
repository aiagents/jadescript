package it.unipr.ailab.jadescript.semantics.block;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.effectanalysis.EffectfulOperationSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsDispatchHelper;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsClassState;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/12/16.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
public class BlockSemantics extends Semantics<CodeBlock>
        implements EffectfulOperationSemantics {


    public static final BlockSemantics EMPTY_BLOCK = new BlockSemantics(null) {
        @Override
        public BlockWriter compile(Maybe<CodeBlock> input) {
            return w.block().addStatement(
                    new StatementWriter() {
                        @Override
                        public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
                            return this;
                        }

                        @Override
                        public void writeSonnet(SourceCodeBuilder s) {
                            s.add("//do nothing");
                        }
                    }
            );
        }

        @Override
        public void validate(Maybe<CodeBlock> input, ValidationMessageAcceptor acceptor) {
            //do nothing
        }
    };


    private final SemanticsClassState<CodeBlock, List<StatementWriter>> injectedStatements
            = new SemanticsClassState<>(ArrayList::new);

    private final SemanticsClassState<CodeBlock, List<NamedSymbol>> injectedVariables
            = new SemanticsClassState<>(ArrayList::new);


    public BlockSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    public BlockWriter compileOptionalBlock(Maybe<OptionalBlock> input) {
        if (input.isPresent() && input.toNullable().isNothing()) {
            return EMPTY_BLOCK.compile(input.__(OptionalBlock::getBlock));
        } else {
            return compile(input.__(OptionalBlock::getBlock));
        }
    }

    public void validateOptionalBlock(Maybe<OptionalBlock> input, ValidationMessageAcceptor acceptor) {
        if (!input.isPresent() || !input.toNullable().isNothing()) {
            validate(input.__(OptionalBlock::getBlock), acceptor);
        }
    }


    public BlockWriter compile(Maybe<CodeBlock> input) {
        BlockWriter bp = w.block();
        module.get(ContextManager.class).pushScope();
        Maybe<EList<Statement>> statements = input.__(CodeBlock::getStatements);
        try {

            for (StatementWriter injectedStatement : injectedStatements.getOrNew(input)) {
                bp.addStatement(injectedStatement);
            }

            for (NamedSymbol injectedVariable : injectedVariables.getOrNew(input)) {
                module.get(ContextManager.class).currentScope().addNamedElement(injectedVariable);
            }

            for (Maybe<Statement> statement : iterate(statements)) {
                List<StatementWriter> auxiliaryStatements = new ArrayList<>();
                List<BlockWriterElement> statementWriters = new ArrayList<>();
                module.get(SemanticsDispatchHelper.class).dispatchStatementSemantics(statement, sem -> {
                    auxiliaryStatements.addAll(sem.generateAuxiliaryStatements(wrappedSubCast(statement)));
                    statementWriters.addAll(sem.compileStatement(wrappedSubCast(statement)));
                });
                auxiliaryStatements.forEach(bp::addStatement);
                statementWriters.forEach(bp::add);
            }

        } catch (Throwable e) {
            if (GenerationParameters.DEBUG) {
                throw e;
            } else {
                bp.addStatement(new GenerationError(e).buildThrowStatementWriter());
            }
        } finally {
            module.get(ContextManager.class).popScope();
        }
        return bp;
    }



    public void validate(Maybe<CodeBlock> input, ValidationMessageAcceptor acceptor) {

        Maybe<EList<Statement>> statements = input.__(CodeBlock::getStatements);

        Maybe.safeDo(input, statements,
                /*NULLSAFE REGION*/(inputSafe, statementsSafe) -> {
                    //this portion of code is done  only if input and statements
                    // are != null (and everything in the dotchains that generated them is !=null too)
                    module.get(ContextManager.class).pushScope();
                    try {

                        for (NamedSymbol injectedVariable : injectedVariables.getOrNew(input)) {
                            module.get(ContextManager.class).currentScope().addNamedElement(injectedVariable);
                        }

                        for (int i = 0; i < statementsSafe.size(); i++) {
                            Maybe<Statement> statement = of(statementsSafe.get(i));
                            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
                            final int finalI = i;
                            module.get(SemanticsDispatchHelper.class).dispatchStatementSemantics(statement, (sem) -> {
                                sem.validate(wrappedSubCast(statement), interceptAcceptor);
                                List<Effect> effects = sem.computeEffects(statement);
                                if (effects.stream().anyMatch(e -> e instanceof Effect.JumpsAwayFromIteration)) {
                                    if (!interceptAcceptor.thereAreErrors() && finalI < statementsSafe.size() - 1) {
                                        acceptor.acceptError(
                                                "Unreachable statement",
                                                inputSafe,
                                                JadescriptPackage.eINSTANCE.getCodeBlock_Statements(),
                                                finalI + 1,
                                                "UnreachableStatement"
                                        );
                                    }
                                }

                            });


                        }
                    } catch (Throwable e) {
                        if (GenerationParameters.DEBUG) {
                            throw e;
                        } else {
                            new GenerationError(e).toValidationError(inputSafe,
                                    JadescriptPackage.eINSTANCE.getCodeBlock_Statements(),
                                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX, acceptor
                            );
                        }
                    } finally {
                        module.get(ContextManager.class).popScope();
                    }

                }/*END NULLSAFE REGION - (inputSafe, statementsSafe)*/
        );


    }

    public void addInjectedStatements(Maybe<CodeBlock> input, List<StatementWriter> injectedStatements) {
        this.injectedStatements.getOrNew(input).addAll(injectedStatements);
    }

    public void addInjectedStatement(Maybe<CodeBlock> input, StatementWriter injectedStatement) {
        this.injectedStatements.getOrNew(input).add(injectedStatement);
    }

    public void addInjectedVariables(Maybe<CodeBlock> input, List<NamedSymbol> injectedVariables) {
        this.injectedVariables.getOrNew(input).addAll(injectedVariables);
    }

    public void addInjectedVariable(Maybe<CodeBlock> input, NamedSymbol injectedVariable) {
        this.injectedVariables.getOrNew(input).add(injectedVariable);
    }

    @Override
    public List<Effect> computeEffects(Maybe<? extends EObject> i) {
        if(!i.isInstanceOf(CodeBlock.class)){
            return Collections.emptyList();
        }
        Maybe<CodeBlock> input = i.__(x->(CodeBlock)x);

        List<Effect> result = new ArrayList<>();
        module.get(ContextManager.class).pushScope();
        try {
            Maybe<EList<Statement>> statements = input.__(CodeBlock::getStatements);
            statements.safeDo(statementsSafe -> {
                for (Statement statement : statementsSafe) {
                    module.get(SemanticsDispatchHelper.class).dispatchStatementSemantics(of(statement), (sem) -> {
                        result.addAll(sem.computeEffects(of(statement)));
                    });
                }
            });
        } finally {
            module.get(ContextManager.class).popScope();
        }
        return result;
    }

    public List<Effect> computeLastStatementEffects(Maybe<CodeBlock> input) {
        List<Effect> result = new ArrayList<>();
        module.get(ContextManager.class).pushScope();
        try {
            Maybe<EList<Statement>> statements = input.__(CodeBlock::getStatements);
            statements.safeDo(statementsSafe -> {
                for (int i = 0; i < statementsSafe.size(); i++) {
                    Statement statement = statementsSafe.get(i);
                    int finalI = i;
                    module.get(SemanticsDispatchHelper.class).dispatchStatementSemantics(of(statement), (sem) ->{
                        if(finalI == statementsSafe.size() - 1){
                            result.addAll(sem.computeEffects(of(statement)));
                        }
                    });
                }
            });
        } finally {
            module.get(ContextManager.class).popScope();
        }
        return result;
    }
}
