package it.unipr.ailab.jadescript.semantics.expression;

/**
 * Created on 28/12/16.
 */
@SuppressWarnings({"serial", "RedundantSuppression"})
public class UnsupportedNodeType extends RuntimeException {

    public UnsupportedNodeType(String msg) {
        super(msg);
    }

}
