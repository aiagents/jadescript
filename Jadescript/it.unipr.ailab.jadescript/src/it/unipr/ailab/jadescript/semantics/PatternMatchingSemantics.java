package it.unipr.ailab.jadescript.semantics;
//
//
//import it.unipr.ailab.jadescript.jadescript.*;
//import it.unipr.ailab.jadescript.semantics.context.ContextManager;
//import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
//import it.unipr.ailab.jadescript.semantics.expression.*;
//import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
//import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
//import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
//import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
//import it.unipr.ailab.jadescript.semantics.proxyeobjects.PatternMatchRequest;
//import it.unipr.ailab.jadescript.semantics.proxyeobjects.ProxyEObject;
//import it.unipr.ailab.jadescript.semantics.proxyeobjects.VirtualIdentifier;
//import it.unipr.ailab.maybe.Maybe;
//import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
//import it.unipr.ailab.sonneteer.classmember.FieldWriter;
//import it.unipr.ailab.sonneteer.classmember.MethodWriter;
//import it.unipr.ailab.sonneteer.classmember.ParameterWriter;
//import it.unipr.ailab.sonneteer.qualifiers.Visibility;
//import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
//import it.unipr.ailab.sonneteer.statement.StatementWriter;
//import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
//import it.unipr.ailab.sonneteer.type.ClassDeclarationWriter;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.xtext.common.types.*;
//import org.eclipse.xtext.validation.ValidationMessageAcceptor;
//import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//import static it.unipr.ailab.maybe.Functional.filterAndCast;
//import static it.unipr.ailab.maybe.Maybe.*;
//
//
public class PatternMatchingSemantics
//        extends Semantics<PatternMatchRequest>
        {
//
//    public PatternMatchingSemantics(SemanticsModule semanticsModule) {
//        super(semanticsModule);
//    }
//
//
//    /**
//     * Converts a list of {@link LocalClassStatementWriter} into a list of {@link JvmDeclaredType} (using a
//     * {@link JvmTypesBuilder}) which can in turn be used to generate inner classes to pattern match a "content" of an
//     * event against the pattern in the header of the event handler
//     *
//     * @param auxiliaryStatements the input auxiliaryStatements
//     * @param sourceEObject       the eObject indicated as source of the pattern
//     * @param module              the semantics module
//     * @return a list of {@link JvmDeclaredType} where each instance is a pattern matcher class.
//     */
//    public static List<JvmDeclaredType> getPatternMatcherClasses(
//            List<StatementWriter> auxiliaryStatements,
//            Maybe<? extends EObject> sourceEObject,
//            SemanticsModule module
//    ) {
//        if (sourceEObject.isNothing()) {
//            return Collections.emptyList();
//        }
//
//        JvmTypesBuilder jvmTypesBuilder = module.get(JvmTypesBuilder.class);
//        TypeHelper typeHelper = module.get(TypeHelper.class);
//        CompilationHelper compilationHelper = module.get(CompilationHelper.class);
//
//        EObject eobj = sourceEObject.toNullable();
//        return auxiliaryStatements.stream()
//                .filter(LocalClassStatementWriter.class::isInstance)
//                .map(LocalClassStatementWriter.class::cast)
//                .map(localClass -> jvmTypesBuilder.toClass(eobj, localClass.getName(), itClass -> {
//                    for (ClassMemberWriter member : localClass.getMembers()) {
//                        if (member instanceof ClassDeclarationWriter.ConstructorWriter) {
//                            ClassDeclarationWriter.ConstructorWriter ctor = (ClassDeclarationWriter.ConstructorWriter) member;
//                            itClass.getMembers().add(jvmTypesBuilder.toConstructor(eobj, itCtor -> {
//                                itCtor.setVisibility(convertToJvm(member.getVisibility()));
//                                for (ParameterWriter parameter : ctor.getParameters()) {
//                                    JvmFormalParameter param = jvmTypesBuilder.toParameter(
//                                            eobj,
//                                            parameter.getName(),
//                                            typeHelper.typeRef(parameter.getType())
//                                    );
//                                    itCtor.getParameters().add(param);
//                                }
//                                compilationHelper.createAndSetBody(
//                                        itCtor,
//                                        ctor.getBody()::writeSonnet
//                                );
//                            }));
//                        } else if (member instanceof FieldWriter) {
//                            FieldWriter field = (FieldWriter) member;
//                            itClass.getMembers().add(jvmTypesBuilder.toField(eobj, field.getName(), typeHelper.typeRef(field.getType()), itField -> {
//                                itField.setVisibility(convertToJvm(member.getVisibility()));
//                                if (field.getInitExpression() != null) {
//                                    compilationHelper.createAndSetInitializer(
//                                            itField,
//                                            field.getInitExpression()::writeSonnet
//                                    );
//                                }
//                            }));
//                        } else if (member instanceof MethodWriter) {
//                            MethodWriter method = (MethodWriter) member;
//                            itClass.getMembers().add(
//                                    jvmTypesBuilder.toMethod(eobj, method.getName(), typeHelper.typeRef(method.getReturnType()),
//                                            itMethod -> {
//                                                itMethod.setVisibility(convertToJvm(member.getVisibility()));
//                                                for (ParameterWriter parameter : method.getParameters()) {
//                                                    JvmFormalParameter param = jvmTypesBuilder.toParameter(
//                                                            eobj,
//                                                            parameter.getName(),
//                                                            typeHelper.typeRef(parameter.getType())
//                                                    );
//                                                    itMethod.getParameters().add(param);
//                                                }
//                                                compilationHelper.createAndSetBody(
//                                                        itMethod,
//                                                        method.getBody()::writeSonnet
//                                                );
//                                            }
//                                    ));
//                        }//else ignore
//                    }
//                })).collect(Collectors.toList());
//    }
//
//    public static List<JvmField> getPatternMatcherFieldDeclarations(
//            List<StatementWriter> auxiliaryStatements,
//            Maybe<? extends EObject> sourceObject,
//            SemanticsModule module
//    ) {
//        if (sourceObject.isNothing()) {
//            return Collections.emptyList();
//        }
//
//        EObject eobj = sourceObject.toNullable();
//
//        JvmTypesBuilder jvmTypesBuilder = module.get(JvmTypesBuilder.class);
//        TypeHelper typeHelper = module.get(TypeHelper.class);
//        CompilationHelper compilationHelper = module.get(CompilationHelper.class);
//        return auxiliaryStatements.stream()
//                .filter(VariableDeclarationWriter.class::isInstance)
//                .map(VariableDeclarationWriter.class::cast)
//                .map(variableDeclarationWriter -> {
//                    return jvmTypesBuilder.toField(
//                            eobj,
//                            variableDeclarationWriter.getName(),
//                            typeHelper.typeRef(variableDeclarationWriter.getType()),
//                            itField -> {
//                                compilationHelper.createAndSetInitializer(
//                                        itField,
//                                        w.callExpr("new " + variableDeclarationWriter.getType())::writeSonnet
//                                );
//                            }
//                    );
//                }).collect(Collectors.toList());
//    }
//
//    private static JvmVisibility convertToJvm(Visibility visibility) {
//        switch (visibility) {
//            case PUBLIC:
//                return JvmVisibility.PUBLIC;
//            case PRIVATE:
//                return JvmVisibility.PRIVATE;
//            case PROTECTED:
//                return JvmVisibility.PROTECTED;
//            case PACKAGE:
//            default:
//                return JvmVisibility.DEFAULT;
//        }
//    }
//
//
//    @Override
//    public void validate(Maybe<PatternMatchRequest> input, ValidationMessageAcceptor acceptor) {
//        input.__(ProxyEObject::getProxyEObject).safeDo(inputSafe -> {
//            String localClassName = "__PatternMatcher" + inputSafe.hashCode();
//            final Maybe<UnaryPrefix> unary = input.__(PatternMatchRequest::getUnary)
//                    .extract(Maybe::flatten);
//            module.get(UnaryPrefixExpressionSemantics.class).validate(unary, acceptor);
//            IJadescriptType leftType = module.get(UnaryPrefixExpressionSemantics.class).inferType(unary);
////            validatePatternMatchDetails(
////                    input,
////                    input.__(PatternMatchRequest::getPattern).extract(Maybe::flatten),
////                    "__",
////                    localClassName + "_obj",
////                    leftType,
////                    acceptor
////            );
//        });
//    }
//
//
//    public Maybe<String> compileMatchesExpression(
//            Maybe<? extends EObject> input,
//            Maybe<UnaryPrefix> unary
//    ) {
//        if (input.isNothing()) {
//            return nothing();
//        }
//
//        EObject inputSafe = input.toNullable();
//
//        String localClassName = "__PatternMatcher" + inputSafe.hashCode();
//        String variableName = localClassName + "_obj";
//
//        return of(variableName + ".__matches(" + module.get(UnaryPrefixExpressionSemantics.class).compile(unary).orElse("") + ")");
//    }
//
//    public Maybe<String> compileMatchesExpression(
//            Maybe<PatternMatchRequest> input
//    ) {
//        return compileMatchesExpression(
//                input.__(ProxyEObject::getProxyEObject),
//                Maybe.flatten(input.__(PatternMatchRequest::getUnary))
//        );
//    }
//
//    public List<? extends StatementWriter> generateAuxiliaryStatements(
//            Maybe<PatternMatchRequest> input
//    ) {
//
//        List<StatementWriter> result = new ArrayList<>();
//
//
//        String localClassName = "__PatternMatcher" + input.toNullable().getProxyEObject().hashCode();
//        LocalClassStatementWriter localClass = new LocalClassStatementWriter(localClassName);
//
//        final Maybe<UnaryPrefix> unary = input.__(PatternMatchRequest::getUnary)
//                .extract(Maybe::flatten);
//        IJadescriptType expectedType = module.get(UnaryPrefixExpressionSemantics.class).inferType(unary);
//        final Maybe<Pattern> pattern = input.__(PatternMatchRequest::getPattern).extract(Maybe::flatten);
//        if (pattern.isPresent()) {
//            Maybe<IJadescriptType> inf = inferPatternType(pattern.toNullable());
//            if (inf.isPresent() && !inf.toNullable().isErroneous()) {
//                expectedType = module.get(TypeHelper.class).getGLB(expectedType, inf.toNullable());
//            }
//        }
//
//        List<ClassMemberWriter> classMemberWriters = null;/* compilePatternMatchDetails(
//                input,
//                pattern,
//                "__",
//                localClassName + "_obj",
//                expectedType
//        );*/
//
//        for (ClassMemberWriter m : classMemberWriters) {
//            localClass.addMember(m);
//        }
//
//        String mainMethodInvokedFunction = classMemberWriters.stream()
//                .flatMap(filterAndCast(MethodWriter.class))
//                .findFirst()
//                .map(MethodWriter::getName)
//                .orElse("__");
//
//        String mainMethodArgType = classMemberWriters.stream()
//                .flatMap(filterAndCast(MethodWriter.class))
//                .findFirst().stream()
//                .flatMap(mw -> mw.getParameters().stream())
//                .findFirst()
//                .map(ParameterWriter::getType)
//                .orElse("java.lang.Object");
//
//        MethodWriter mainMethod = w.method(Visibility.PUBLIC, false, false, "boolean", "__matches")
//                .addParameter(w.param("java.lang.Object", "__x"));
//
//
//        mainMethod.getBody()
//                .addStatement(w.ifStmnt(w.expr("!" + module.get(TypeHelper.class).noGenericsTypeName(mainMethodArgType) +
//                        ".class.isInstance(__x)"), w.block()
//                        .addStatement(w.returnStmnt(w.expr("false")))
//                ))
//                .addStatement(w.returnStmnt(w.callExpr("this." + mainMethodInvokedFunction, w.expr("(" + mainMethodArgType + ")__x"))));
//
//        localClass.addMember(mainMethod);
//
//        String variableName = localClassName + "_obj";
//        VariableDeclarationWriter variable = w.variable(localClassName, variableName, w.expr("new " + localClassName + "()"));
//
//
//        result.add(localClass);
//        result.add(variable);
//
//        return result;
//    }
//
//
//    public Maybe<IJadescriptType> inferPatternType(Pattern p) {
//        //TODO
//    }
//
//
//
}
