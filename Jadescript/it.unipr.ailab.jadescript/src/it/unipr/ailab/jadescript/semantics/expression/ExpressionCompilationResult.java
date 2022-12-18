package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ExpressionCompilationResult extends ExpressionSemanticsResult {
    private String generatedText;

    private ExpressionCompilationResult(String generatedText) {
        this.generatedText = generatedText;
    }



    public String getGeneratedText() {
        return generatedText;
    }

    @Override
    public String toString() {
        return this.generatedText;
    }

    public ExpressionCompilationResult replaceText(String newText){
        this.generatedText = newText;
        return this;
    }

    public ExpressionCompilationResult mapText(Function<String, String> changeText){
        this.generatedText = changeText.apply(this.generatedText);
        return this;
    }

    @Override
    public ExpressionCompilationResult updateFTKB(Consumer<ExpressionTypeKB> update) {
        return (ExpressionCompilationResult) super.updateFTKB(update);
    }


    @Override
    public ExpressionCompilationResult setFTKB(ExpressionTypeKB kb) {
        return (ExpressionCompilationResult) super.setFTKB(kb);
    }

    @Override
    public ExpressionCompilationResult withPropertyChain(String... chain) {
        return (ExpressionCompilationResult) super.withPropertyChain(chain);
    }

    @Override
    public ExpressionCompilationResult withPropertyChain(List<String> chain) {
        return (ExpressionCompilationResult) super.withPropertyChain(chain);
    }

    public static ExpressionCompilationResult result(String generatedText) {
        return new ExpressionCompilationResult(generatedText);
    }

    public static ExpressionCompilationResult result(Maybe<String> maybeGeneratedText){
        if(maybeGeneratedText.isPresent()){
            return result(maybeGeneratedText.toNullable());
        }else{
            return empty();
        }
    }

    public static ExpressionCompilationResult empty(){
        return new ExpressionCompilationResult("");
    }

    public static ExpressionCompilationResult combine(
            ExpressionCompilationResult c1,
            ExpressionCompilationResult c2,
            BiFunction<String, String, String> combine
    ){
        return result(combine.apply(c1.getGeneratedText(), c2.getGeneratedText()));
    }
}
