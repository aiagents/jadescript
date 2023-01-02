package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.UnaryPrefixExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.MatchesExpression;
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
import it.unipr.ailab.sonneteer.type.ClassDeclarationWriter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PatternMatchHelper implements SemanticsConsts {
    public static WriterFactory w = WriterFactory.getInstance();

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

        JvmTypesBuilder jvmTypesBuilder = module.get(JvmTypesBuilder.class);
        TypeHelper typeHelper = module.get(TypeHelper.class);
        CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        EObject eobj = sourceEObject.toNullable();
        return auxiliaryStatements.stream()
            .filter(LocalClassStatementWriter.class::isInstance)
            .map(LocalClassStatementWriter.class::cast)
            .map(localClass -> jvmTypesBuilder.toClass(eobj,
                localClass.getName(), itClass -> {
                    for (ClassMemberWriter member : localClass.getMembers()) {
                        if (member instanceof ClassDeclarationWriter.ConstructorWriter) {
                            ClassDeclarationWriter.ConstructorWriter ctor =
                                (ClassDeclarationWriter.ConstructorWriter) member;
                            itClass.getMembers().add(jvmTypesBuilder.toConstructor(eobj, itCtor -> {
                                itCtor.setVisibility(convertToJvm(member.getVisibility()));
                                for (ParameterWriter parameter :
                                    ctor.getParameters()) {
                                    JvmFormalParameter param =
                                        jvmTypesBuilder.toParameter(
                                            eobj,
                                            parameter.getName(),
                                            typeHelper.typeRef(parameter.getType())
                                        );
                                    itCtor.getParameters().add(param);
                                }
                                compilationHelper.createAndSetBody(
                                    itCtor,
                                    ctor.getBody()::writeSonnet
                                );
                            }));
                        } else if (member instanceof FieldWriter) {
                            FieldWriter field = (FieldWriter) member;
                            itClass.getMembers().add(jvmTypesBuilder.toField(eobj
                                , field.getName(),
                                typeHelper.typeRef(field.getType()),
                                itField -> {
                                    itField.setVisibility(convertToJvm(member.getVisibility()));
                                    if (field.getInitExpression() != null) {
                                        compilationHelper.createAndSetInitializer(
                                            itField,
                                            field.getInitExpression()::writeSonnet
                                        );
                                    }
                                }
                            ));
                        } else if (member instanceof MethodWriter) {
                            MethodWriter method = (MethodWriter) member;
                            itClass.getMembers().add(
                                jvmTypesBuilder.toMethod(eobj, method.getName(),
                                    typeHelper.typeRef(method.getReturnType()),
                                    itMethod -> {
                                        itMethod.setVisibility(convertToJvm(member.getVisibility()));
                                        for (ParameterWriter parameter :
                                            method.getParameters()) {
                                            JvmFormalParameter param =
                                                jvmTypesBuilder.toParameter(
                                                    eobj,
                                                    parameter.getName(),
                                                    typeHelper.typeRef(parameter.getType())
                                                );
                                            itMethod.getParameters().add(param);
                                        }
                                        compilationHelper.createAndSetBody(
                                            itMethod,
                                            method.getBody()::writeSonnet
                                        );
                                    }
                                ));
                        }//else ignore
                    }
                }
            )).collect(Collectors.toList());
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
                            w.callExpr("new " + variableDeclarationWriter.getType())::writeSonnet
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

    public PatternMatcher compileWhenMatchesStatementPatternMatching(
        Maybe<RValueExpression> inputExpr,
        Maybe<LValueExpression> pattern,
        CompilationOutputAcceptor acceptor
    ) {
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.WhenMatchesStatement<LValueExpression> patternMatchInput =
            new PatternMatchInput.WhenMatchesStatement<>(
                module,
                inputExpr,
                pattern,
                "__",
                variableName
            );
        final PatternMatcher output =
            module.get(LValueExpressionSemantics.class).compilePatternMatch(
                patternMatchInput, ,
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
                patternMatchInput, ,
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

    public boolean
    validateWhenMatchesStatementPatternMatching(
        Maybe<RValueExpression> inputExpr,
        Maybe<LValueExpression> pattern,
        ValidationMessageAcceptor acceptor
    ) {
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput.WhenMatchesStatement<LValueExpression> patternMatchInput =
            new PatternMatchInput.WhenMatchesStatement<>(
                module,
                inputExpr,
                pattern,
                "__",
                variableName
            );


        return module.get(LValueExpressionSemantics.class).validatePatternMatch(
            patternMatchInput, ,
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
        Maybe<LValueExpression> pattern,
        Maybe<UnaryPrefix> unary
    ) {
        return module.get(LValueExpressionSemantics.class).inferPatternType(
            pattern,
            PatternMatchInput.MatchesExpression.MODE,
            ).solve(module.get(UnaryPrefixExpressionSemantics.class).inferType(unary, ));
    }

    public IJadescriptType inferHandlerHeaderPatternType(
        Maybe<LValueExpression> pattern,
        IJadescriptType contentUpperBound
    ) {
        return module.get(LValueExpressionSemantics.class).inferPatternType(
            pattern,
            PatternMatchInput.HandlerHeader.MODE,
            ).solve(contentUpperBound);
    }
}
