package it.unipr.ailab.jadescript.semantics.expression;

/**
 * Created on 28/12/16.
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@SuppressWarnings({"serial", "RedundantSuppression"})
public class UnsupportedNodeType extends RuntimeException {
    public UnsupportedNodeType(String msg) {
        super(msg);
    }
}
