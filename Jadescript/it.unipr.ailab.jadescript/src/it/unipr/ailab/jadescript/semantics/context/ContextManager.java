package it.unipr.ailab.jadescript.semantics.context;

import it.unipr.ailab.jadescript.jadescript.Model;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.c0outer.EmulatedFileContext;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.c0outer.ModuleContext;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.*;
import it.unipr.ailab.jadescript.semantics.context.scope.ProceduralScope;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xtype.XImportDeclaration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class ContextManager {
    private final SemanticsModule module;
    private final Deque<Context> outerContexts = new ArrayDeque<>();
    private Context innerContext = null;

    public ContextManager(SemanticsModule module) {
        this.module = module;
    }

    //TODO ensure that each search from local scopes starts from
    // state, not from currentContext
    public Context currentContext() {
        return innerContext;
    }

    public void enterModule(
        String moduleName,
        Maybe<Model> sourceModule
    ) {
        this.innerContext = new ModuleContext(module, moduleName, sourceModule);
    }

    public void enterFile(
        String fileURI,
        String fileName,
        List<Maybe<XImportDeclaration>> importDeclarations
    ) {
        if (this.innerContext instanceof ModuleContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = new FileContext(
                module,
                ((ModuleContext) this.innerContext),
                fileName,
                fileURI,
                importDeclarations
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    FileContext.class
                );
            }
        }
    }

    public void enterTopLevelDeclaration(
        BiFunction<SemanticsModule, ? super FileContext, ?
            extends TopLevelDeclarationContext> provideContext
    ) {
        if (this.innerContext instanceof FileContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = provideContext.apply(
                module,
                ((FileContext) this.innerContext)
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    TopLevelDeclarationContext.class
                );
            }
        }
    }

    public void enterOntologyElementDeclaration() {
        if (this.innerContext instanceof OntologyDeclarationSupportContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = new OntologyElementDeclarationContext(
                module,
                ((OntologyDeclarationSupportContext) this.innerContext)
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    OntologyElementDeclarationContext.class
                );
            }
        }
    }

    public void enterProceduralFeatureContainer(
        IJadescriptType thisType,
        Maybe<? extends EObject> featureContainer
    ) {
        if (this.innerContext instanceof TopLevelDeclarationContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = new ProceduralFeatureContainerContext(
                module,
                ((TopLevelDeclarationContext) this.innerContext),
                thisType,
                featureContainer
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    ProceduralFeatureContainerContext.class
                );
            }
        }
    }

    public void enterProceduralFeatureContainer(
        Maybe<? extends EObject> featureContainer
    ) {
        if (this.innerContext instanceof TopLevelDeclarationContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = new ProceduralFeatureContainerContext(
                module,
                ((TopLevelDeclarationContext) this.innerContext),
                featureContainer
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    ProceduralFeatureContainerContext.class
                );
            }
        }
    }

    public void enterProceduralFeature(
        BiFunction<? super SemanticsModule, ?
            super ProceduralFeatureContainerContext,
            ? extends ProceduralFeatureContext> supplyContext
    ) {
        if (this.innerContext instanceof ProceduralFeatureContainerContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = supplyContext.apply(
                module,
                ((ProceduralFeatureContainerContext) this.innerContext)
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    ProceduralFeatureContext.class
                );
            }
        }
    }

    public void enterSuperSlotInitializer(
        Map<String, IJadescriptType> superSlotsInitScope
    ) {
        if (this.innerContext instanceof OntologyElementDeclarationContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = new SuperSlotInitializerContext(
                module,
                (OntologyElementDeclarationContext) this.innerContext,
                superSlotsInitScope
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    SuperSlotInitializerContext.class
                );
            }
        }
    }

    public void enterEmulatedFile() {
        if (this.innerContext instanceof ProceduralFeatureContainerContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = new EmulatedFileContext(
                module,
                (ProceduralFeatureContainerContext) this.innerContext
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else if (this.innerContext instanceof FileContext) {
            this.outerContexts.push(this.innerContext);
            this.innerContext = new EmulatedFileContext(
                module,
                (FileContext) this.innerContext
            );
            if (GenerationParameters.DEBUG_CONTEXT) {
                System.out.println("[" + module.getPhase() + "]: entered " +
                    innerContext.getClass().getSimpleName());
            }
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                // Throwing only in DEBUG mode, since illegal context orders
                // can happen
                // also when the source code is sintactically wrong, and we
                // don't need to
                // ensure correct context ordering in those cases.
                throw new IllegalContextOrder(
                    this.innerContext,
                    EmulatedFileContext.class
                );
            }
        }
    }

    public void exit() {
        if (GenerationParameters.DEBUG_CONTEXT) {
            System.out.println("[" + module.getPhase() + "]: exiting " +
                innerContext.getClass().getSimpleName());
        }
        this.innerContext = outerContexts.pop();
    }


    public void pushScope() {
        if (this.innerContext instanceof ScopedContext) {
            ((ScopedContext) this.innerContext).getScopeManager().enterScope();
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                throw new IllegalContextOrder(
                    this.innerContext,
                    ProceduralScope.class
                );
            }
        }
    }

    public void popScope() {
        if (this.innerContext instanceof ScopedContext) {
            ((ScopedContext) this.innerContext).getScopeManager().exitScope();
        } else {
            if (GenerationParameters.DEBUG_CONTEXT) {
                throw new IllegalContextOrder(
                    this.innerContext,
                    ProceduralScope.class
                );
            }
        }
    }

    public ProceduralScope currentScope() {
        if (this.innerContext instanceof ScopedContext) {
            return ((ScopedContext) this.innerContext).getScopeManager().getCurrentScope();
        } else {
            throw new IllegalContextOrder(
                this.innerContext,
                ProceduralScope.class
            );
        }
    }

    public SavedContext save() {
        if (GenerationParameters.DEBUG_CONTEXT) {
            System.out.println("[" + module.getPhase() + "]: Saving context " +
                "(inner=" + (this.innerContext != null
                ? this.innerContext.getClass().getSimpleName()
                : "null"
            ) + ")");
        }
        return new SavedContext(this.innerContext, this.outerContexts);
    }

    public void restore(SavedContext saved) {
        this.innerContext = saved.getInnerContext();
        this.outerContexts.clear();
        this.outerContexts.addAll(saved.getOuterContexts());
        if (GenerationParameters.DEBUG_CONTEXT) {
            System.out.println("[" + module.getPhase() + "]: Resumed context " +
                "(inner=" + (this.innerContext != null
                ? this.innerContext.getClass().getSimpleName()
                : "null"
            ) + ")");
        }
    }

    public ContextManager restoring(SavedContext saved) {
        this.restore(saved);
        return this;
    }

    public void debugDump(SourceCodeBuilder scb) {
        scb.line("****** CONTEXT DUMP: BEGIN ******");
        scb.line("{").indent();
        AtomicInteger counter = new AtomicInteger(outerContexts.size());
        outerContexts.descendingIterator().forEachRemaining(context -> {
            scb.line("****** BEGIN context " + counter.get() + " of class '" + context.getClass().getName() + "' ******");
            scb.line("{").indent();
            context.debugDump(scb);
            scb.dedent().line("}");
            scb.line("******  END  context " + counter.get() + " of class '" + context.getClass().getName() + "' ******");
            counter.decrementAndGet();
        });
        scb.line("****** BEGIN context 0 of class '" + currentContext().getClass().getName() + "' ******");
        scb.line("{").indent();
        currentContext().debugDump(scb);
        scb.dedent().line("}");
        scb.line("******  END  context 0 of class '" + currentContext().getClass().getName() + "' ******");
        counter.decrementAndGet();
        scb.dedent().line("}");
        scb.line("****** CONTEXT DUMP:  END  ******");
    }


    public class IllegalContextOrder extends RuntimeException {
        public IllegalContextOrder(Context outer, Class<?> attemptedInner) {
            super("[" + module.getPhase() + "]: A <" + (outer != null ?
                outer.getClass().getName() : "null")
                + "> context cannot be the direct container of a <" + attemptedInner.getName() + "> context.");
        }
    }

}
