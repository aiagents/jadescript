package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.DebugTypeComparison;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.*;

public class DebugTypeComparisonSemantics
    extends StatementSemantics<DebugTypeComparison> {

    public DebugTypeComparisonSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState validateStatement(
        Maybe<DebugTypeComparison> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<TypeExpression> typEx1 =
            input.__(DebugTypeComparison::getType1);
        final Maybe<TypeExpression> typEx2 =
            input.__(DebugTypeComparison::getType2);
        final IJadescriptType type1 =
            module.get(TypeExpressionSemantics.class).toJadescriptType(typEx1);
        final IJadescriptType type2 =
            module.get(TypeExpressionSemantics.class).toJadescriptType(typEx2);

        input.safeDo(inputSafe -> acceptor.acceptInfo(
                getComparisonMessage(type1, type2),
                inputSafe,
                null,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                "DEBUG"
            )
        );

        return state;
    }


    private String getComparisonMessage(
        IJadescriptType type1,
        IJadescriptType type2
    ) {
        String result = "Comparison of types.\n" +
            "Type A: " + type1.getDebugPrint() + "\n" +
            "Type B: " + type2.getDebugPrint() + "\n" +
            "\n";

        final TypeComparator comparator = module.get(TypeComparator.class);
        final TypeRelationship comparison = comparator.compare(type1, type2);

        if (comparison.is(equal())) {
            result += "TypeA(" + type1 + ") and TypeB(" + type2 +
                ") are equal.\n";
        }

        if (comparison.is(strictSuperType())) {
            result += "TypeA(" + type1 + ") > TypeB(" + type2 + ").\n";
        }

        if (comparison.is(strictSubType())) {
            result += "TypeA(" + type1 + ") < TypeB(" + type2 + ").\n";
        }

        if(comparison.is(notRelated())) {
            result += "TypeA(" + type1 + ") and TypeB(" + type2 + ") are not " +
                "related.";
        }
        return result;
    }


    @Override
    public StaticState compileStatement(
        Maybe<DebugTypeComparison> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final Maybe<TypeExpression> typEx1 =
            input.__(DebugTypeComparison::getType1);
        final Maybe<TypeExpression> typEx2 =
            input.__(DebugTypeComparison::getType2);
        final IJadescriptType type1 =
            module.get(TypeExpressionSemantics.class).toJadescriptType(typEx1);
        final IJadescriptType type2 =
            module.get(TypeExpressionSemantics.class).toJadescriptType(typEx2);
        acceptor.accept(
            w.simpleStmt("/*" + getComparisonMessage(type1, type2) + "*/")
        );
        return state;
    }


}
