package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ForStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.SetType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 26/04/18.
 */
@Singleton
public class ForStatementSemantics extends StatementSemantics<ForStatement> {


    public ForStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    //TODO new var assignment semantics
    @Override
    public StaticState compileStatement(
        Maybe<ForStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        Maybe<RValueExpression> collection =
            input.__(ForStatement::getCollection);
        Maybe<String> varName =
            input.__(ForStatement::getVarName);
        Maybe<String> var2Name =
            input.__(ForStatement::getVar2Name);
        Maybe<OptionalBlock> forBody =
            input.__(ForStatement::getForBody);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        IJadescriptType collectionType = rves.inferType(collection, state);

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
            firstVarType = module.get(TypeHelper.class).TOP.apply(
                "Unexpected collection type: " +
                    collectionType.getJadescriptName()
            );
        }

        final String compiledCollection = rves.compile(
            collection,
            state,
            acceptor
        );

        final StaticState afterCollection = rves.advance(collection, state);

        if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)) {
            Maybe<RValueExpression> end = input.__(ForStatement::getEndIndex);

            final String compiledEndIndex =
                rves.compile(end, afterCollection, acceptor);

            final StaticState afterEndIndex =
                rves.advance(end, afterCollection);

            ExpressionWriter completeCollExpression = w.expr(
                "new jadescript.util.IntegerRange(" + compiledCollection +
                    ", " + compiledEndIndex + ", true, true)"
            );

            StaticState withVar = afterEndIndex.declareName(
                new UserVariable(
                    varName.extract(nullAsEmptyString),
                    firstVarType,
                    true
                ) //TODO specific NameMember for for-variables
            );


            StaticState inBlockScope = withVar.enterLoopScope();

            final PSR<BlockWriter> blockPSR = module.get(BlockSemantics.class)
                .compileOptionalBlock(
                    forBody,
                    inBlockScope
                );

            BlockWriter compiledBlock = blockPSR.result();

            StaticState endOfBlock = blockPSR.state();

            StaticState afterBlock = endOfBlock.exitScope();

            acceptor.accept(w.foreach(
                firstVarType.compileToJavaTypeReference(),
                varName.extract(nullAsEmptyString),
                completeCollExpression,
                compiledBlock
            ));

            return afterEndIndex.intersectAlternative(afterBlock);

        } else if (
            input.__(ForStatement::isMapIteration).extract(nullAsFalse)
                && collectionType instanceof MapType
        ) {

            final MapType mapType = ((MapType) collectionType);
            firstVarType = mapType.getKeyType();
            IJadescriptType secondVarType = mapType.getValueType();

            String collectionAuxVar = acceptor.auxiliaryVariable(
                collection,
                mapType.compileToJavaTypeReference(),
                "collection",
                compiledCollection
            );


            StaticState withVars = afterCollection
                .declareName(
                    new UserVariable(
                        varName.extract(nullAsEmptyString),
                        firstVarType,
                        true
                    )
                ).declareName(
                    new UserVariable(
                        var2Name.extract(nullAsEmptyString),
                        secondVarType,
                        true
                    )
                );


            StaticState inBlock = withVars.enterLoopScope();

            PSR<BlockWriter> blockPSR = module.get(BlockSemantics.class)
                .compileOptionalBlock(
                    forBody,
                    inBlock
                );
            BlockWriter compiledBlock = blockPSR.result();
            StaticState endOfBlock = blockPSR.state();

            StaticState afterBlock = endOfBlock.exitScope();


            compiledBlock.addStatement(
                0,
                w.variable(
                    secondVarType.compileToJavaTypeReference(),
                    var2Name.extract(nullAsEmptyString),
                    w.expr(collectionAuxVar + ".get(" + varName.extract(
                        nullAsEmptyString) + ")")
                )
            );

            acceptor.accept(w.foreach(
                firstVarType.compileToJavaTypeReference(),
                varName.extract(nullAsEmptyString),
                w.expr(collectionAuxVar + ".keySet()"),
                compiledBlock
            ));

            return afterCollection.intersectAlternative(afterBlock);
        } else {
            // Something's wrong: best-effort compiling

            StaticState withVar = afterCollection
                .declareName(
                    new UserVariable(
                        varName.extract(nullAsEmptyString),
                        firstVarType,
                        true
                    )
                );

            StaticState inBlock = withVar.enterLoopScope();

            final PSR<BlockWriter> blockPSR = module.get(BlockSemantics.class)
                .compileOptionalBlock(
                    forBody,
                    inBlock
                );

            final BlockWriter blockCompiled = blockPSR.result();

            final StaticState endOfBlock = blockPSR.state();

            final StaticState afterBlock = endOfBlock.exitScope();

            acceptor.accept(w.foreach(
                firstVarType.compileToJavaTypeReference(),
                varName.extract(nullAsEmptyString),
                w.expr(compiledCollection),
                blockCompiled
            ));


            return afterCollection.intersectAlternative(afterBlock);
        }


    }


    @Override
    public StaticState validateStatement(
        Maybe<ForStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return state;

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
            return state;
        }


        Maybe<RValueExpression> collection =
            input.__(ForStatement::getCollection);
        Maybe<RValueExpression> endIndex =
            input.__(ForStatement::getEndIndex);
        Maybe<String> varName = input.__(ForStatement::getVarName);
        Maybe<String> var2Name = input.__(ForStatement::getVar2Name);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        boolean collectionCheck = rves.validate(
            collection,
            state,
            acceptor
        );
        if (collectionCheck == INVALID) {
            //just validate body and return

            final StaticState inBlock = state.enterLoopScope();

            StaticState endOfBlock =
                module.get(BlockSemantics.class).validateOptionalBlock(
                    input.__(ForStatement::getForBody),
                    inBlock,
                    acceptor
                );

            return endOfBlock.exitScope();
        }

        IJadescriptType collectionType = rves.inferType(collection, state);

        StaticState afterCollection = rves.advance(collection, state);

        StaticState afterForHeader;
        boolean endIndexCheck;
        if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)) {
            endIndexCheck = rves.validate(
                endIndex,
                afterCollection,
                acceptor
            );
            if (endIndexCheck == VALID) {
                afterForHeader = rves.advance(endIndex, afterCollection);
            } else {
                afterForHeader = afterCollection;
            }
        } else {
            afterForHeader = afterCollection;
            endIndexCheck = VALID;
        }

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (collectionCheck == INVALID || endIndexCheck == INVALID) {
            return state;
        }

        if (varName.isPresent()) {
            validationHelper.asserting(
                state.searchAs(
                    NameMember.Namespace.class,
                    (NameMember.Namespace s) -> s.searchName(
                        varName.extract(nullAsEmptyString),
                        null,
                        null
                    )
                ).findAny().isEmpty(),
                "AlreadyDefinedVariable",
                "A variable with same name is already defined in this" +
                    " scope",
                input,
                JadescriptPackage.eINSTANCE.getForStatement_VarName(),
                acceptor
            );

            validationHelper.assertNotReservedName(
                varName,
                input,
                JadescriptPackage.eINSTANCE.getForStatement_VarName(),
                acceptor
            );
        }


        if (input.__(ForStatement::isIndexedLoop).extract(nullAsFalse)) {
            @SuppressWarnings("UnnecessaryLocalVariable")
            IJadescriptType startType = collectionType;
            IJadescriptType endType = rves.inferType(
                endIndex,
                afterCollection
            );
            validationHelper.assertExpectedType(module.get(
                    TypeHelper.class).INTEGER, startType,
                "InvalidIndexType",
                input,
                JadescriptPackage.eINSTANCE.getForStatement_Collection(),
                acceptor
            );
            validationHelper.assertExpectedType(module.get(
                    TypeHelper.class).INTEGER, endType,
                "InvalidIndexType",
                input,
                JadescriptPackage.eINSTANCE.getForStatement_EndIndex(),
                acceptor
            );
            IJadescriptType varType =
                module.get(TypeHelper.class).INTEGER;

            final StaticState afterBody = validateBody(
                input,
                varType,
                nothing(),
                afterForHeader,
                acceptor
            );

            return afterForHeader.intersectAlternative(afterBody);

        } else {
            if (input.__(ForStatement::isMapIteration).extract(
                nullAsFalse)) {
                if (var2Name.isPresent()) {
                    validationHelper.asserting(
                        state.searchAs(
                            NameMember.Namespace.class,
                            (NameMember.Namespace s) -> s.searchName(
                                var2Name.orElse(""),
                                null,
                                null
                            )
                        ).findAny().isEmpty(),
                        "AlreadyDefinedVariable",
                        "A variable with same name is already defined" +
                            " in this scope",
                        input,
                        JadescriptPackage.eINSTANCE
                            .getForStatement_Var2Name(),
                        acceptor
                    );

                    validationHelper.assertNotReservedName(
                        var2Name,
                        input,
                        JadescriptPackage.eINSTANCE.getForStatement_Var2Name(),
                        acceptor
                    );

                    if (varName.isPresent() && var2Name.toNullable()
                        .equals(varName.toNullable())) {
                        validationHelper.emitError(
                            "AlreadyDefinedVariable",
                            "A variable with same name is already defined" +
                                " in this scope",
                            input,
                            JadescriptPackage.eINSTANCE
                                .getForStatement_Var2Name(),
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            acceptor
                        );
                    }
                }


                boolean isMapTypeCheck = validationHelper.asserting(
                    collectionType instanceof MapType,
                    "InvalidMapType",
                    "Invalid collection type, map expected, " +
                        "found: " + collectionType,
                    collection,
                    acceptor
                );

                if (isMapTypeCheck != VALID
                    || !(collectionType instanceof MapType)) {
                    return afterForHeader;
                }

                final MapType mapTypeDescriptor = ((MapType) collectionType);
                IJadescriptType firstVarType = mapTypeDescriptor.getKeyType();
                IJadescriptType secondVarType =
                    mapTypeDescriptor.getValueType();

                final StaticState afterBody = validateBody(
                    input,
                    firstVarType,
                    some(secondVarType),
                    afterForHeader,
                    acceptor
                );

                return afterForHeader.intersectAlternative(afterBody);
            } else {
                IJadescriptType firstVarType;
                if (collectionType instanceof MapType) {
                    firstVarType =
                        ((MapType) collectionType).getKeyType();
                } else if (collectionType instanceof ListType) {
                    firstVarType =
                        ((ListType) collectionType).getElementType();
                } else if (collectionType instanceof SetType) {
                    firstVarType =
                        ((SetType) collectionType).getElementType();
                } else {
                    validationHelper.emitError(
                        "InvalidCollection",
                        "This expression should return a list, a " +
                            "set, or a map; type found: "
                            + collectionType,
                        input,
                        JadescriptPackage.eINSTANCE
                            .getForStatement_Collection(),
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        acceptor
                    );


                    return afterForHeader;
                }
                final StaticState afterBody = validateBody(
                    input,
                    firstVarType,
                    nothing(),
                    afterForHeader,
                    acceptor
                );
                return afterForHeader.intersectAlternative(afterBody);
            }
        }
    }


    private StaticState validateBody(
        Maybe<ForStatement> input,
        IJadescriptType var1Type,
        Maybe<IJadescriptType> var2Type,
        StaticState afterForHeader,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<String> varName = input.__(ForStatement::getVarName);
        Maybe<String> var2Name = input.__(ForStatement::getVar2Name);
        Maybe<OptionalBlock> forBody = input.__(ForStatement::getForBody);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        StaticState withVars = afterForHeader;
        if (varName.isPresent()) {
            withVars = withVars.declareName(new UserVariable(
                varName.orElse(""),
                var1Type,
                true
            ));
        }

        if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
            validationHelper.emitInfo(
                ISSUE_CODE_PREFIX + "Info",
                "Inferred type: " + var1Type,
                input,
                JadescriptPackage.eINSTANCE.getForStatement_VarName(),
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
            );
        }

        if (var2Type.isPresent()) {
            if (var2Name.isPresent()) {
                withVars = withVars.declareName(new UserVariable(
                    var2Name.orElse(""),
                    var2Type.toNullable(),
                    true
                ));
            }

            if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
                validationHelper.emitInfo(
                    ISSUE_CODE_PREFIX + "Info",
                    "Inferred type: " + var2Type,
                    input,
                    JadescriptPackage.eINSTANCE.getForStatement_VarName(),
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    acceptor
                );
            }

        }

        StaticState inBlock = withVars.enterLoopScope();

        final StaticState endOfBlock =
            module.get(BlockSemantics.class).validateOptionalBlock(
                forBody,
                inBlock,
                acceptor
            );

        return endOfBlock.exitScope();


    }

}
