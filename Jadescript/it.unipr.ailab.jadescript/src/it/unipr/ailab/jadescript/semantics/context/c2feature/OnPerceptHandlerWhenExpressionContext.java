package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnPerceptHandlerWhenExpressionContext
        extends HandlerWhenExpressionContext
        implements NameMember.Namespace, PerceptPerceivedContext {

    public OnPerceptHandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }


    @Override
    public IJadescriptType getPerceptContentType() {
        return module.get(TypeHelper.class).PROPOSITION;
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
        scb.open("--> is OnPerceptHandlerWhenExpressionContext {");
        scb.line("perceptContentType = " +
            getPerceptContentType().getDebugPrint());
        scb.close("}");
        debugDumpPerception(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }
}
