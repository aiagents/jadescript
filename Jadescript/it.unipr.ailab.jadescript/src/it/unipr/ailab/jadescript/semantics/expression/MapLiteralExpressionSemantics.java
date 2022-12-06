package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.MapOrSetLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.zip;

public class MapLiteralExpressionSemantics extends ExpressionSemantics<MapOrSetLiteral> {

    //TODO pipe operator
    public MapLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }


    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        return zip(keys.stream(), values.stream(), (k, v) -> Stream.of(
                new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), k),
                new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), v)
        )).flatMap(x -> x)
                .collect(Collectors.toList());

    }

    @Override
    public Maybe<String> compile(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);

        if (values.isEmpty() || keys.isEmpty()
                || values.stream().allMatch(Maybe::isNothing)
                || keys.stream().allMatch(Maybe::isNothing)) {

            return Maybe.of(module.get(TypeHelper.class).MAP
                    .apply(List.of(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                            module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter)
                    )).compileNewEmptyInstance());
        }


        StringBuilder sb = new StringBuilder("jadescript.util.JadescriptCollections.createMap(");


        sb.append("java.util.Arrays.asList(");
        for (int i = 0; i < keys.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(keys.get(i)).orElse(""));
        }


        sb.append("), java.util.Arrays.asList(");
        for (int i = 0; i < values.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(values.get(i)).orElse(""));
        }


        sb.append("))");

        return Maybe.of(sb.toString());
    }

    @Override
    public IJadescriptType inferType(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);


        if ((values.isEmpty() || keys.isEmpty()
                || (keysTypeParameter.isPresent() && valuesTypeParameter.isPresent()))) {
            return module.get(TypeHelper.class).MAP.apply(Arrays.asList(
                    module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                    module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter)
            ));
        }


        IJadescriptType lubKeys = module.get(RValueExpressionSemantics.class).inferType(keys.get(0));
        for (int i = 1; i < keys.size(); i++) {
            lubKeys = module.get(TypeHelper.class).getLUB(lubKeys, module.get(RValueExpressionSemantics.class).inferType(keys.get(i)));
        }

        IJadescriptType lubValues = module.get(RValueExpressionSemantics.class).inferType(values.get(0));
        for (int i = 1; i < values.size(); i++) {
            lubValues = module.get(TypeHelper.class).getLUB(lubValues, module.get(RValueExpressionSemantics.class).inferType(values.get(i)));
        }
        return module.get(TypeHelper.class).MAP.apply(Arrays.asList(lubKeys, lubValues));
    }

    @Override
    public void validate(Maybe<MapOrSetLiteral> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final boolean hasTypeSpecifiers = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(Maybe.nullAsFalse);
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);

        InterceptAcceptor stage1Validation = new InterceptAcceptor(acceptor);

        for (Maybe<RValueExpression> key : keys) {
            module.get(RValueExpressionSemantics.class).validate(key, stage1Validation);
        }

        for (Maybe<RValueExpression> value : values) {
            module.get(RValueExpressionSemantics.class).validate(value, stage1Validation);
        }

        module.get(ValidationHelper.class).assertion(
                (!values.isEmpty() && !values.stream().allMatch(Maybe::isNothing)
                        && !keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing))
                        || hasTypeSpecifiers,
                "MapLiteralCannotComputeTypes",
                "Missing type specifications for empty map literal",
                input,
                stage1Validation
        );

        module.get(ValidationHelper.class).assertion(
                values.stream().filter(Maybe::isPresent).count()
                        == keys.stream().filter(Maybe::isPresent).count(),
                "InvalidMapLiteral",
                "Non-matching number of keys and values in the map",
                input,
                stage1Validation
        );


        if (!stage1Validation.thereAreErrors()) {
            if (!values.isEmpty() && !values.stream().allMatch(Maybe::isNothing)
                    && !keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing)) {
                IJadescriptType keysLub = module.get(RValueExpressionSemantics.class).inferType(keys.get(0));
                IJadescriptType valuesLub = module.get(RValueExpressionSemantics.class).inferType(values.get(0));
                for (int i = 1; i < Math.min(keys.size(), values.size()); i++) {
                    keysLub = module.get(TypeHelper.class).getLUB(keysLub, module.get(RValueExpressionSemantics.class).inferType(keys.get(i)));
                    valuesLub = module.get(TypeHelper.class).getLUB(valuesLub, module.get(RValueExpressionSemantics.class).inferType(values.get(i)));
                }

                InterceptAcceptor interceptAcceptorK = new InterceptAcceptor(acceptor);
                module.get(ValidationHelper.class).assertion(
                        !keysLub.isErroneous(),
                        "MapLiteralCannotComputeType",
                        "Can not find a valid common parent type of the keys in the map.",
                        input,
                        interceptAcceptorK
                );

                module.get(TypeExpressionSemantics.class).validate(keysTypeParameter, interceptAcceptorK);
                if (!interceptAcceptorK.thereAreErrors() && hasTypeSpecifiers) {
                    module.get(ValidationHelper.class).assertExpectedType(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                            keysLub,
                            "MapLiteralTypeMismatch",
                            input,
                            JadescriptPackage.eINSTANCE.getMapOrSetLiteral_KeyTypeParameter(),
                            acceptor
                    );
                }
                InterceptAcceptor interceptAcceptorV = new InterceptAcceptor(acceptor);
                module.get(ValidationHelper.class).assertion(
                        !valuesLub.isErroneous(),
                        "MapLiteralCannotComputeType",
                        "Can not find a valid common parent type of the values in the map.",
                        input,
                        interceptAcceptorV
                );

                module.get(TypeExpressionSemantics.class).validate(valuesTypeParameter, interceptAcceptorV);
                if (!interceptAcceptorV.thereAreErrors() && hasTypeSpecifiers) {
                    module.get(ValidationHelper.class).assertExpectedType(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter),
                            valuesLub,
                            "MapLiteralTypeMismatch",
                            input,
                            JadescriptPackage.eINSTANCE.getMapOrSetLiteral_ValueTypeParameter(),
                            acceptor
                    );
                }
            }
        }
    }

    @Override
    public boolean mustTraverse(Maybe<MapOrSetLiteral> input) {
        return false;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<MapOrSetLiteral> input) {
        return Optional.empty();
    }

}
