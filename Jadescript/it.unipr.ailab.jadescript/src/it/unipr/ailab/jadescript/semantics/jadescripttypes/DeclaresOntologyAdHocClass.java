package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmMember;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public interface DeclaresOntologyAdHocClass {
    WriterFactory w = WriterFactory.getInstance();

    void declareAdHocClass(
            EList<JvmMember> members,
            Maybe<ExtendingFeature> feature,
            HashMap<String, String> generatedSpecificClasses,
            List<StatementWriter> addSchemaWriters,
            List<StatementWriter> describeSchemaWriters,
            TypeExpression slotTypeExpression,
            Function<TypeExpression, String> schemaNameForSlotProvider,
            SemanticsModule module
    );

    String getAdHocClassName();

    String getConverterToAdHocClassMethodName();
}
