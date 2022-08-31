package it.unipr.ailab.jadescript.semantics.context;

import java.util.ArrayDeque;
import java.util.Deque;

public class SavedContext {
    private final Context innerContext;
    private final Deque<Context> outerContexts;


    public SavedContext(Context innerContext, Deque<Context> outerContexts) {
        this.innerContext = innerContext;
        this.outerContexts = new ArrayDeque<>(outerContexts);
    }

    public Context getInnerContext() {
        return innerContext;
    }

    public Deque<Context> getOuterContexts() {
        return outerContexts;
    }
}
