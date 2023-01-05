package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.AssignmentDeconstruction;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.MatchesExpression;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.WhenMatchesStatement;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
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
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

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


    private static JvmGenericType convertLocalClassToInnerClass(
        SemanticsModule module,
        EObject eobj,
        LocalClassStatementWriter localClass
    ) {
        JvmTypesBuilder jvmtb = module.get(JvmTypesBuilder.class);
        TypeHelper typeH = module.get(TypeHelper.class);
        CompilationHelper compH =
            module.get(CompilationHelper.class);
        return jvmtb.toClass(eobj, localClass.getName(), itClass -> {
            for (ClassMemberWriter member : localClass.getMembers()) {
                if (member instanceof ConstructorWriter) {
                    ConstructorWriter ctor = (ConstructorWriter) member;
                    itClass.getMembers().add(jvmtb.toConstructor(
                        eobj,
                        itCtor -> {
                            itCtor.setVisibility(
                                convertToJvm(member.getVisibility())
                            );
                            for (
                                ParameterWriter parameter :
                                ctor.getParameters()
                            ) {
                                itCtor.getParameters().add(jvmtb.toParameter(
                                    eobj,
                                    parameter.getName(),
                                    typeH.typeRef(parameter.getType())
                                ));
                            }
                            compH.createAndSetBody(
                                itCtor,
                                ctor.getBody()::writeSonnet
                            );
                        }
                    ));
                } else if (member instanceof FieldWriter) {
                    FieldWriter field = (FieldWriter) member;
                    itClass.getMembers().add(jvmtb.toField(
                        eobj,
                        field.getName(),
                        typeH.typeRef(field.getType()),
                        itField -> {
                            itField.setVisibility(
                                convertToJvm(member.getVisibility())
                            );
                            if (field.getInitExpression() != null) {
                                compH.createAndSetInitializer(
                                    itField,
                                    field.getInitExpression()::writeSonnet
                                );
                            }
                        }
                    ));
                } else if (member instanceof MethodWriter) {
                    MethodWriter method = (MethodWriter) member;
                    itClass.getMembers().add(
                        jvmtb.toMethod(eobj, method.getName(),
                            typeH.typeRef(method.getReturnType()),
                            itMethod -> {
                                itMethod.setVisibility(convertToJvm(
                                    member.getVisibility()));
                                for (ParameterWriter parameter :
                                    method.getParameters()) {
                                    JvmFormalParameter param =
                                        jvmtb.toParameter(
                                            eobj,
                                            parameter.getName(),
                                            typeH.typeRef(parameter.getType())
                                        );
                                    itMethod.getParameters().add(param);
                                }
                                compH.createAndSetBody(
                                    itMethod,
                                    method.getBody()::writeSonnet
                                );
                            }
                        )
                    );
                }//else ignore
            }
        });
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
        TypeHelper typeHelper = module.get(TypeHelper.class);
        CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);
        return auxiliaryStatements.stream()
            .filter(VariableDeclarationWriter.class::isInstance)
            .map(VariableDeclarationWriter.class::cast)
            .map(variableDeclarationWriter -> {
                return jvmTypesBuilder.toField(
                    eobj,
                    variableDeclarationWriter.getName(),
                    typeHelper.typeRef(variableDeclarationWriter.getType()),
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


    public PatternMatcher compileAssignmentDeconstructionPatternMatching(
        IJadescriptType rightType,
        Maybe<LValueExpression> pattern,
        StaticState afterInputExpr,
        CompilationOutputAcceptor acceptor
    ) {
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final AssignmentDeconstruction<LValueExpression> patternMatchInput =
            new AssignmentDeconstruction<>(
                module,
                rightType,
                pattern,
                "__",
                variableName
            );
        final PatternMatcher output =
            module.get(LValueExpressionSemantics.class).compilePatternMatch(
                patternMatchInput,
                afterInputExpr,
                acceptor
            );

        final LocalClassStatementWriter localClass =
            w.localClass(localClassName);

        output.getWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(w.variable(
            localClassName,
            variableName,
            w.expr("new "
                + localClassName + "()")
        ));
        return output;
    }

    public PatternMatcher compileWhenMatchesStatementPatternMatching(
        Maybe<RValueExpression> inputExpr,
        Maybe<LValueExpression> pattern,
        StaticState afterInputExpr,
        CompilationOutputAcceptor acceptor
    ) {
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final WhenMatchesStatement<LValueExpression> patternMatchInput =
            new WhenMatchesStatement<>(
                module,
                inputExpr,
                pattern,
                "__",
                variableName
            );
        final PatternMatcher output =
            module.get(LValueExpressionSemantics.class).compilePatternMatch(
                patternMatchInput,
                afterInputExpr,
                acceptor
            );

        final LocalClassStatementWriter localClass =
            w.localClass(localClassName);

        output.getWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(w.variable(
            localClassName,
            variableName,
            w.expr("new "
                + localClassName + "()")
        ));
        return output;
    }


    public PatternMatcher compileMatchesExpressionPatternMatching(
        IJadescriptType inputExprType,
        Maybe<LValueExpression> pattern,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final MatchesExpression<LValueExpression> patternMatchInput =
            new PatternMatchInput.MatchesExpression<>(
                module,
                inputExprType,
                pattern,
                "__",
                variableName
            );
        final PatternMatcher output =
            module.get(LValueExpressionSemantics.class).compilePatternMatch(
                patternMatchInput, state, acceptor
            );


        final LocalClassStatementWriter localClass =
            w.localClass(localClassName);

        output.getWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(w.variable(localClassName, variableName, w.expr("new "
            + localClassName + "()")));
        return output;
    }


    public PatternMatcher compileHeaderPatternMatching(
        IJadescriptType contentUpperBound,
        Maybe<LValueExpression> pattern,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {

        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.HandlerHeader<LValueExpression>
            patternMatchInput =
            new PatternMatchInput.HandlerHeader<>(
                module,
                contentUpperBound,
                pattern,
                "__",
                variableName
            );
        final PatternMatcher output =
            module.get(LValueExpressionSemantics.class).compilePatternMatch(
                patternMatchInput,
                state,
                acceptor
            );


        final LocalClassStatementWriter localClass =
            w.localClass(localClassName);

        output.getWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(w.variable(localClassName, variableName, w.expr("new "
            + localClassName + "()")));
        return output;
    }


    public StaticState advanceMatchesExpressionPatternMatching(
        IJadescriptType inputExprType,
        Maybe<LValueExpression> pattern,
        StaticState state
    ) {
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final MatchesExpression<LValueExpression> patternMatchInput =
            new PatternMatchInput.MatchesExpression<>(
                module,
                inputExprType,
                pattern,
                "__",
                variableName
            );
        return module.get(LValueExpressionSemantics.class).advancePattern(
            patternMatchInput,
            state
        );
    }


    public StaticState advanceHeaderPatternMatching(
        IJadescriptType contentUpperBound,
        Maybe<LValueExpression> pattern,
        StaticState state
    ) {

        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.HandlerHeader<LValueExpression>
            patternMatchInput =
            new PatternMatchInput.HandlerHeader<>(
                module,
                contentUpperBound,
                pattern,
                "__",
                variableName
            );
        return module.get(LValueExpressionSemantics.class).advancePattern(
            patternMatchInput,
            state
        );
    }

    public StaticState advanceAssignmentDeconstructionPatternMatching(
        IJadescriptType rightType,
        Maybe<LValueExpression> left,
        StaticState afterRight
    ) {

        String localClassName =
            "__PatternMatcher" + Util.extractEObject(left).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.AssignmentDeconstruction<LValueExpression>
            patternMatchInput =
            new PatternMatchInput.AssignmentDeconstruction<>(
                module,
                rightType,
                left,
                "__",
                variableName
            );
        return module.get(LValueExpressionSemantics.class).advancePattern(
            patternMatchInput,
            afterRight
        );
    }


    public boolean validateWhenMatchesStatementPatternMatching(
        Maybe<RValueExpression> inputExpr,
        Maybe<LValueExpression> pattern,
        StaticState afterInputExpr,
        ValidationMessageAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" +
            Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final WhenMatchesStatement<LValueExpression> patternMatchInput =
            new WhenMatchesStatement<>(
                module,
                inputExpr,
                pattern,
                "__",
                variableName
            );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
            patternMatchInput,
            afterInputExpr,
            acceptor
        );
    }


    public boolean validateMatchesExpressionPatternMatching(
        IJadescriptType inputExprType,
        Maybe<LValueExpression> pattern,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final MatchesExpression<LValueExpression> patternMatchInput =
            new MatchesExpression<>(
                module,
                inputExprType,
                pattern,
                "__",
                variableName
            );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
            patternMatchInput,
            state,
            acceptor
        );
    }
    public boolean validateAssignmentDeconstructionPatternMatching(
        IJadescriptType rightType,
        Maybe<LValueExpression> leftPattern,
        StaticState afterRight,
        ValidationMessageAcceptor acceptor
    ) {

        String localClassName =
            "__PatternMatcher" + Util.extractEObject(leftPattern).hashCode();
        final String variableName = localClassName + "_obj";
        final AssignmentDeconstruction<LValueExpression> patternMatchInput =
            new AssignmentDeconstruction<>(
                module,
                rightType,
                leftPattern,
                "__",
                variableName
            );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
            patternMatchInput,
            afterRight,
            acceptor
        );
    }


    public boolean
    validateHeaderPatternMatching(
        IJadescriptType contentUpperBound,
        Maybe<LValueExpression> pattern,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        String localClassName = "__PatternMatcher" +
            Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        PatternMatchInput.HandlerHeader<LValueExpression> patternMatchInput =
            new PatternMatchInput.HandlerHeader<>(
                module,
                contentUpperBound,
                pattern,
                "__",
                variableName
            );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
            patternMatchInput,
            state,
            acceptor
        );
    }


    public IJadescriptType inferMatchesExpressionPatternType(
        IJadescriptType inputExprType,
        Maybe<LValueExpression> pattern,
        StaticState state
    ) {
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final MatchesExpression<LValueExpression> patternMatchInput =
            new MatchesExpression<>(
                module,
                inputExprType,
                pattern,
                "__",
                variableName
            );
        return module.get(LValueExpressionSemantics.class)
            .inferPatternType(patternMatchInput, state)
            .solve(inputExprType);
    }


    public IJadescriptType inferHandlerHeaderPatternType(
        IJadescriptType contentUpperBound, Maybe<LValueExpression> pattern,
        StaticState state
    ) {
        String localClassName = "__PatternMatcher" +
            Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        PatternMatchInput.HandlerHeader<LValueExpression> patternMatchInput =
            new PatternMatchInput.HandlerHeader<>(
                module,
                contentUpperBound,
                pattern,
                "__",
                variableName
            );

        return module.get(LValueExpressionSemantics.class)
            .inferPatternType(patternMatchInput, state)
            .solve(contentUpperBound);
    }

}
