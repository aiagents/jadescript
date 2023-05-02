package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ProceduralFeatureContainerContext;

/**
 * A context redirects to the outer file context.
 * <p>
 * For now, used for member behaviours.
 */
public class EmulatedFileContext extends FileContext {

    public EmulatedFileContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        this(
            module,
            outer.getOuterContextTopLevelDeclaration()
                .getOuterContextFile()
        );
    }


    public EmulatedFileContext(
        SemanticsModule module,
        FileContext outer
    ) {
        super(
            module,
            outer.getOuterContextModule(),
            outer.getFileName(),
            outer.getFileURI(),
            outer.getImportDeclarations()
        );
    }


}
