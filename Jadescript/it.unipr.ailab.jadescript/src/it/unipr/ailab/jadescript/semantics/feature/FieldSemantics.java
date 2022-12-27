package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.DroppingAcceptor;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FieldInitializerContext;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.safeDo;

/**
 * Created on 27/04/18.
 */
@Singleton
public class FieldSemantics extends FeatureSemantics<Field> {

    public FieldSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    /**
     * Method for declaring an element's field.
     * It adds a private field to the generated class.
     * Generated code: {@code private VariableType variableName = initializer;}
     * A getter and a setter are also generated.
     *
     * @param members       The {@link EList} list of class members.
     * @param beingDeclared The java type (a class) being declared
     */
    @Override
    public void generateJvmMembers(
            Maybe<Field> input,
            Maybe<FeatureContainer> container,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        if (input == null) return;
        IJadescriptType type = getFieldType(input);
        Maybe<String> name = input.__(Field::getName);
        safeDo(input, name,
                /*NULLSAFE REGION*/(inputSafe, nameSafe) -> {
                    //this portion of code is done  only if input and name
                    // are != null (and everything in the dotchains that generated them is !=null too)
                    final SavedContext savedContext = module.get(ContextManager.class).save();
                    final JvmTypeReference typeRef = type.asJvmTypeReference();
                    final boolean isResolved = typeRef.getType() instanceof JvmDeclaredType;
                    members.add(module.get(JvmTypesBuilder.class).toField(inputSafe, nameSafe, typeRef, itField -> {
                        itField.setVisibility(JvmVisibility.PROTECTED);
                        Maybe<RValueExpression> right = input.__(Field::getRight);

                        if (right.isPresent()) {
                            module.get(CompilationHelper.class).createAndSetInitializer(itField, scb -> {

                                module.get(ContextManager.class).restore(savedContext);
                                module.get(ContextManager.class).enterProceduralFeature(FieldInitializerContext::new);

                                scb.add(module.get(RValueExpressionSemantics.class).compile(right,
                                        //TODO TODO TODO!!
                                        // Create acceptor and collector of initialization stuff for fields
                                        new DroppingAcceptor()));

                                module.get(ContextManager.class).exit();

                            });
                        } else {
                            module.get(CompilationHelper.class).createAndSetInitializer(itField, scb ->
                                    scb.add(module.get(CompilationHelper.class).compileEmptyConstructorCall(
                                            input.__(Field::getType)
                                    )));

                        }
                    }));
                    if (!isResolved) {
                        String eObjectToStringBefore = "" + typeRef;
                        members.add(module.get(JvmTypesBuilder.class).toMethod(
                                inputSafe,
                                "__debug_UnresolvedFieldType__"+nameSafe,
                                typeRef,
                                itMethod -> {
                                    itMethod.setVisibility(JvmVisibility.PRIVATE);
                                    JvmTypeReference typeReference2 = TypeHelper.attemptResolveTypeRef(
                                            module, typeRef
                                    );
                                    String eObjectToStringReattempt1 = "" + typeReference2;
                                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                        String eObjectToStringAfter = "" + typeRef;
                                        JvmTypeReference typeReference3 = TypeHelper.attemptResolveTypeRef(
                                                module, typeRef
                                                );
                                        String eObjectToStringReattempt2 = "" + typeReference3;
                                        itMethod.setReturnType(typeReference3);

                                        scb.open("/*BEFORE");
                                        scb.add("eObject=").line(""+eObjectToStringBefore);
                                        scb.close("*/");
                                        scb.open("/*AFTER");
                                        scb.add("eObject=").line(""+eObjectToStringAfter);
                                        scb.close("*/");
                                        scb.open("/*REATTEMPT1");
                                        scb.add("eObject=").line(""+eObjectToStringReattempt1);
                                        scb.close("*/");
                                        scb.open("/*REATTEMPT2");
                                        scb.add("eObject=").line(""+eObjectToStringReattempt2);
                                        scb.close("*/");
                                        scb.line("return null;");
                                    });
                                }
                        ));
                        members.add(module.get(JvmTypesBuilder.class).toField(
                                inputSafe,
                                "__eIsProxy_" + nameSafe + "_"+typeRef.eIsProxy(),
                                module.get(TypeHelper.class).BOOLEAN.asJvmTypeReference(),
                                itField -> module.get(CompilationHelper.class).createAndSetInitializer(itField, scb -> {
                                    scb.line("" + typeRef.eIsProxy());
                                })
                        ));
                    }

                    members.add(module.get(JvmTypesBuilder.class).toSetter(inputSafe, nameSafe, typeRef));
                    members.add(module.get(JvmTypesBuilder.class).toGetter(inputSafe, nameSafe, typeRef));

                }/*END NULLSAFE REGION - (inputSafe, nameSafe)*/
        );

    }

    private IJadescriptType getFieldType(Maybe<Field> input) {
        IJadescriptType type;
        Maybe<RValueExpression> right = input.__(Field::getRight);
        final Maybe<TypeExpression> explicitType = input.__(Field::getType);
        if (explicitType.isPresent()) {
            type = module.get(TypeExpressionSemantics.class).toJadescriptType(explicitType);
        } else if (right.isPresent()) {
            module.get(ContextManager.class).enterProceduralFeature(FieldInitializerContext::new);
            type = module.get(RValueExpressionSemantics.class).inferType(right);
            module.get(ContextManager.class).exit();
        } else {
            // Something's wrong.
            type = module.get(TypeHelper.class).ANY;
        }
        return type;
    }

    @Override
    public void validateFeature(Maybe<Field> input, Maybe<FeatureContainer> container, ValidationMessageAcceptor acceptor) {
        Maybe<String> name = input.__(Field::getName);
        module.get(ValidationHelper.class).assertNotReservedName(name, input, JadescriptPackage.eINSTANCE.getField_Name(), acceptor);
        final Maybe<TypeExpression> explicitType = input.__(Field::getType);


        Maybe<RValueExpression> right = input.__(Field::getRight);
        Maybe<IJadescriptType> typeDescriptor = nothing();

        if (explicitType.isPresent()) {
            InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
            module.get(TypeExpressionSemantics.class).validate(explicitType, typeValidation);
            if (!typeValidation.thereAreErrors()) {
                typeDescriptor = Maybe.of(module.get(TypeExpressionSemantics.class).toJadescriptType(explicitType));
                typeDescriptor.safeDo(t -> t.validateType(explicitType, acceptor));
            }
        }

        if (right.isPresent()) {
            module.get(ContextManager.class).enterProceduralFeature(FieldInitializerContext::new);

            InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);

            module.get(RValueExpressionSemantics.class).validate(right, subValidation);

            if (!subValidation.thereAreErrors()) {
                IJadescriptType inferredType = module.get(RValueExpressionSemantics.class).inferType(right);

                if (typeDescriptor.isPresent()) {
                    module.get(ValidationHelper.class).assertExpectedType(
                            typeDescriptor.toNullable(),
                            inferredType,
                            "TypeMismatch",
                            right,
                            subValidation
                    );
                }

                if (!subValidation.thereAreErrors()) {

                    safeDo(name, input,
                            /*NULLSAFE REGION*/(nameSafe, inputSafe) -> {
                                //this portion of code is done  only if name and input
                                // are != null (and everything in the dotchains that generated them is !=null too)

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
                            }/*END NULLSAFE REGION - (nameSafe, inputSafe)*/
                    );

                    if (typeDescriptor.isNothing()) {
                        typeDescriptor = Maybe.of(inferredType);
                    }
                }
            }

            module.get(ContextManager.class).exit();
        }

        module.get(ValidationHelper.class).validateFieldCompatibility(
                name,
                typeDescriptor.orElseGet(() -> module.get(TypeHelper.class).ANY),
                input,
                getLocationOfThis(),
                acceptor
        );

    }


}
