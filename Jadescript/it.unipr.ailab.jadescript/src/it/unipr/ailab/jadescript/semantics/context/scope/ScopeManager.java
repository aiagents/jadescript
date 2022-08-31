package it.unipr.ailab.jadescript.semantics.context.scope;


public class ScopeManager {
    private ProceduralScope currentScope;

    public ScopeManager(){
        this.currentScope = new RootProceduralScope();
    }

    public void enterScope(){
        this.currentScope = new ChildProceduralScope(this.currentScope);
    }

    public void exitScope() {
        if(this.currentScope instanceof ChildProceduralScope){
            this.currentScope = ((ChildProceduralScope) this.currentScope).getOuterScope();
        }
    }

    public ProceduralScope getCurrentScope(){
        return this.currentScope;
    }
}
