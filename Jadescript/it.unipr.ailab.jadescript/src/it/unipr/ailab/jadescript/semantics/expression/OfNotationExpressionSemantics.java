package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.result;
import static it.unipr.ailab.maybe.Maybe.*;


/**
 * Created on 01/04/18.
 */
@Singleton
public class OfNotationExpressionSemantics extends AssignableExpressionSemantics<OfNotation> {


    public OfNotationExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    private String generateMethodName(String propName, IJadescriptType prevType, boolean isAssignment) {
        if (propName.equals("size") || propName.equals("length")) {
            if (module.get(TypeHelper.class).TEXT.isAssignableFrom(prevType)) {
                return "length";
            } else {
                return "size";
            }
        } else {
            return (isAssignment ? "set" : "get") + Strings.toFirstUpper(propName);
        }
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<OfNotation> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        return Collections.singletonList(new SemanticsBoundToExpression<>(
                module.get(AidLiteralExpressionSemantics.class),
                aidLiteral
        ));
    }




    @Override
    public ExpressionCompilationResult compile(Maybe<OfNotation> input, StatementCompilationOutputAcceptor acceptor) {
        if (input == null) return ExpressionCompilationResult.empty();


        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        ExpressionCompilationResult r = module.get(AidLiteralExpressionSemantics.class).compile(aidLiteral, acceptor);
        List<String> propertyChain = new ArrayList<>(r.getPropertyChain());
        IJadescriptType prev = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);
        for (int i = properties.size() - 1; i >= 0; i--) {
            String propName = properties.get(i).extract(nullAsEmptyString);
            Optional<? extends NamedSymbol> property = prev.namespace().searchAs(
                    NamedSymbol.Searcher.class,
                    s -> s.searchName(propName, null, null)
            ).findFirst();
            if (property.isPresent()) {
                r = result(property.get().compileRead(r + "."));
            } else {
                r = result(r + "." + generateMethodName(propName, prev, false) + "()");
            }
            prev = inferTypeProperty(of(propName), prev);
            propertyChain.add(0, propName);
        }
        return r.withPropertyChain(propertyChain);
    }

    @Override
    public void compileAssignment(
            Maybe<OfNotation> input,
            String compiledExpression,
            IJadescriptType exprType,
            StatementCompilationOutputAcceptor acceptor
    ) {

        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (properties.isEmpty()) {
            module.get(AidLiteralExpressionSemantics.class).compileAssignment(
                    aidLiteral,
                    compiledExpression,
                    exprType,
                    acceptor
            );
            return;
        }

        StringBuilder sb = new StringBuilder(module.get(AidLiteralExpressionSemantics.class)
                .compile(aidLiteral, acceptor).toString());
        IJadescriptType prevType = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);
        for (int i = properties.size() - 1; i >= 0; i--) {
            String propName = properties.get(i).extract(nullAsEmptyString);
            IJadescriptType currentPropType = inferTypeProperty(of(propName), prevType);

            Optional<? extends NamedSymbol> property = prevType.namespace().searchAs(
                    NamedSymbol.Searcher.class,
                    s -> s.searchName(propName, null, null)
            ).findFirst();

            final String rExprConverted = module.get(TypeHelper.class).compileWithEventualImplicitConversions(
                    compiledExpression,
                    exprType,
                    currentPropType
            );
            if (property.isPresent()) {

                if (i == 0) {
                    sb = new StringBuilder(property.get().compileWrite(
                            sb + ".",
                            rExprConverted
                    ));
                } else {
                    sb = new StringBuilder(property.get().compileRead(
                            sb + "."
                    ));
                }
            } else {
                if (i == 0) {
                    sb.append(".").append(generateMethodName(propName, prevType, true)).append("(");
                    sb.append(rExprConverted).append(")");
                } else {
                    sb.append(".").append(generateMethodName(propName, prevType, false)).append("()");
                }
            }


            prevType = inferTypeProperty(of(propName), prevType);
        }
        acceptor.accept(w.simpleStmt(sb.toString()));
    }

    @Override
    public IJadescriptType inferType(Maybe<OfNotation> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);

        List<Maybe<String>> props = new ArrayList<>(properties);
        IJadescriptType afterLastOf = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);
        if (mustTraverse(input)) {
            return afterLastOf;
        } else {
            IJadescriptType prevType = afterLastOf;
            for (int i = props.size() - 1; i >= 0; i--) {
                Maybe<String> prop = props.get(i);
                prevType = inferTypeProperty(prop, prevType);
            }
            return prevType;
        }
    }

    @Override
    public boolean mustTraverse(Maybe<OfNotation> input) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        return properties.isEmpty();
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<OfNotation> input) {
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(module.get(AidLiteralExpressionSemantics.class), aidLiteral));
        }
        return Optional.empty();
    }


    public IJadescriptType inferTypeProperty(Maybe<String> prop, IJadescriptType prevType) {
        String propSafe = prop.extract(nullAsEmptyString);
        return prevType.namespace().searchAs(
                        NamedSymbol.Searcher.class,
                        s -> s.searchName(propSafe, null, null)
                ).findFirst()
                .map(NamedSymbol::readingType)
                .orElse(
                        module.get(TypeHelper.class).NOTHING
                );
    }

    @Override
    public void validateAssignment(
            Maybe<OfNotation> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return;

        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (mustTraverse(input)) {
            module.get(AidLiteralExpressionSemantics.class).validateAssignment(aidLiteral, expression, acceptor);
            return;
        }

        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
        module.get(RValueExpressionSemantics.class).validate(expression, subValidation);
        if (!subValidation.thereAreErrors()) {
            IJadescriptType typeOfRExpression = module.get(RValueExpressionSemantics.class).inferType(expression);

            List<Maybe<String>> props = new ArrayList<>(properties);

            IJadescriptType prevType = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);

            for (int i = props.size() - 1; i >= 0; i--) {
                Maybe<String> prop = props.get(i);
                InterceptAcceptor propSubValidation = new InterceptAcceptor(acceptor);
                module.get(ValidationHelper.class).assertTypeManipulable(input, "Can not operate on instances of type '"
                        + prevType.getJadescriptName() + "'", prevType, propSubValidation);
                if (i == 0) {
                    validateAssignProperty(input, acceptor, typeOfRExpression, i, prop, prevType);
                } else {
                    validateProperty(input, propSubValidation, i, prop, prevType);
                }
                if (!propSubValidation.thereAreErrors()) {
                    prevType = inferTypeProperty(prop, prevType);
                } else {
                    break;
                }
            }

        }
    }


    public void validateProperty(
            Maybe<OfNotation> input,
            ValidationMessageAcceptor acceptor,
            int index,
            Maybe<String> propmaybe,
            IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return;
        }
        //propmaybe is present
        String prop = propmaybe.toNullable();
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        if (!interceptAcceptor.thereAreErrors()) {
            module.get(ValidationHelper.class).assertion(
                    prevType.namespace().searchAs(
                            NamedSymbol.Searcher.class,
                            s -> s.searchName(prop, null, null)
                    ).findFirst().isPresent(),
                    "InvalidOfNotation",
                    "Can not find property '" + prop + "' in type " + prevType.getJadescriptName() + ".",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
            );
        }

    }

    public void validateAssignProperty(
            Maybe<OfNotation> input,
            ValidationMessageAcceptor acceptor,
            IJadescriptType typeOfRExpression,
            int index,
            Maybe<String> propmaybe,
            IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return;
        }
        //propmaybe is present
        String prop = propmaybe.toNullable();

        Optional<? extends NamedSymbol> foundProperty = prevType.namespace().searchAs(
                NamedSymbol.Searcher.class,
                s -> s.searchName(prop, null, null)
        ).findFirst();

        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(ValidationHelper.class).assertion(
                foundProperty.isPresent(),
                "InvalidOfNotation",
                "Can not find property '" + prop + "' in type " + prevType.getJadescriptName() + ".",
                input,
                JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                index,
                interceptAcceptor
        );

        if (!interceptAcceptor.thereAreErrors() && foundProperty.isPresent()) {
            module.get(ValidationHelper.class).assertion(
                    foundProperty.get().canWrite(),
                    "InvalidOfNotation",
                    "Cannot assign to read-only property '" + prop + "'",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    interceptAcceptor
            );
        }

        if (!interceptAcceptor.thereAreErrors() && foundProperty.isPresent()) {
            module.get(ValidationHelper.class).assertExpectedType(
                    foundProperty.get().readingType(),
                    typeOfRExpression,
                    "InvalidOfNotation",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
            );
        }


    }


    @Override
    public void syntacticValidateLValue(Maybe<OfNotation> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (properties.isEmpty()) {
            module.get(AidLiteralExpressionSemantics.class).syntacticValidateLValue(aidLiteral, acceptor);
        }
        //else: IS VALID
    }

    @Override
    public boolean isValidLExpr(Maybe<OfNotation> input) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (properties.isEmpty()) {
            return module.get(AidLiteralExpressionSemantics.class).isValidLExpr(aidLiteral);
        }
        return false;
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<OfNotation> input) {
        if (mustTraverse(input)) {
            return module.get(AidLiteralExpressionSemantics.class).isPatternEvaluationPure(
                    input.__(OfNotation::getAidLiteral)
            );
        }
        return false;
    }

    @Override
    public void validate(Maybe<OfNotation> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(AidLiteralExpressionSemantics.class).validate(aidLiteral, interceptAcceptor);
        if (!interceptAcceptor.thereAreErrors()) {
            IJadescriptType afterLastOfType = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);
            List<Maybe<String>> props = new ArrayList<>(properties);
            if (!props.isEmpty()) {
                IJadescriptType prevType = afterLastOfType;
                for (int i = props.size() - 1; i >= 0; i--) { //reverse
                    Maybe<String> prop = props.get(i);
                    InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
                    if (!prop.wrappedEquals("length") || !module.get(TypeHelper.class).TEXT.isAssignableFrom(prevType)) {
                        module.get(ValidationHelper.class).assertTypeManipulable(input, "Can not operate on instances of type '"
                                + prevType.getJadescriptName() + "'", prevType, subValidation);
                    }
                    validateProperty(input, subValidation, i, prop, prevType);
                    if (!subValidation.thereAreErrors()) {
                        prevType = inferTypeProperty(prop, prevType);
                    } else {
                        break;
                    }

                }
            }
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<OfNotation, ?, ?> input, StatementCompilationOutputAcceptor acceptor) {
        final Maybe<OfNotation> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(AidLiteralExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(OfNotation::getAidLiteral),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<OfNotation> input) {
        if (mustTraverse(input)) {
            return module.get(AidLiteralExpressionSemantics.class).inferPatternTypeInternal(
                    input.__(OfNotation::getAidLiteral));
        } else {
            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<OfNotation, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<OfNotation> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(AidLiteralExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(OfNotation::getAidLiteral),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }


}
