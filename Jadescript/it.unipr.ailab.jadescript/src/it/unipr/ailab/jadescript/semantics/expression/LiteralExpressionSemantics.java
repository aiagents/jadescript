package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

import java.util.Optional;
import java.util.stream.Stream;



/**
 * Created on 28/12/16.
 */
@SuppressWarnings("restriction")
@Singleton
public class LiteralExpressionSemantics
    extends AssignableExpressionSemantics<Literal> {


    public LiteralExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public static Class<? extends Number> getTypeOfNumberLiteral(
        SemanticsModule module,
        Maybe<String> l
    ) {
        Optional<String> ol = l.toOpt();
        if (ol.isPresent()) {
            String literal = ol.get();
            NumberLiterals numberLiterals = module.get(NumberLiterals.class);
            XNumberLiteral xNumberLiteral = XbaseFactory.eINSTANCE
                .createXNumberLiteral();
            xNumberLiteral.setValue(literal);
            Class<? extends Number> type = numberLiterals.getJavaType(
                xNumberLiteral
            );
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
     * When true, the input is a map because the literal type specification
     * says so.
     * Please note that it could be still a map if this returns false; this
     * happens when there is no type specification.
     */
    private boolean isMapT(Maybe<MapOrSetLiteral> input) {
        return input.__(MapOrSetLiteral::isIsMapT).orElse(false)
            || input.__(MapOrSetLiteral::getValueTypeParameter).isPresent();
    }


    /**
     * When true, the input is a map because at least one of the 'things'
     * between commas is a key:value pair or because
     * its an empty ('{:}') map literal.
     */
    private boolean isMapV(Maybe<MapOrSetLiteral> input) {
        return input.__(MapOrSetLiteral::isIsMap).orElse(false);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Literal> input
    ) {
        return Stream.empty();
    }


    @Override
    protected String compileInternal(
        Maybe<Literal> input, StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (input == null){
            return "";
        }

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
    protected IJadescriptType inferTypeInternal(
        Maybe<Literal> input,
        StaticState state
    ) {
        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> bool = input.__(Literal::getBool);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);


        if (number.isPresent()) {
            return module.get(TypeSolver.class).fromClass(
                getTypeOfNumberLiteral(module, number)
            );
        } else if (bool.isPresent()) {
            return module.get(BuiltinTypeProvider.class).boolean_();
        } else if (timestamp.isPresent()) {
            return module.get(BuiltinTypeProvider.class).timestamp();
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
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<Literal> input) {
        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> mapOrSet = input.__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);

        if (mustTraverse(input)) {
            if (string.isPresent()) {
                return Optional.of(
                    new SemanticsBoundToAssignableExpression<>(
                        module.get(StringLiteralSemantics.class),
                        string
                    )
                );
            } else if (list.isPresent()) {
                return Optional.of(
                    new SemanticsBoundToAssignableExpression<>(
                        module.get(ListLiteralExpressionSemantics.class),
                        list
                    )
                );
            } else if (mapOrSet.isPresent()) {
                if (isMap) {
                    return Optional.of(
                        new SemanticsBoundToAssignableExpression<>(
                            module.get(MapLiteralExpressionSemantics.class),
                            mapOrSet
                        )
                    );
                } else {
                    return Optional.of(
                        new SemanticsBoundToAssignableExpression<>(
                            module.get(SetLiteralExpressionSemantics.class),
                            mapOrSet
                        )
                    );
                }
            }
        }

        return Optional.empty();

    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<Literal> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

    }


    @Override
    protected IJadescriptType assignableTypeInternal(
        Maybe<Literal> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).nothing("");
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<Literal> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<Literal> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<Literal> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(
            input,
            acceptor
        );
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Literal> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Literal> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Literal> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final Maybe<String> number = input.getPattern().__(Literal::getNumber);
        final Maybe<String> bool = input.getPattern().__(Literal::getBool);
        final Maybe<String> timestamp = input.getPattern()
            .__(Literal::getTimestamp);

        if (number.isPresent() || bool.isPresent() || timestamp.isPresent()) {
            return compileExpressionEqualityPatternMatch(
                input,
                state,
                acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        final Maybe<String> number = input.getPattern().__(Literal::getNumber);
        final Maybe<String> bool = input.getPattern().__(Literal::getBool);
        final Maybe<String> timestamp = input.getPattern()
            .__(Literal::getTimestamp);

        if (number.isPresent() || bool.isPresent() || timestamp.isPresent()) {
            return PatternType.simple(inferType(input.getPattern(), state));
        } else {
            return PatternType.empty(module);
        }
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Literal> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final Maybe<String> number = input.getPattern().__(Literal::getNumber);
        final Maybe<String> bool = input.getPattern().__(Literal::getBool);
        final Maybe<String> timestamp =
            input.getPattern().__(Literal::getTimestamp);
        if (number.isPresent() || bool.isPresent() || timestamp.isPresent()) {
            return validateExpressionEqualityPatternMatch(
                input,
                state,
                acceptor
            );
        } else {
            return VALID;
        }
    }


    @Override
    protected boolean validateInternal(
        Maybe<Literal> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null){
            return VALID;
        }

        final Maybe<String> number = input.__(Literal::getNumber);

        if (number.isPresent()) {
            return input.__(inputSafe -> validateNumberLiteral(
                number.toNullable(),
                inputSafe,
                acceptor
            )).orElse(VALID);
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
            XNumberLiteral xNumberLiteral =
                XbaseFactory.eINSTANCE.createXNumberLiteral();
            xNumberLiteral.setValue(number);
            final NumberLiterals numberLiterals =
                module.get(NumberLiterals.class);
            numberLiterals.numberValue(
                xNumberLiteral,
                numberLiterals.getJavaType(xNumberLiteral)
            );
            return VALID;
        } catch (Exception e) {
            return module.get(ValidationHelper.class).emitError(
                "InvalidNumberFormat",
                "Invalid number format: " + e.getMessage(),
                Maybe.some(inputSafe),
                acceptor
            );
        }
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Literal> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Literal> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<Literal> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<Literal> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Literal> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Literal> input,
        StaticState state
    ) {
        return false;
    }


}
