package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ListLiteral;
import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.MapOrSetLiteral;
import it.unipr.ailab.jadescript.jadescript.StringLiteralSimple;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nothing;


/**
 * Created on 28/12/16.
 *
 * 
 */
@SuppressWarnings("restriction")
@Singleton
public class LiteralExpressionSemantics extends ExpressionSemantics<Literal> {


    public LiteralExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<Literal> input) {
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.__(Literal::getMap);
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Arrays.asList(
                list.extract(x -> new SemanticsBoundToExpression<>(module.get(ListLiteralExpressionSemantics.class), list)),
                map.extract(x -> new SemanticsBoundToExpression<>(module.get(MapOrSetLiteralExpressionSemantics.class), map))
        );
    }

    @Override
    public Maybe<String> compile(Maybe<Literal> input) {
        if (input == null) return nothing();

        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);


        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);
        final Maybe<String> bool = input.__(Literal::getBool);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.__(Literal::getMap);

        if (string.isPresent()) {
            return module.get(StringLiteralSemantics.class).compile(string);
        } else if (number.isPresent()) {
            if (number.isPresent() && number.toNullable().contains(".")) {
                return Maybe.of(number.toNullable() + "f");
            }
            return number;
        } else if (timestamp.isPresent()) {
            if (timestamp.wrappedEquals("today")) {
                return Maybe.of("jadescript.lang.Timestamp.today()");
            } else {
                return Maybe.of("jadescript.lang.Timestamp.now()");
            }
        } else if (bool.isPresent()) {
            return bool;
        } else if (list.isPresent()) {
            return module.get(ListLiteralExpressionSemantics.class).compile(list);
        } else if (map.isPresent()) {
            return module.get(MapOrSetLiteralExpressionSemantics.class).compile(map);
        } else {
            throw new UnsupportedNodeType("Literals supported now: String, Integer, Float, Boolean, List, Map");
        }
    }


    @Override
    public IJadescriptType inferType(Maybe<Literal> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;

        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> bool = input.__(Literal::getBool);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.__(Literal::getMap);
        if (string.isPresent()) {
            return module.get(StringLiteralSemantics.class).inferType(string);
        } else if (number.isPresent()) {
            return module.get(TypeHelper.class).jtFromClass(getTypeOfNumberLiteral(module, number));
        } else if (bool.isPresent()) {
            return module.get(TypeHelper.class).BOOLEAN;
        } else if (timestamp.isPresent()) {
            return module.get(TypeHelper.class).TIMESTAMP;
        } else if (list.isPresent()) {
            return module.get(ListLiteralExpressionSemantics.class).inferType(list);
        } else if (map.isPresent()) {
            return module.get(MapOrSetLiteralExpressionSemantics.class).inferType(map);
        } else {
            throw new UnsupportedNodeType("Literals supported now: text, integer, real, boolean, timestamp, list, map, set");
        }
    }


    @Override
    public boolean mustTraverse(Maybe<Literal> input) {
        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.__(Literal::getMap);
        return string.isPresent() || list.isPresent() || map.isPresent();

    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Literal> input) {
        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.__(Literal::getMap);
        if (mustTraverse(input)) {
            if (string.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(StringLiteralSemantics.class), string));
            } else if (list.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(ListLiteralExpressionSemantics.class), list));
            } else if (map.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(MapOrSetLiteralExpressionSemantics.class), map));
            }
        }

        return Optional.empty();

    }


    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Literal, ?, ?> input) {
        final Maybe<StringLiteralSimple> string = input.getPattern().__(Literal::getString);
        final Maybe<ListLiteral> list = input.getPattern().__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.getPattern().__(Literal::getMap);
        if (mustTraverse(input.getPattern())) {
            if (string.isPresent()) {
                return module.get(StringLiteralSemantics.class).compilePatternMatchInternal(
                        input.mapPattern(__ -> string.toNullable())
                );
            } else if (list.isPresent()) {
                return module.get(ListLiteralExpressionSemantics.class).compilePatternMatchInternal(
                        input.mapPattern(__ -> list.toNullable())
                );
            } else if (map.isPresent()) {
                return module.get(MapOrSetLiteralExpressionSemantics.class).compilePatternMatchInternal(
                        input.mapPattern(__ -> map.toNullable())
                );
            }
        }
        return input.createEmptyCompileOutput();
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<Literal, ?, ?> input) {
        final Maybe<StringLiteralSimple> string = input.getPattern().__(Literal::getString);
        final Maybe<ListLiteral> list = input.getPattern().__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.getPattern().__(Literal::getMap);
        if (mustTraverse(input.getPattern())) {
            if (string.isPresent()) {
                return module.get(StringLiteralSemantics.class).inferPatternTypeInternal(
                        input.mapPattern(__ -> string.toNullable())
                );
            } else if (list.isPresent()) {
                return module.get(ListLiteralExpressionSemantics.class).inferPatternTypeInternal(
                        input.mapPattern(__ -> list.toNullable())
                );
            } else if (map.isPresent()) {
                return module.get(MapOrSetLiteralExpressionSemantics.class).inferPatternTypeInternal(
                        input.mapPattern(__ -> map.toNullable())
                );
            }
        }
        return PatternType.empty(module);
    }

    @Override
    protected PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Literal, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<StringLiteralSimple> string = input.getPattern().__(Literal::getString);
        final Maybe<ListLiteral> list = input.getPattern().__(Literal::getList);
        final Maybe<MapOrSetLiteral> map = input.getPattern().__(Literal::getMap);
        if (mustTraverse(input.getPattern())) {
            if (string.isPresent()) {
                return module.get(StringLiteralSemantics.class).validatePatternMatchInternal(
                        input.mapPattern(__ -> string.toNullable()),
                        acceptor
                );
            } else if (list.isPresent()) {
                return module.get(ListLiteralExpressionSemantics.class).validatePatternMatchInternal(
                        input.mapPattern(__ -> list.toNullable()),
                        acceptor
                );
            } else if (map.isPresent()) {
                return module.get(MapOrSetLiteralExpressionSemantics.class).validatePatternMatchInternal(
                        input.mapPattern(__ -> map.toNullable()),
                        acceptor
                );
            }
        }
        return input.createEmptyValidationOutput();
    }

    @Override
    public void validate(Maybe<Literal> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;

        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> bool = input.__(Literal::getBool);
        final Maybe<ListLiteral> list = input.__(Literal::getList);

        final Maybe<MapOrSetLiteral> map = input.__(Literal::getMap);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);
        if (string.isPresent()) {
            module.get(StringLiteralSemantics.class).validate(string, acceptor);
        } else if (list.isPresent()) {
            module.get(ListLiteralExpressionSemantics.class).validate(list, acceptor);
        } else if (number.isPresent()) {
            input.safeDo(inputSafe -> {
                validateNumberLiteral(number.toNullable(), inputSafe, acceptor);
            });
        } else if (bool.isPresent() || timestamp.isPresent()) {
            //nothing to validate
        } else {
            if (map.isPresent()) {
                module.get(MapOrSetLiteralExpressionSemantics.class).validate(map, acceptor);
            } else {
                throw new UnsupportedNodeType("Literals supported: text, integer, real, boolean, " +
                        "list, map, set, duration, timestamp, aid and performative.");
            }
        }
    }

    private void validateNumberLiteral(
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
        } catch (Exception e) {
            acceptor.acceptError(
                    "Invalid number format: " + e.getMessage(),
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidNumberFormat"
            );
        }
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


}
