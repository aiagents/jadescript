package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnPerceptHandlerContext
        extends HandlerWithWhenExpressionContext
        implements NameMember.Namespace, PerceptPerceivedContext {

    private final IJadescriptType perceptContentType;

    public OnPerceptHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            IJadescriptType perceptContentType
    ) {
        super(module, outer, "percept");
        this.perceptContentType = perceptContentType;
    }


    @Override
    public IJadescriptType getPerceptContentType() {
        return perceptContentType;
    }

    @Override
    public Stream<? extends NameMember> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        return getPerceptContentStream(name, readingType, canWrite);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnPerceptHandlerContext");
        debugDumpPerception(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on percept";
    }
}
