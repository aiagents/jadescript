package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.sonneteer.WriterFactory;

/**
 * Created on 26/04/18.
 */
public abstract class Semantics implements SemanticsConsts {

    public static final WriterFactory w = WriterFactory.getInstance();

    public final SemanticsModule module;


    public Semantics(SemanticsModule semanticsModule) {
        this.module = semanticsModule;
    }


}
