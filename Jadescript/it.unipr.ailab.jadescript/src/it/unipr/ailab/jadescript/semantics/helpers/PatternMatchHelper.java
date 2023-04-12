package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.AssignmentDeconstruction;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.HandlerHeader;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.MatchesExpression;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.WhenMatchesStatement;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.classmember.ParameterWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
import it.unipr.ailab.sonneteer.type.ClassDeclarationWriter.ConstructorWriter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PatternMatchHelper implements SemanticsConsts {

    public static final WriterFactory w = WriterFactory.getInstance();

    private final SemanticsModule module;


    public PatternMatchHelper(SemanticsModule module) {
        this.module = module;
    }


    /**
     * Converts a list of {@link LocalClassStatementWriter} into a list of
     * {@link JvmDeclaredType} (using a
     * {@link JvmTypesBuilder}) which can in turn be used to generate inner
     * classes to pattern match a "content" of an
     * event against the pattern in the header of the event handler
     *
     * @param auxiliaryStatements the input auxiliaryStatements
     * @param sourceEObject       the eObject indicated as source of the pattern
     * @param module              the semantics module
     * @return a list of {@link JvmDeclaredType} where each instance is a
     * pattern matcher class.
     */
    public static List<JvmDeclaredType> getPatternMatcherClasses(
        List<BlockWriterElement> auxiliaryStatements,
        Maybe<? extends EObject> sourceEObject,
        SemanticsModule module
    ) {
        if (sourceEObject.isNothing()) {
            return Collections.emptyList();
        }

        EObject eobj = sourceEObject.toNullable();
        return auxiliaryStatements.stream()
            .filter(LocalClassStatementWriter.class::isInstance)
            .map(LocalClassStatementWriter.class::cast)
            .map(localClass -> convertLocalClassToInnerClass(
                module,
                eobj,
                localClass
            )).collect(Collectors.toList());
    }


    public static JvmGenericType convertLocalClassToInnerClass(
        SemanticsModule module,
        EObject eobj,
        LocalClassStatementWriter localClass
    ) {
        JvmTypesBuilder jvmtb = module.get(JvmTypesBuilder.class);
        JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        CompilationHelper compH = module.get(CompilationHelper.class);
        return jvmtb.toClass(eobj, localClass.getName(), it -> {
            for (ClassMemberWriter member : localClass.getMembers()) {
                convertMemberWriter(eobj, jvmtb, jvm, compH, it, member);
            }
        });
    }


    private static void convertMemberWriter(
        EObject eobj,
        JvmTypesBuilder jvmtb,
        JvmTypeHelper jvm,
        CompilationHelper compH,
        JvmGenericType it,
        ClassMemberWriter member
    ) {
        if (member instanceof ConstructorWriter) {
            ConstructorWriter ctor = (ConstructorWriter) member;
            it.getMembers().add(jvmtb.toConstructor(eobj, itCtor -> {
                itCtor.setVisibility(
                    convertToJvm(member.getVisibility())
                );
                for (ParameterWriter parameter : ctor.getParameters()) {
                    itCtor.getParameters().add(jvmtb.toParameter(
                        eobj,
                        parameter.getName(),
                        jvm.typeRef(parameter.getType())
                    ));
                }
                compH.createAndSetBody(itCtor, ctor.getBody()::writeSonnet);
            }));
        } else if (member instanceof FieldWriter) {
            FieldWriter field = (FieldWriter) member;
            it.getMembers().add(jvmtb.toField(
                eobj,
                field.getName(),
                jvm.typeRef(field.getType()),
                itField -> {
                    itField.setVisibility(
                        convertToJvm(member.getVisibility())
                    );

                    if (field.getInitExpression() == null) {
                        return; // @ lambda
                    }
                    compH.createAndSetInitializer(
                        itField,
                        field.getInitExpression()::writeSonnet
                    );
                }
            ));
        } else if (member instanceof MethodWriter) {
            MethodWriter method = (MethodWriter) member;
            it.getMembers().add(jvmtb.toMethod(
                eobj,
                method.getName(),
                jvm.typeRef(method.getReturnType()),
                itMethod -> {
                    itMethod.setVisibility(convertToJvm(
                        member.getVisibility()));
                    for (ParameterWriter parameter : method.getParameters()) {
                        JvmFormalParameter param = jvmtb.toParameter(
                            eobj,
                            parameter.getName(),
                            jvm.typeRef(parameter.getType())
                        );
                        itMethod.getParameters().add(param);
                    }
                    compH.createAndSetBody(
                        itMethod,
                        method.getBody()::writeSonnet
                    );
                }
            ));
        }//else ignore
    }


    public static List<JvmField> getPatternMatcherFieldDeclarations(
        List<BlockWriterElement> auxiliaryStatements,
        Maybe<? extends EObject> sourceObject,
        SemanticsModule module
    ) {
        if (sourceObject.isNothing()) {
            return Collections.emptyList();
        }

        EObject eobj = sourceObject.toNullable();

        JvmTypesBuilder jvmTypesBuilder = module.get(JvmTypesBuilder.class);
        JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);
        return auxiliaryStatements.stream()
            .filter(VariableDeclarationWriter.class::isInstance)
            .map(VariableDeclarationWriter.class::cast)
            .map(variableDeclarationWriter -> {
                return jvmTypesBuilder.toField(
                    eobj,
                    variableDeclarationWriter.getName(),
                    jvm.typeRef(variableDeclarationWriter.getType()),
                    itField -> {
                        compilationHelper.createAndSetInitializer(
                            itField,
                            w.callExpr(
                                "new " + variableDeclarationWriter.getType()
                            )::writeSonnet
                        );
                    }
                );
            }).collect(Collectors.toList());
    }


    private static JvmVisibility convertToJvm(Visibility visibility) {
        switch (visibility) {
            case PUBLIC:
                return JvmVisibility.PUBLIC;
            case PRIVATE:
                return JvmVisibility.PRIVATE;
            case PROTECTED:
                return JvmVisibility.PROTECTED;
            case PACKAGE:
            default:
                return JvmVisibility.DEFAULT;
        }
    }


    @NotNull
    public String getPatternMatcherClassName(Maybe<LValueExpression> pattern) {
        return "__PatternMatcher" + SemanticsUtils.extractEObject(pattern)
            .hashCode();
    }


    public String getPatternMatcherVariableName(
        Maybe<LValueExpression> pattern
    ) {
        return getPatternMatcherClassName(pattern) + "_obj";
    }


    public FieldWriter getSelfField(Maybe<LValueExpression> pattern) {
        return w.field(
            Visibility.PRIVATE,
            false,
            true,
            getPatternMatcherClassName(pattern),
            getPatternMatcherVariableName(pattern),
            w.expr("this")
        );
    }


    public AssignmentDeconstruction<LValueExpression> assignmentDeconstruction(
        IJadescriptType rightType,
        Maybe<LValueExpression> pattern,
        Maybe<ExpressionDescriptor> descriptorMaybe
    ) {

        return new AssignmentDeconstruction<>(
            module,
            rightType,
            pattern,
            "deconstruct",
            getPatternMatcherVariableName(pattern),
            descriptorMaybe
        );
    }


    public WhenMatchesStatement<LValueExpression> whenMatchesStatement(
        IJadescriptType inputExprType,
        Maybe<LValueExpression> pattern,
        Maybe<ExpressionDescriptor> descriptorMaybe
    ) {
        return new WhenMatchesStatement<>(
            module,
            inputExprType,
            pattern,
            "whenMatch",
            getPatternMatcherVariableName(pattern),
            descriptorMaybe
        );
    }


    public MatchesExpression<LValueExpression> matchesExpression(
        IJadescriptType inputExprType,
        Maybe<LValueExpression> pattern,
        Maybe<ExpressionDescriptor> descriptorMaybe
    ) {
        return new MatchesExpression<>(
            module,
            inputExprType,
            pattern,
            "match",
            getPatternMatcherVariableName(pattern),
            descriptorMaybe
        );
    }


    public HandlerHeader<LValueExpression> handlerHeader(
        IJadescriptType contentUpperBound,
        Maybe<LValueExpression> pattern,
        Maybe<ExpressionDescriptor> descriptorMaybe
    ) {
        return new HandlerHeader<>(
            module,
            contentUpperBound,
            pattern,
            "headerMatch",
            getPatternMatcherVariableName(pattern),
            descriptorMaybe
        );
    }


}
