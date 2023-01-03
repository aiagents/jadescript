package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;

public class FlowTypingRuleConsequence {
    private final SemanticsModule module;
    private final ExpressionDescriptor forExpression;
    private final Maybe<IJadescriptType> upperBound;
    private final Maybe<IJadescriptType> negatedUpperBound;

    private FlowTypingRuleConsequence(
        SemanticsModule module,
        ExpressionDescriptor forExpression,
        Maybe<IJadescriptType> upperBound,
        Maybe<IJadescriptType> negatedUpperBound
    ) {
        this.module = module;
        this.forExpression = forExpression;
        this.upperBound = upperBound;
        this.negatedUpperBound = negatedUpperBound;
    }


    public ExpressionDescriptor getExpression() {
        return forExpression;
    }

    public Maybe<IJadescriptType> getUpperBound() {
        return upperBound;
    }

    public Maybe<IJadescriptType> getNegatedUpperBound() {
        return negatedUpperBound;
    }


    private Maybe<IJadescriptType> maybeMergeGLB(
        Maybe<IJadescriptType> t1,
        Maybe<IJadescriptType> t2
    ){
        if (!t1.isPresent()) {
            return t2;
        }
        if (!t2.isPresent()) {
            return t1;
        }
        return Maybe.some(module.get(TypeHelper.class)
            .getGLB(t1.toNullable(), t2.toNullable())
        );
    }

    private Maybe<IJadescriptType> maybeMergeLUB(
        Maybe<IJadescriptType> t1,
        Maybe<IJadescriptType> t2
    ){
        if (!t1.isPresent()) {
            return t2;
        }
        if (!t2.isPresent()) {
            return t1;
        }
        return Maybe.some(module.get(TypeHelper.class)
            .getLUB(t1.toNullable(), t2.toNullable())
        );
    }




    public FlowTypingRuleConsequence and(FlowTypingRuleConsequence other){
        return new FlowTypingRuleConsequence(
            module,
            getExpression(),
            maybeMergeGLB(this.getUpperBound(), other.getUpperBound()),
            maybeMergeGLB(
                this.getNegatedUpperBound(),
                other.getNegatedUpperBound()
            )
        );
    }

    public FlowTypingRuleConsequence or(FlowTypingRuleConsequence other){
        return new FlowTypingRuleConsequence(
            module,
            getExpression(),
            maybeMergeLUB(this.getUpperBound(), other.getUpperBound()),
            maybeMergeLUB(
                this.getNegatedUpperBound(),
                other.getNegatedUpperBound()
            )
        );
    }

    public FlowTypingRuleConsequence not(){
        return new FlowTypingRuleConsequence(
            module,
            getExpression(),
            // Swapped on purpose:
            this.getNegatedUpperBound(), this.getUpperBound()
        );
    }


    public StaticState assertAnd(StaticState state) {
        StaticState result = state;
        final Maybe<IJadescriptType> ub = getUpperBound();
        final Maybe<IJadescriptType> nub = getNegatedUpperBound();
        if(ub.isPresent()){
            result = result.assertFlowTypingUpperBound(
                getExpression(),
                ub.toNullable()
            );
        }
        if(nub.isPresent()){
            result = result.assertFlowTypingNegatedUpperBound(
                getExpression(),
                nub.toNullable()
            );
        }
        return result;
    }

    public StaticState assertOr(StaticState state){
        StaticState result = state;
        final Maybe<IJadescriptType> ub = getUpperBound();
        final Maybe<IJadescriptType> nub = getNegatedUpperBound();
        if(ub.isPresent()){
            result = result.alternativeFlowTypingUpperBound(
                getExpression(),
                ub.toNullable()
            );
        }
        if(nub.isPresent()){
            result = result.alternativeFlowTypingNegatedUpperBound(
                getExpression(),
                nub.toNullable()
            );
        }
        return result;
    }
}
