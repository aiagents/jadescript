package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Class containing useful values resulting from the evaluation of an expression.
 * <p></p>
 * The first is a flow-typing knowledge base, i.e., a set simple data structures which represent compile-time logical
 * facts about the type of the underlying expression, which can be used to flow-type (i.e., narrow the type of expressions
 * depending on the control-flow context of the source code - also known as flow-typing or smart-casting in some
 * languages) parts of the expression.
 * For example, given the expression 'a is A', when used inside the guard of an if-statement, the compiler could
 * deduce that in the then-branch of the if, the type of a is, in fact, A.
 * This feature is primarily designed for the automatic extraction of incoming messages in message handlers, using
 * information given by the when-expressions and the content pattern of the handler.
 * <p></p>
 * The second is the "property chain" metadata.
 * For example, the expression "a of b of c of d" produces the property chain [a,b,c,d].
 */
public class ExpressionSemanticsResult {
    private ExpressionTypeKB flowTypingKnowledgeBase = new ExpressionTypeKB();
    private List<String> propertyChain = List.of();

    public ExpressionSemanticsResult updateFTKB(Consumer<ExpressionTypeKB> update) {
        update.accept(flowTypingKnowledgeBase);
        return this;
    }

    public ExpressionSemanticsResult withPropertyChain(String... chain) {
        propertyChain = List.of(chain);
        return this;
    }

    public ExpressionSemanticsResult withPropertyChain(List<String> chain) {
        propertyChain = chain;
        return this;
    }

    public ExpressionSemanticsResult setFTKB(ExpressionTypeKB kb) {
        this.flowTypingKnowledgeBase = kb;
        return this;
    }

    public List<String> getPropertyChain() {
        return propertyChain;
    }

    public ExpressionTypeKB getFlowTypingKB() {
        return flowTypingKnowledgeBase;
    }


}
