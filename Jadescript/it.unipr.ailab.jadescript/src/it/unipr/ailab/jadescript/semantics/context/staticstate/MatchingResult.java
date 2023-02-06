package it.unipr.ailab.jadescript.semantics.context.staticstate;

public interface MatchingResult {

    interface DidMatch extends MatchingResult {

        DidMatch INSTANCE = new DidMatch() {
        };

    }

}
