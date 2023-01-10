package it.unipr.ailab.jadescript.semantics.context.staticstate;

public interface MatchingResult {

    public interface DidMatch extends MatchingResult {

        public static final DidMatch INSTANCE = new DidMatch() {
        };

    }

}
