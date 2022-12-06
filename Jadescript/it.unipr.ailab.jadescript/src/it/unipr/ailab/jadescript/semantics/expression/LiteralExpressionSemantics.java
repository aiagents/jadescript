package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
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
     * When true, the input is a map because all the 'things' between commas are key:value pairs or because its an empty
     * ('{:}') map literal.
     */
    private boolean isMapV(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        return input.__(MapOrSetLiteral::isIsMap).extract(nullAsFalse) && !values.isEmpty();
    }


    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<Literal> input) {
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> mapOrSet = input.__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);

        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        if (list.isPresent()) {
            final SemanticsBoundToExpression<?> extract =
                    list.extract(x -> new SemanticsBoundToExpression<>(
                            module.get(ListLiteralExpressionSemantics.class),
                            list
                    ));
            return List.of(extract);
        } else if (mapOrSet.isPresent()) {
            if (isMap) {
                final SemanticsBoundToExpression<?> extract = mapOrSet.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(MapLiteralExpressionSemantics.class),
                        mapOrSet
                ));
                return List.of(extract);
            } else {
                final SemanticsBoundToExpression<?> extract = mapOrSet.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(SetLiteralExpressionSemantics.class),
                        mapOrSet
                ));
                return List.of(extract);
            }
        } else {
            return Collections.emptyList();
        }

    }

    @Override
    public Maybe<String> compile(Maybe<Literal> input) {
        if (input == null) return nothing();

        final Maybe<StringLiteralSimple> string = input.__(Literal::getString);
        final Maybe<String> number = input.__(Literal::getNumber);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);
        final Maybe<String> bool = input.__(Literal::getBool);
        final Maybe<ListLiteral> list = input.__(Literal::getList);
        final Maybe<MapOrSetLiteral> mapOrSet = input.__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);

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
        } else if (mapOrSet.isPresent()) {
            if (isMap) {
                return module.get(MapLiteralExpressionSemantics.class).compile(mapOrSet);
            } else {
                return module.get(SetLiteralExpressionSemantics.class).compile(mapOrSet);
            }
        } else {
            throw new UnsupportedNodeType("Literals supported now: text, integer, float, boolean, timestamp, list, " +
                    "map, set");
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
        final Maybe<MapOrSetLiteral> mapOrSet = input.__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);


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
        } else if (mapOrSet.isPresent()) {
            if (isMap) {
                return module.get(MapLiteralExpressionSemantics.class).inferType(mapOrSet);
            } else {
                return module.get(SetLiteralExpressionSemantics.class).inferType(mapOrSet);
            }
        } else {
            throw new UnsupportedNodeType("Literals supported now: text, integer, real, boolean, timestamp, list, " +
                    "map, set");
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
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Literal, ?, ?> input) {
        final Maybe<StringLiteralSimple> string = input.getPattern().__(Literal::getString);
        final Maybe<ListLiteral> list = input.getPattern().__(Literal::getList);
        final Maybe<MapOrSetLiteral> mapOrSet = input.getPattern().__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);
        if (mustTraverse(input.getPattern())) {
            if (string.isPresent()) {
                return module.get(StringLiteralSemantics.class).compilePatternMatchInternal(
                        input.mapPattern(__ -> string.toNullable())
                );
            } else if (list.isPresent()) {
                return module.get(ListLiteralExpressionSemantics.class).compilePatternMatchInternal(
                        input.mapPattern(__ -> list.toNullable())
                );
            } else if (mapOrSet.isPresent()) {
                if (isMap) {
                    return module.get(MapLiteralExpressionSemantics.class).compilePatternMatchInternal(
                            input.mapPattern(__ -> mapOrSet.toNullable())
                    );
                } else {
                    return module.get(SetLiteralExpressionSemantics.class).compilePatternMatchInternal(
                            input.mapPattern(__ -> mapOrSet.toNullable())
                    );
                }
            }
        }
        return input.createEmptyCompileOutput();
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<Literal, ?, ?> input) {
        final Maybe<StringLiteralSimple> string = input.getPattern().__(Literal::getString);
        final Maybe<ListLiteral> list = input.getPattern().__(Literal::getList);
        final Maybe<MapOrSetLiteral> mapOrSet = input.getPattern().__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);

        if (mustTraverse(input.getPattern())) {
            if (string.isPresent()) {
                return module.get(StringLiteralSemantics.class).inferPatternTypeInternal(
                        input.mapPattern(__ -> string.toNullable())
                );
            } else if (list.isPresent()) {
                return module.get(ListLiteralExpressionSemantics.class).inferPatternTypeInternal(
                        input.mapPattern(__ -> list.toNullable())
                );
            } else if (mapOrSet.isPresent()) {
                if (isMap) {
                    return module.get(MapLiteralExpressionSemantics.class).inferPatternTypeInternal(
                            input.mapPattern(__ -> mapOrSet.toNullable())
                    );
                } else {
                    return module.get(SetLiteralExpressionSemantics.class).inferPatternTypeInternal(
                            input.mapPattern(__ -> mapOrSet.toNullable())
                    );
                }
            }
        }
        return PatternType.empty(module);
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Literal, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<StringLiteralSimple> string = input.getPattern().__(Literal::getString);
        final Maybe<ListLiteral> list = input.getPattern().__(Literal::getList);
        final Maybe<MapOrSetLiteral> mapOrSet = input.getPattern().__(Literal::getMap);
        final boolean isMap = isMap(mapOrSet);

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
            } else if (mapOrSet.isPresent()) {
                if (isValidSyntactically(mapOrSet, "pattern", acceptor)) {
                    if (isMap) {
                        return module.get(MapLiteralExpressionSemantics.class).validatePatternMatchInternal(
                                input.mapPattern(__ -> mapOrSet.toNullable()),
                                acceptor
                        );
                    } else {
                        return module.get(SetLiteralExpressionSemantics.class).validatePatternMatchInternal(
                                input.mapPattern(__ -> mapOrSet.toNullable()),
                                acceptor
                        );
                    }
                }
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
        final Maybe<MapOrSetLiteral> mapOrSet = input.__(Literal::getMap);
        final Maybe<String> timestamp = input.__(Literal::getTimestamp);
        final boolean isMap = isMap(mapOrSet);


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
            if (mapOrSet.isPresent()) {
                if (isValidSyntactically(mapOrSet, "literal", acceptor)) {
                    if (isMap) {
                        module.get(MapLiteralExpressionSemantics.class).validate(mapOrSet, acceptor);
                    } else {
                        module.get(SetLiteralExpressionSemantics.class).validate(mapOrSet, acceptor);
                    }
                }
            } else {
                throw new UnsupportedNodeType("Literals supported: text, integer, real, boolean, " +
                        "list, map, set, timestamp, aid");
            }
        }
    }

    private boolean isValidSyntactically(
            Maybe<MapOrSetLiteral> input,
            String literalOrPattern,
            ValidationMessageAcceptor acceptor
    ) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues))
                .stream().filter(Maybe::isPresent).collect(Collectors.toList());
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys))
                .stream().filter(Maybe::isPresent).collect(Collectors.toList());
        final boolean isMapV = isMapV(input);
        final boolean isMapT = isMapT(input);
        InterceptAcceptor syntacticValidation = new InterceptAcceptor(acceptor);
        final ValidationHelper vh = module.get(ValidationHelper.class);

        if (isMapV) {
            vh.assertion(
                    values.size() == keys.size(),
                    "InvalidMap" + Strings.toFirstUpper(literalOrPattern),
                    "Non-matching number of keys and values in the map " + literalOrPattern,
                    input,
                    syntacticValidation
            );
        }

        if (isMapT) {
            vh.assertion(
                    isMapV,
                    "InvalidSetOrMap" + Strings.toFirstUpper(literalOrPattern),
                    "Type specifiers of the literal do not match the kind of the " + literalOrPattern +
                            " (is this a set or a map?).",
                    input,
                    syntacticValidation
            );
        }


        return !syntacticValidation.thereAreErrors();
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
