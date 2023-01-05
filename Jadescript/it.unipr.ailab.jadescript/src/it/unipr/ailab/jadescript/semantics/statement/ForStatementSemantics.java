package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ForStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 26/04/18.
 */
@Singleton
public class ForStatementSemantics extends StatementSemantics<ForStatement> {


    public ForStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void compileStatement(Maybe<ForStatement> input, CompilationOutputAcceptor acceptor) {
        Maybe<RValueExpression> collection = input.__(ForStatement::getCollection);
        Maybe<String> varName = input.__(ForStatement::getVarName);
        Maybe<String> var2Name = input.__(ForStatement::getVar2Name);
        Maybe<OptionalBlock> forBody = input.__(ForStatement::getForBody);
        IJadescriptType collectionType = module.get(RValueExpressionSemantics.class).inferType(collection, );
        IJadescriptType firstVarType;

        if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)) {
            firstVarType = module.get(TypeHelper.class).INTEGER;
        } else if (collectionType instanceof MapType) {
            firstVarType = ((MapType) collectionType).getKeyType();
        } else if (collectionType instanceof ListType) {
            firstVarType = ((ListType) collectionType).getElementType();
        } else if (collectionType instanceof SetType) {
            firstVarType = ((SetType) collectionType).getElementType();
        } else {
            firstVarType = module.get(TypeHelper.class).ANY;
        }


        module.get(ContextManager.class).pushScope();
        module.get(ContextManager.class).currentScope().addUserVariable(
                varName.extract(nullAsEmptyString),
                firstVarType,
                true
        );

        final String compiledCollection = module.get(RValueExpressionSemantics.class)
                .compile(collection, , acceptor);
        if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)) {

            Maybe<RValueExpression> end = input.__(ForStatement::getEndIndex);

            final String compiledEndIndex = module.get(RValueExpressionSemantics.class)
                    .compile(end, , acceptor);

            ExpressionWriter completeCollExpression = w.expr(
                    "new jadescript.util.IntegerRange(" + compiledCollection + ", "
                            + compiledEndIndex + ", true, true)"
            );

            acceptor.accept(w.foreach(
                    firstVarType.compileToJavaTypeReference(),
                    varName.extract(nullAsEmptyString),
                    completeCollExpression,
                    module.get(BlockSemantics.class).compileOptionalBlock(forBody)
            ));

        } else if (input.__(ForStatement::isMapIteration).extract(nullAsFalse)
                && collectionType instanceof MapType) {

            final MapType mapType = ((MapType) collectionType);
            firstVarType = mapType.getKeyType();
            IJadescriptType secondVarType = mapType.getValueType();
            module.get(ContextManager.class).currentScope().addUserVariable(
                    var2Name.extract(nullAsEmptyString),
                    secondVarType,
                    true
            );
            String collectionAuxVar = acceptor.auxiliaryVariable(
                    collection,
                    mapType.compileToJavaTypeReference(),
                    "collection",
                    compiledCollection
            );
            BlockWriter block = module.get(BlockSemantics.class).compileOptionalBlock(forBody);
            block.addStatement(0, w.variable(secondVarType.compileToJavaTypeReference(), var2Name.extract(nullAsEmptyString),
                    w.expr(collectionAuxVar + ".get(" + varName.extract(nullAsEmptyString) + ")")
            ));
            acceptor.accept(w.foreach(
                    firstVarType.compileToJavaTypeReference(),
                    varName.extract(nullAsEmptyString),
                    w.expr(collectionAuxVar + ".keySet()"),
                    module.get(BlockSemantics.class).compileOptionalBlock(forBody)
            ));
        } else {
            acceptor.accept(w.foreach(
                    firstVarType.compileToJavaTypeReference(),
                    varName.extract(nullAsEmptyString),
                    w.expr(compiledCollection),
                    module.get(BlockSemantics.class).compileOptionalBlock(forBody)
            ));
        }


        module.get(ContextManager.class).popScope();
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<ForStatement> input) {
        return Stream.of(input.__(ForStatement::getCollection), input.__(ForStatement::getEndIndex))
                .filter(Maybe::isPresent)
                .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    @Override
    public void validate(Maybe<ForStatement> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;

        if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)
                && input.__(ForStatement::isMapIteration).extract(nullAsFalse)) {

            input.safeDo(inputSafe -> {

                acceptor.acceptError(
                        "Cannot iterate by pairs in an indexed loop",
                        inputSafe,
                        JadescriptPackage.eINSTANCE.getForStatement_Var2Name(),
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        "InvalidForStatement"
                );
            });

        } else {
            Maybe<RValueExpression> collection = input.__(ForStatement::getCollection);
            Maybe<RValueExpression> endIndex = input.__(ForStatement::getEndIndex);
            Maybe<String> varName = input.__(ForStatement::getVarName);
            Maybe<String> var2Name = input.__(ForStatement::getVar2Name);
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(RValueExpressionSemantics.class).validate(collection, , interceptAcceptor);
            if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)) {
                module.get(RValueExpressionSemantics.class).validate(endIndex, , interceptAcceptor);
            }
            if (!interceptAcceptor.thereAreErrors()) {

                varName.safeDo(varNameSafe -> {
                    module.get(ValidationHelper.class).asserting(
                            module.get(ContextManager.class).currentContext().searchAs(
                                    NamedSymbol.Searcher.class,
                                    (NamedSymbol.Searcher s) -> s.searchName(
                                            varNameSafe::equals,
                                            null,
                                            null
                                    )
                            ).findAny().isEmpty(),
                            "AlreadyDefinedVariable",
                            "A variable with same name is already defined in this scope",
                            input,
                            JadescriptPackage.eINSTANCE.getForStatement_VarName(),
                            acceptor
                    );
                });


                IJadescriptType collectionType = module.get(RValueExpressionSemantics.class).inferType(collection, );

                if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)) {
                    @SuppressWarnings("UnnecessaryLocalVariable") IJadescriptType startType = collectionType;
                    IJadescriptType endType = module.get(RValueExpressionSemantics.class).inferType(endIndex, );
                    module.get(ValidationHelper.class).assertExpectedType(module.get(TypeHelper.class).INTEGER, startType,
                            "InvalidIndexType",
                            input,
                            JadescriptPackage.eINSTANCE.getForStatement_Collection(),
                            acceptor
                    );
                    module.get(ValidationHelper.class).assertExpectedType(module.get(TypeHelper.class).INTEGER, endType,
                            "InvalidIndexType",
                            input,
                            JadescriptPackage.eINSTANCE.getForStatement_EndIndex(),
                            acceptor
                    );
                    IJadescriptType varType = module.get(TypeHelper.class).INTEGER;

                    validateBody(input, varType, nothing(), acceptor);

                } else {
                    if (input.__(ForStatement::isMapIteration).extract(nullAsFalse)) {
                        var2Name.safeDo(var2NameSafe -> {
                            module.get(ValidationHelper.class).asserting(
                                    module.get(ContextManager.class).currentContext().searchAs(
                                            NamedSymbol.Searcher.class,
                                            (NamedSymbol.Searcher s) -> s.searchName(
                                                    var2NameSafe::equals,
                                                    null,
                                                    null
                                            )
                                    ).findAny().isEmpty(),
                                    "AlreadyDefinedVariable",
                                    "A variable with same name is already defined in this scope",
                                    input,
                                    JadescriptPackage.eINSTANCE.getForStatement_Var2Name(),
                                    acceptor
                            );
                        });

                        module.get(ValidationHelper.class).asserting(
                                collectionType instanceof MapType,
                                "InvalidMapType",
                                "Invalid collection type, map expected, found: " + collectionType,
                                input,
                                JadescriptPackage.eINSTANCE.getForStatement_Collection(),
                                interceptAcceptor
                        );
                        if (!interceptAcceptor.thereAreErrors() && collectionType instanceof MapType) {
                            final MapType mapTypeDescriptor = ((MapType) collectionType);
                            IJadescriptType firstVarType = mapTypeDescriptor.getKeyType();
                            IJadescriptType secondVarType = mapTypeDescriptor.getValueType();
                            validateBody(input, firstVarType, some(secondVarType), acceptor);
                        }
                    } else {
                        IJadescriptType firstVarType;

                        if (collectionType instanceof MapType) {
                            firstVarType = ((MapType) collectionType).getKeyType();
                        } else if (collectionType instanceof ListType) {
                            firstVarType = ((ListType) collectionType).getElementType();
                        } else if (collectionType instanceof SetType) {
                            firstVarType = ((SetType) collectionType).getElementType();
                        } else {
                            input.safeDo(inputSafe -> {
                                acceptor.acceptError(
                                        "This expression should return a list, a set, or a map; type found: "
                                                + collectionType,
                                        inputSafe,
                                        JadescriptPackage.eINSTANCE.getForStatement_Collection(),
                                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                                        "InvalidCollection"
                                );

                            });
                            return;
                        }
                        validateBody(input, firstVarType, nothing(), acceptor);
                    }
                }


            }
            module.get(ValidationHelper.class).assertNotReservedName(
                    varName,
                    input,
                    JadescriptPackage.eINSTANCE.getForStatement_VarName(),
                    acceptor
            );
            module.get(ValidationHelper.class).assertNotReservedName(
                    var2Name,
                    input,
                    JadescriptPackage.eINSTANCE.getForStatement_Var2Name(),
                    acceptor
            );
        }


    }


    private void validateBody(
            Maybe<ForStatement> input,
            IJadescriptType var1Type,
            Maybe<IJadescriptType> var2Type,
            ValidationMessageAcceptor acceptor
    ) {

        Maybe<String> varName = input.__(ForStatement::getVarName);
        Maybe<String> var2Name = input.__(ForStatement::getVar2Name);
        Maybe<OptionalBlock> forBody = input.__(ForStatement::getForBody);

        module.get(ContextManager.class).pushScope();
        varName.safeDo(varNameSafe -> {
            module.get(ContextManager.class).currentScope().addUserVariable(varNameSafe, var1Type, true);
        });
        input.safeDo(inputSafe -> {
            if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
                acceptor.acceptInfo(
                        "Inferred type: " + var1Type,
                        inputSafe,
                        JadescriptPackage.eINSTANCE.getForStatement_VarName(),
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        ISSUE_CODE_PREFIX + "Info"
                );
            }
        });

        if (var2Type.isPresent()) {
            var2Name.safeDo(var2NameSafe -> {
                module.get(ContextManager.class).currentScope().addUserVariable(var2NameSafe, var2Type.toNullable(), true);
            });
            input.safeDo(inputSafe -> {
                if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
                    acceptor.acceptInfo(
                            "Inferred type: " + var2Type.toNullable(),
                            inputSafe,
                            JadescriptPackage.eINSTANCE.getForStatement_Var2Name(),
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            ISSUE_CODE_PREFIX + "Info"
                    );
                }
            });
        }


        module.get(BlockSemantics.class).validateOptionalBlock(forBody, acceptor);


        module.get(ContextManager.class).popScope();
    }
}
