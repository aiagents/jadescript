package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
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
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit.ImplicitConversionsHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
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
public class FieldSemantics extends DeclarationMemberSemantics<Field> {

    public FieldSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<Field> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        if (input == null) {
            return;
        }
        final ContextManager contextManager = module.get(ContextManager.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        Maybe<RValueExpression> right = input.__(Field::getRight);
        final Maybe<TypeExpression> explicitTypeExpr = input.__(Field::getType);
        final IJadescriptType finalType;

        if (explicitTypeExpr.isPresent()) {
            final TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);

            finalType = tes.toJadescriptType(explicitTypeExpr);

        } else if (right.isPresent()) {
            contextManager
                .enterProceduralFeature(FieldInitializerContext::new);

            StaticState beforeInit = StaticState.beginningOfOperation(module);

            finalType = module.get(RValueExpressionSemantics.class)
                .inferType(right, beforeInit);

            contextManager.exit();
        } else {
            finalType = builtins.any(
                "Cannot infer type of field without initializer expression " +
                    "or type specifier."
            );
        }
        Maybe<String> name = input.__(Field::getName);

        if (input.isNothing() || name.isNothing()) {
            return;
        }

        final Field inputSafe = input.toNullable();
        final String nameSafe = name.toNullable();

        final SavedContext savedContext = contextManager.save();

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


                compilationHelper.createAndSetInitializer(itField, scb -> {
                    //Puts null as initializer expression:
                    scb.add("null");

                    final String dereferencedField =
                        SemanticsUtils.getOuterClassThisReference(container) +
                            "." + nameSafe;

                    // But then uses the __initializeProperties() method to
                    // actually initialize the properties.
                    if (right.isPresent()) {
                        generateFieldInitializer(
                            dereferencedField,
                            savedContext,
                            right,
                            finalType,
                            explicitTypeExpr.isPresent(),
                            fieldInitializationAcceptor
                        );
                    } else {
                        fieldInitializationAcceptor.accept(w.assign(
                            dereferencedField,
                            w.expr(CompilationHelper
                                .compileDefaultValueForType(finalType))
                        ));
                    }
                });


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
                    final JvmTypeHelper jvmTypeHelper =
                        module.get(JvmTypeHelper.class);
                    JvmTypeReference typeReference2 =
                        jvmTypeHelper.attemptResolveTypeRef(typeRef);
                    String eObjectToStringReattempt1 = "" + typeReference2;
                    compilationHelper.createAndSetBody(itMethod, scb -> {
                        String eObjectToStringAfter = "" + typeRef;
                        JvmTypeReference typeReference3 =
                            jvmTypeHelper.attemptResolveTypeRef(typeRef);
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
                builtins.boolean_().asJvmTypeReference(),
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


    private void generateFieldInitializer(
        String dereferencedField,
        SavedContext savedContext,
        Maybe<RValueExpression> right,
        IJadescriptType type,
        boolean isExplicitType,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        module.get(ContextManager.class).restore(savedContext);
        module.get(ContextManager.class).enterProceduralFeature(
            FieldInitializerContext::new);

        StaticState beforeInit = StaticState.beginningOfOperation(module);

        final ImplicitConversionsHelper implicits =
            module.get(ImplicitConversionsHelper.class);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final String initExpr;
        if (isExplicitType) {
            initExpr = implicits.compileWithEventualImplicitConversions(
                rves.compile(right, beforeInit, fieldInitializationAcceptor),
                rves.inferType(right, beforeInit),
                type
            );
        } else {
            initExpr = rves.compile(
                right,
                beforeInit,
                fieldInitializationAcceptor
            );
        }

        fieldInitializationAcceptor.accept(
            w.assign(
                dereferencedField,
                w.expr(initExpr)
            )
        );


        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateOnEdit(
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
            .orElseGet(() -> module.get(BuiltinTypeProvider.class).any(
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
                finalType = doTypeConformanceCheck(
                    input,
                    acceptor,
                    name,
                    validationHelper,
                    right,
                    explicitType,
                    finalType,
                    rves,
                    beforeInit
                );
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


    private IJadescriptType doTypeConformanceCheck(
        Maybe<Field> input,
        ValidationMessageAcceptor acceptor,
        Maybe<String> name,
        ValidationHelper validationHelper,
        Maybe<RValueExpression> right,
        Maybe<IJadescriptType> explicitType,
        IJadescriptType finalType,
        RValueExpressionSemantics rves,
        StaticState beforeInit
    ) {
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
        return finalType;
    }


    @Override
    public void validateOnSave(
        Maybe<Field> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {


    }


}
