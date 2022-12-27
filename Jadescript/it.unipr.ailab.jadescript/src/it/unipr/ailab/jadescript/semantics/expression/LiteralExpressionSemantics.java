package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
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
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;


/**
 * Created on 28/12/16.
 */
@SuppressWarnings("restriction")
@Singleton
public class LiteralExpressionSemantics extends ExpressionSemantics<Literal> {


    public LiteralExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    public static Class<? extends Number> getTypeOfNumberLiteral(SemanticsModule module, Maybe<String> l) {
        Optional<String> ol = l.toOpt();
        if (ol.isPresent()) {
            String literal = ol.get();
            NumberLiterals numberLiterals = module.get(NumberLiterals.class);
            XNumberLiteral xNumberLiteral = XbaseFactory.eINSTANCE.createXNumberLiteral();
            xNumberLiteral.setValue(literal);
            Class<? extends Number> type = numberLiterals.getJavaType(xNumberLiteral);
            if (type == Byte.TYPE) {
                return Byte.class;
            }
            if (type == Integer.TYPE) {
                return Integer.class;
            }
            if (type == Float.TYPE) {
                return Float.class;
            }
            if (type == Long.TYPE) {
                return Long.class;
            }
            if (type == Short.TYPE) {
                return Short.class;
            }
            return Double.class;
        } else {
            return Integer.class;
        }

    }

    public boolean isMap(Maybe<MapOrSetLiteral> input) {
        return isMapV(input) || isMapT(input);
    }

    /**
     * When true, the input is a map because the literal type specification says so.
     * Please note that it could be still a map if this returns false; this happens when there is no type specification.
     */
    private boolean isMapT(Maybe<MapOrSetLiteral> input) {
        return input.__(MapOrSetLiteral::isIsMapT).extract(nullAsFalse)
                || input.__(MapOrSetLiteral::getValueTypeParameter).isPresent();
    }

    /**
     * When true, the input is a map because at least one of the 'things' between commas is a key:value pair or because
     * its an empty ('{:}') map literal.
     */
    private boolean isMapV(Maybe<MapOrSetLiteral> input) {
        return input.__(MapOrSetLiteral::isIsMap).extract(nullAsFalse);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<Literal> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(Maybe<Literal> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";

        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);
        final Maybe<String> bool = input.__(Literal::getBool);

        if (number.isPresent()) {
            if (number.isPresent() && number.toNullable().contains(".")) {
                return number.toNullable() + "f";
            }
            return number.orElse("/*null number*/0");
        } else if (timestamp.isPresent()) {
            if (timestamp.wrappedEquals("today")) {
                return "jadescript.lang.Timestamp.today()";
            } else {
                return "jadescript.lang.Timestamp.now()";
            }
        } else if (bool.isPresent()) {
            return bool.orElse("/*null boolean*/false");
        } else {
            throw new UnsupportedNodeType("Unsupported type of literal");
        }
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Literal> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> bool = input.__(Literal::getBool);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);


        if (number.isPresent()) {
            return module.get(TypeHelper.class).jtFromClass(getTypeOfNumberLiteral(module, number));
        } else if (bool.isPresent()) {
            return module.get(TypeHelper.class).BOOLEAN;
        } else if (timestamp.isPresent()) {
            return module.get(TypeHelper.class).TIMESTAMP;
        } else {
            throw new UnsupportedNodeType("Unsupported type of literal");
        }
    }

    @Override
    protected boolean mustTraverse(Maybe<Literal> input) {
        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.__(Literal::getMap);
        return string.isPresent() || list.isPresent() || map.isPresent();

    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Literal> input) {
        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> mapOrSet = input.__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);

        if (mustTraverse(input)) {
            if (string.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(StringLiteralSemantics.class), string));
            } else if (list.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(ListLiteralExpressionSemantics.class), list));
            } else if (mapOrSet.isPresent()) {
                if (isMap) {
                    return Optional.of(new SemanticsBoundToExpression<>(
                            module.get(MapLiteralExpressionSemantics.class),
                            mapOrSet
                    ));
                } else {
                    return Optional.of(new SemanticsBoundToExpression<>(
                            module.get(SetLiteralExpressionSemantics.class),
                            mapOrSet
                    ));
                }
            }
        }

        return Optional.empty();

    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<Literal> input) {
        return subPatternEvaluationsAllPure(input);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Literal, ?, ?> input, CompilationOutputAcceptor acceptor) {
        final Maybe<String> number = input.getPattern().__(Literal::getNumber);
        final Maybe<String> bool = input.getPattern().__(Literal::getBool);
        final Maybe<String> timestamp = input.getPattern().__(Literal::getTimestamp);

        if (number.isPresent() || bool.isPresent() || timestamp.isPresent()) {
            return compileExpressionEqualityPatternMatch(
                    input,
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Literal> input) {
        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> bool = input.__(Literal::getBool);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);


        if (number.isPresent() || bool.isPresent() || timestamp.isPresent()) {
            return PatternType.simple(inferType(input));
        } else {
            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Literal, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<String> number = input.getPattern().__(Literal::getNumber);
        final Maybe<String> bool = input.getPattern().__(Literal::getBool);
        final Maybe<String> timestamp = input.getPattern().__(Literal::getTimestamp);
        if (number.isPresent() || bool.isPresent() || timestamp.isPresent()) {
            return validateExpressionEqualityPatternMatch(input, acceptor);
        } else {
            return input.createEmptyValidationOutput();
        }
    }

    @Override
    protected boolean validateInternal(Maybe<Literal> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;

        final Maybe<String> number = input.__(Literal::getNumber);

        if (number.isPresent()) {
            return input.__(inputSafe ->
                    validateNumberLiteral(number.toNullable(), inputSafe, acceptor)
            ).orElse(VALID);
        } else {
            return VALID;
        }
    }


    private boolean validateNumberLiteral(
            String number,
            Literal inputSafe,
            ValidationMessageAcceptor acceptor
    ) {
        try {
            XNumberLiteral xNumberLiteral = XbaseFactory.eINSTANCE.createXNumberLiteral();
            xNumberLiteral.setValue(number);
            final NumberLiterals numberLiterals = module.get(NumberLiterals.class);
            numberLiterals.numberValue(
                    xNumberLiteral,
                    numberLiterals.getJavaType(xNumberLiteral)
            );
            return VALID;
        } catch (Exception e) {
            acceptor.acceptError(
                    "Invalid number format: " + e.getMessage(),
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidNumberFormat"
            );
            return INVALID;
        }
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<Literal> input) {
        return List.of();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<Literal> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<Literal> input) {
        return true;
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Literal> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<Literal> input) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Literal> input) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Literal> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<Literal> input) {
        return false;
    }


}
