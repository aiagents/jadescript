package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FieldInitializerContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 27/04/18.
 */
@Singleton
public class FieldSemantics extends FeatureSemantics<Field> {

    public FieldSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<Field> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared
    ) {
        if (input == null) return;


        Maybe<RValueExpression> right = input.__(Field::getRight);
        final Maybe<TypeExpression> explicitTypeExpr = input.__(Field::getType);
        final IJadescriptType finalType;
        if (explicitTypeExpr.isPresent()) {
            final TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);

            finalType = tes.toJadescriptType(explicitTypeExpr);

        } else if (right.isPresent()) {
            module.get(ContextManager.class)
                .enterProceduralFeature(FieldInitializerContext::new);

            StaticState beforeInit = StaticState.beginningOfOperation(module);

            finalType = module.get(RValueExpressionSemantics.class)
                .inferType(right, beforeInit);

            module.get(ContextManager.class).exit();
        } else {
            // Something's wrong.
            finalType = module.get(TypeHelper.class).ANY;
        }
        Maybe<String> name = input.__(Field::getName);

        if (input.isNothing() || name.isNothing()) {
            return;
        }

        final Field inputSafe = input.toNullable();
        final String nameSafe = name.toNullable();

        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        final JvmTypeReference typeRef = finalType.asJvmTypeReference();

        final boolean isResolved =
            typeRef.getType() instanceof JvmDeclaredType;

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);
        members.add(module.get(JvmTypesBuilder.class).toField(
            inputSafe,
            nameSafe,
            typeRef,
            itField -> {
                itField.setVisibility(JvmVisibility.PROTECTED);
                if (right.isPresent()) {
                    compilationHelper.createAndSetInitializer(itField, scb ->
                        fillFieldInitializer(
                            savedContext,
                            right,
                            finalType,
                            explicitTypeExpr.isPresent(),
                            scb
                        )
                    );
                } else {
                    compilationHelper.createAndSetInitializer(
                        itField,
                        scb -> scb.add(CompilationHelper
                            .compileDefaultValueForType(finalType)
                        )
                    );
                }
            }
        ));


        if (!isResolved) {
            String eObjectToStringBefore = "" + typeRef;
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                inputSafe,
                "__debug_UnresolvedFieldType__" + nameSafe,
                typeRef,
                itMethod -> {
                    itMethod.setVisibility(JvmVisibility.PRIVATE);
                    JvmTypeReference typeReference2 =
                        TypeHelper.attemptResolveTypeRef(
                            module, typeRef
                        );
                    String eObjectToStringReattempt1 = "" + typeReference2;
                    compilationHelper.createAndSetBody(itMethod, scb -> {
                        String eObjectToStringAfter = "" + typeRef;
                        JvmTypeReference typeReference3 =
                            TypeHelper.attemptResolveTypeRef(
                                module, typeRef
                            );
                        String eObjectToStringReattempt2 = "" + typeReference3;
                        itMethod.setReturnType(typeReference3);

                        scb.open("/*BEFORE");
                        scb.add("eObject=");
                        scb.line("" + eObjectToStringBefore);
                        scb.close("*/");
                        scb.open("/*AFTER");
                        scb.add("eObject=");
                        scb.line("" + eObjectToStringAfter);
                        scb.close("*/");
                        scb.open("/*REATTEMPT1");
                        scb.add("eObject=");
                        scb.line("" + eObjectToStringReattempt1);
                        scb.close("*/");
                        scb.open("/*REATTEMPT2");
                        scb.add("eObject=");
                        scb.line("" + eObjectToStringReattempt2);
                        scb.close("*/");
                        scb.line("return null;");
                    });
                }
            ));
            members.add(module.get(JvmTypesBuilder.class).toField(
                inputSafe,
                "__eIsProxy_" + nameSafe + "_" + typeRef.eIsProxy(),
                module.get(TypeHelper.class).BOOLEAN.asJvmTypeReference(),
                itField -> compilationHelper.createAndSetInitializer(
                    itField,
                    scb -> {
                        scb.line("" + typeRef.eIsProxy());
                    }
                )
            ));
        }

        members.add(module.get(JvmTypesBuilder.class).toSetter(
            inputSafe,
            nameSafe,
            typeRef
        ));
        members.add(module.get(JvmTypesBuilder.class).toGetter(
            inputSafe,
            nameSafe,
            typeRef
        ));

    }


    private void fillFieldInitializer(
        SavedContext savedContext,
        Maybe<RValueExpression> right,
        IJadescriptType type,
        boolean isExplicitType,
        SourceCodeBuilder scb
    ) {
        module.get(ContextManager.class).restore(savedContext);
        module.get(ContextManager.class).enterProceduralFeature(
            FieldInitializerContext::new);

        StaticState beforeInit = StaticState.beginningOfOperation(module);


        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);


        final String initExpr;
        if (isExplicitType) {
            initExpr = compilationHelper.compileRValueAsLambdaSupplier(
                right,
                beforeInit,
                type,
                type
            );
        } else {
            initExpr = compilationHelper.compileRValueAsLambdaSupplier(
                right,
                beforeInit,
                type,
                null
            );
        }

        scb.add(initExpr);


        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateFeature(
        Maybe<Field> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<String> name = input.__(Field::getName);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        validationHelper.assertNotReservedName(
            name,
            input,
            JadescriptPackage.eINSTANCE.getField_Name(),
            acceptor
        );
        final Maybe<TypeExpression> explicitTypeExpr = input.__(Field::getType);


        Maybe<RValueExpression> right = input.__(Field::getRight);
        Maybe<IJadescriptType> explicitType = nothing();

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        if (explicitTypeExpr.isPresent()) {
            InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
            tes.validate(
                explicitTypeExpr,
                typeValidation
            );


            if (!typeValidation.thereAreErrors()) {
                explicitType =
                    Maybe.some(tes.toJadescriptType(explicitTypeExpr));
                explicitType.safeDo(t -> t.validateType(
                    explicitTypeExpr,
                    acceptor
                ));
            }
        }

        IJadescriptType finalType = explicitType
            .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                "Cannot compute type of property with no explicit type " +
                    "and no initializer expression"
            ));

        if (right.isPresent()) {
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);

            module.get(ContextManager.class)
                .enterProceduralFeature(FieldInitializerContext::new);

            StaticState beforeInit = StaticState.beginningOfOperation(module);

            boolean initExprCheck = rves.validate(
                right,
                beforeInit,
                acceptor
            );

            if (initExprCheck == VALID) {
                IJadescriptType inferredType =
                    rves.inferType(right, beforeInit);

                boolean typeConformanceCheck = VALID;
                if (explicitType.isPresent()) {
                    typeConformanceCheck = validationHelper.assertExpectedType(
                        explicitType.toNullable(),
                        inferredType,
                        "TypeMismatch",
                        right,
                        acceptor
                    );
                }

                if (typeConformanceCheck == VALID) {

                    if (name.isPresent() && input.isPresent()) {
                        final Field inputSafe = input.toNullable();

                        inferredType.validateType(right, acceptor);
                        if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
                            acceptor.acceptInfo(
                                "Field declaration; type: " + inferredType,
                                inputSafe,
                                JadescriptPackage.eINSTANCE.getField_Name(),
                                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                                ISSUE_CODE_PREFIX + "Info"
                            );
                        }

                    }


                    if (explicitType.isNothing()) {
                        finalType = inferredType;
                    }
                }
            }

            module.get(ContextManager.class).exit();
        }

        validationHelper.validateFieldCompatibility(
            name,
            finalType,
            input,
            getLocationOfThis(),
            acceptor
        );

    }


}
