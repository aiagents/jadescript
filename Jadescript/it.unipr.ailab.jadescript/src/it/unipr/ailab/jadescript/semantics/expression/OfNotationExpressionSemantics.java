package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<OfNotation> input) {
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
    protected List<String> propertyChainInternal(Maybe<OfNotation> input) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);

        List<String> result = new ArrayList<>();
        properties.stream()
                .filter(Maybe::isPresent)
                .map(Maybe::toNullable)
                .forEach(result::add);

        result.addAll(module.get(AidLiteralExpressionSemantics.class).propertyChain(aidLiteral));

        return result;
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<OfNotation> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected String compileInternal(Maybe<OfNotation> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";


        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        StringBuilder r = new StringBuilder(module.get(AidLiteralExpressionSemantics.class)
                .compile(aidLiteral, acceptor));

        IJadescriptType prev = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);
        for (int i = properties.size() - 1; i >= 0; i--) {
            String propName = properties.get(i).extract(nullAsEmptyString);
            Optional<? extends NamedSymbol> property = prev.namespace().searchAs(
                    NamedSymbol.Searcher.class,
                    s -> s.searchName(propName, null, null)
            ).findFirst();
            if (property.isPresent()) {
                r = new StringBuilder(property.get().compileRead(r + "."));
            } else {
                r.append(".").append(generateMethodName(propName, prev, false)).append("()");
            }
            prev = inferTypeProperty(of(propName), prev);
        }
        return r.toString();
    }

    @Override
    public void compileAssignmentInternal(
            Maybe<OfNotation> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
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
                .compile(aidLiteral, acceptor));
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
    protected IJadescriptType inferTypeInternal(Maybe<OfNotation> input) {
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
    protected boolean mustTraverse(Maybe<OfNotation> input) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        return properties.isEmpty();
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<OfNotation> input) {
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
    public boolean validateAssignmentInternal(
            Maybe<OfNotation> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;

        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (mustTraverse(input)) {
            return module.get(AidLiteralExpressionSemantics.class).validateAssignment(aidLiteral, expression, acceptor);
        }

        boolean subValidation = module.get(RValueExpressionSemantics.class)
                .validate(expression, acceptor);
        if(subValidation == INVALID){
            return subValidation;
        }

        IJadescriptType typeOfRExpression = module.get(RValueExpressionSemantics.class).inferType(expression);
        List<Maybe<String>> props = new ArrayList<>(properties);
        IJadescriptType prevType = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);

        for (int i = props.size() - 1; i >= 0; i--) {
            Maybe<String> prop = props.get(i);
            boolean result = module.get(ValidationHelper.class).assertPropertiesOfTypeAccessible(
                    input,
                    "Cannot access properties of values of type '"
                            + prevType.getJadescriptName() + "'",
                    prevType,
                    acceptor
            );
            if (i == 0) {
                result = result && validateAssignProperty(input, acceptor, typeOfRExpression, i, prop, prevType);
            } else {
                result = result && validateProperty(input, acceptor, i, prop, prevType);
            }
            if (result == VALID) {
                prevType = inferTypeProperty(prop, prevType);
            } else {
                return result;
            }
        }

        return VALID;
    }


    public boolean validateProperty(
            Maybe<OfNotation> input,
            ValidationMessageAcceptor acceptor,
            int index,
            Maybe<String> propmaybe,
            IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return VALID;
        }
        //propmaybe is present
        String prop = propmaybe.toNullable();

        return module.get(ValidationHelper.class).assertion(
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

    public boolean validateAssignProperty(
            Maybe<OfNotation> input,
            ValidationMessageAcceptor acceptor,
            IJadescriptType typeOfRExpression,
            int index,
            Maybe<String> propmaybe,
            IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return VALID;
        }
        //propmaybe is present
        String prop = propmaybe.toNullable();

        Optional<? extends NamedSymbol> foundProperty = prevType.namespace().searchAs(
                NamedSymbol.Searcher.class,
                s -> s.searchName(prop, null, null)
        ).findFirst();


        boolean result = module.get(ValidationHelper.class).assertion(
                foundProperty.isPresent(),
                "InvalidOfNotation",
                "Can not find property '" + prop + "' in type " + prevType.getJadescriptName() + ".",
                input,
                JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                index,
                acceptor
        );

        if (result == VALID && foundProperty.isPresent()) {
            result = result && module.get(ValidationHelper.class).assertion(
                    foundProperty.get().canWrite(),
                    "InvalidOfNotation",
                    "Cannot assign to read-only property '" + prop + "'",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
            );
        }

        if (result == VALID && foundProperty.isPresent()) {
            result = result && module.get(ValidationHelper.class).assertExpectedType(
                    foundProperty.get().readingType(),
                    typeOfRExpression,
                    "InvalidOfNotation",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
            );
        }

        return result;


    }


    @Override
    public boolean syntacticValidateLValueInternal(Maybe<OfNotation> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (properties.isEmpty()) {
            return module.get(AidLiteralExpressionSemantics.class).syntacticValidateLValue(aidLiteral, acceptor);
        }

        return VALID;
        //else: IS VALID
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<OfNotation> input) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);
        if (properties.isEmpty()) {
            return module.get(AidLiteralExpressionSemantics.class).isValidLExpr(aidLiteral);
        }
        return false;
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<OfNotation> input) {
        if (mustTraverse(input)) {
            return module.get(AidLiteralExpressionSemantics.class).isPatternEvaluationPure(
                    input.__(OfNotation::getAidLiteral)
            );
        }
        return false;
    }

    @Override
    protected boolean validateInternal(Maybe<OfNotation> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral = input.__(OfNotation::getAidLiteral);

        final boolean subValidation = module.get(AidLiteralExpressionSemantics.class)
                .validate(aidLiteral, acceptor);

        if (subValidation == INVALID) {
            return subValidation;
        }

        IJadescriptType afterLastOfType = module.get(AidLiteralExpressionSemantics.class).inferType(aidLiteral);
        List<Maybe<String>> props = new ArrayList<>(properties);
        if (!props.isEmpty()) {
            IJadescriptType prevType = afterLastOfType;
            for (int i = props.size() - 1; i >= 0; i--) { //reverse
                Maybe<String> prop = props.get(i);
                boolean result = VALID;
                if (!prop.wrappedEquals("length") || !module.get(TypeHelper.class).TEXT.isAssignableFrom(prevType)) {
                    result = module.get(ValidationHelper.class).assertPropertiesOfTypeAccessible(
                            input,
                            "Cannot access properties of values of type '" + prevType.getJadescriptName() + "'",
                            prevType,
                            acceptor
                    );
                }
                result = result && validateProperty(input, acceptor, i, prop, prevType);
                if (result == VALID) {
                    prevType = inferTypeProperty(prop, prevType);
                } else {
                    return result;
                }
            }
        }

        return VALID;

    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<OfNotation, ?, ?> input, CompilationOutputAcceptor acceptor) {
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
