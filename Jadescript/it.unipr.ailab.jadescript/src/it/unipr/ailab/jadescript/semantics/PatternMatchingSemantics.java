package it.unipr.ailab.jadescript.semantics;


import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.*;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.PatternMatchRequest;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.ProxyEObject;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.VirtualIdentifier;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.classmember.ParameterWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import it.unipr.ailab.sonneteer.statement.ReturnStatementWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.TryCatchWriter;
import it.unipr.ailab.sonneteer.type.ClassDeclarationWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Functional.filterAndCast;
import static it.unipr.ailab.maybe.Maybe.*;


public class PatternMatchingSemantics extends Semantics<PatternMatchRequest> {

    public PatternMatchingSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public static List<JvmDeclaredType> getPatternMatcherClasses(
            List<StatementWriter> auxiliaryStatements,
            Maybe<? extends EObject> sourceEObject,
            JvmTypesBuilder jvmTypesBuilder,
            TypeHelper typeHelper,
            CompilationHelper compilationHelper
    ) {
        if (sourceEObject.isNothing()) {
            return Collections.emptyList();
        }


        EObject eobj = sourceEObject.toNullable();
        return auxiliaryStatements.stream()
                .filter(LocalClassStatementWriter.class::isInstance)
                .map(LocalClassStatementWriter.class::cast)
                .map(localClass -> jvmTypesBuilder.toClass(eobj, localClass.getName(), itClass -> {
                    for (ClassMemberWriter member : localClass.getMembers()) {
                        if (member instanceof ClassDeclarationWriter.ConstructorWriter) {
                            ClassDeclarationWriter.ConstructorWriter ctor = (ClassDeclarationWriter.ConstructorWriter) member;
                            itClass.getMembers().add(jvmTypesBuilder.toConstructor(eobj, itCtor -> {
                                itCtor.setVisibility(convertToJvm(member.getVisibility()));
                                for (ParameterWriter parameter : ctor.getParameters()) {
                                    JvmFormalParameter param = jvmTypesBuilder.toParameter(
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
                            itClass.getMembers().add(jvmTypesBuilder.toField(eobj, field.getName(), typeHelper.typeRef(field.getType()), itField -> {
                                itField.setVisibility(convertToJvm(member.getVisibility()));
                                if (field.getInitExpression() != null) {
                                    compilationHelper.createAndSetInitializer(
                                            itField,
                                            field.getInitExpression()::writeSonnet
                                    );
                                }
                            }));
                        } else if (member instanceof MethodWriter) {
                            MethodWriter method = (MethodWriter) member;
                            itClass.getMembers().add(
                                    jvmTypesBuilder.toMethod(eobj, method.getName(), typeHelper.typeRef(method.getReturnType()),
                                            itMethod -> {
                                                itMethod.setVisibility(convertToJvm(member.getVisibility()));
                                                for (ParameterWriter parameter : method.getParameters()) {
                                                    JvmFormalParameter param = jvmTypesBuilder.toParameter(
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
                })).collect(Collectors.toList());
    }

    public static List<JvmField> getPatternMatcherFieldDeclarations(
            List<StatementWriter> auxiliaryStatements,
            Maybe<? extends EObject> sourceObject,
            JvmTypesBuilder jvmTypesBuilder,
            TypeHelper typeHelper,
            CompilationHelper compilationHelper

    ) {
        if (sourceObject.isNothing()) {
            return Collections.emptyList();
        }
        EObject eobj = sourceObject.toNullable();

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


    @Override
    public void validate(Maybe<PatternMatchRequest> input, ValidationMessageAcceptor acceptor) {
        input.__(ProxyEObject::getProxyEObject).safeDo(inputSafe -> {
            String localClassName = "__PatternMatcher" + inputSafe.hashCode();
            final Maybe<UnaryPrefix> unary = input.__(PatternMatchRequest::getUnary)
                    .extract(Maybe::flatten);
            module.get(UnaryPrefixExpressionSemantics.class).validate(unary, acceptor);
            IJadescriptType leftType = module.get(UnaryPrefixExpressionSemantics.class).inferType(unary);
            validatePatternMatchDetails(
                    input,
                    input.__(PatternMatchRequest::getPattern).extract(Maybe::flatten),
                    "__",
                    localClassName + "_obj",
                    leftType,
                    acceptor
            );
        });
    }


    public Maybe<String> compileMatchesExpression(
            Maybe<? extends EObject> input,
            Maybe<UnaryPrefix> unary
    ) {
        if (input.isNothing()) {
            return nothing();
        }

        EObject inputSafe = input.toNullable();

        String localClassName = "__PatternMatcher" + inputSafe.hashCode();
        String variableName = localClassName + "_obj";

        return of(variableName + ".__matches(" + module.get(UnaryPrefixExpressionSemantics.class).compile(unary).orElse("") + ")");
    }

    public Maybe<String> compileMatchesExpression(
            Maybe<PatternMatchRequest> input
    ) {
        return compileMatchesExpression(
                input.__(ProxyEObject::getProxyEObject),
                Maybe.flatten(input.__(PatternMatchRequest::getUnary))
        );
    }

    public List<? extends StatementWriter> generateAuxiliaryStatements(
            Maybe<PatternMatchRequest> input
    ) {

        List<StatementWriter> result = new ArrayList<>();


        String localClassName = "__PatternMatcher" + input.toNullable().getProxyEObject().hashCode();
        LocalClassStatementWriter localClass = new LocalClassStatementWriter(localClassName);

        final Maybe<UnaryPrefix> unary = input.__(PatternMatchRequest::getUnary)
                .extract(Maybe::flatten);
        IJadescriptType expectedType = module.get(UnaryPrefixExpressionSemantics.class).inferType(unary);
        final Maybe<Pattern> pattern = input.__(PatternMatchRequest::getPattern).extract(Maybe::flatten);
        if (pattern.isPresent()) {
            Maybe<IJadescriptType> inf = inferPatternType(pattern.toNullable());
            if (inf.isPresent() && !inf.toNullable().isErroneous()) {
                expectedType = module.get(TypeHelper.class).getGLB(expectedType, inf.toNullable());
            }
        }

        List<ClassMemberWriter> classMemberWriters = compilePatternMatchDetails(
                input,
                pattern,
                "__",
                localClassName + "_obj",
                expectedType
        );

        for (ClassMemberWriter m : classMemberWriters) {
            localClass.addMember(m);
        }

        String mainMethodInvokedFunction = classMemberWriters.stream()
                .flatMap(filterAndCast(MethodWriter.class))
                .findFirst()
                .map(MethodWriter::getName)
                .orElse("__");

        String mainMethodArgType = classMemberWriters.stream()
                .flatMap(filterAndCast(MethodWriter.class))
                .findFirst().stream()
                .flatMap(mw -> mw.getParameters().stream())
                .findFirst()
                .map(ParameterWriter::getType)
                .orElse("java.lang.Object");

        MethodWriter mainMethod = w.method(Visibility.PUBLIC, false, false, "boolean", "__matches")
                .addParameter(w.param("java.lang.Object", "__x"));


        mainMethod.getBody()
                .addStatement(w.ifStmnt(w.expr("!" + module.get(TypeHelper.class).noGenericsTypeName(mainMethodArgType) +
                        ".class.isInstance(__x)"), w.block()
                        .addStatement(w.returnStmnt(w.expr("false")))
                ))
                .addStatement(w.returnStmnt(w.callExpr("this." + mainMethodInvokedFunction, w.expr("(" + mainMethodArgType + ")__x"))));

        localClass.addMember(mainMethod);

        String variableName = localClassName + "_obj";
        VariableDeclarationWriter variable = w.variable(localClassName, variableName, w.expr("new " + localClassName + "()"));


        result.add(localClass);
        result.add(variable);

        return result;
    }

    private List<ClassMemberWriter> compilePatternMatchDetails(
            Maybe<PatternMatchRequest> input,
            Maybe<Pattern> p,
            String id,
            String ptmVarName,
            IJadescriptType expectedType
    ) {
        Maybe<StructurePattern> struct = p.__(Pattern::getStructPattern);
        Maybe<TuplePattern> tuple = p.__(Pattern::getTuplePattern);
        Maybe<ListPattern> list = p.__(Pattern::getListPattern);
        Maybe<MapOrSetPattern> mapOrSet = p.__(Pattern::getMapOrSetPattern);
        if (struct.isPresent()) {
            return compileStructPatternMatch(input, struct.toNullable(), id + "struct", ptmVarName, expectedType);
        } else if (tuple.isPresent()) {
            return compileTuplePatternMatch(input, tuple.toNullable(), id + "tuple", ptmVarName, expectedType);
        } else if (list.isPresent()) {
            return compileListPatternMatch(input, list.toNullable(), id + "list", ptmVarName, expectedType);
        } else if (mapOrSet.isPresent()) {
            if (mapOrSet.__(MapOrSetPattern::isIsMap).extract(nullAsFalse)) {
                return compileMapPatternMatch(input, mapOrSet.toNullable(), id + "map", ptmVarName, expectedType);
            } else {
                return compileSetPatternMatch(input, mapOrSet.toNullable(), id + "set", ptmVarName, expectedType);
            }
        } else {
            return Collections.emptyList();
        }
    }

    private List<ClassMemberWriter> compileSetPatternMatch(
            Maybe<PatternMatchRequest> input,
            MapOrSetPattern lp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType
    ) {
        List<ClassMemberWriter> result = new ArrayList<>();

        boolean isPipe = lp.isIsPipe();
        Maybe<PatternTerm> rest = of(lp.getRestPattern());
        List<Maybe<MapPatternKeyTerm>> keyTerms = toListOfMaybes(of(lp.getTermsKey()));

        final String adaptType = expectedType.compileToJavaTypeReference();
        if (keyTerms.stream().noneMatch(Maybe::isPresent)) {
            MethodWriter methodWriter = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                    .addParameter(w.param("java.lang.Object", "__objx"));
            if (!isPipe) {
                methodWriter.setBody(w.block()
                        .addStatements(compileAdaptType(adaptType))
                        .addStatement(w.returnStmnt(w.callExpr("__x.isEmpty"))));
            } else {
                String termBaseId = id + "_rest";
                List<ClassMemberWriter> cmws = compilePatternTerm(
                        input,
                        expectedType,
                        rest.toNullable(),
                        termBaseId,
                        ptmVarName
                );
                StringBuilder returnExpression = new StringBuilder("true");
                cmws.stream()
                        .flatMap(filterAndCast(MethodWriter.class))
                        .findFirst()
                        .ifPresent(mw -> {
                            returnExpression.append(" && ")
                                    .append(mw.getName())
                                    .append("(jadescript.util.JadescriptCollections.getRest(__x)) ");
                        });
                result.addAll(cmws);
                methodWriter.setBody(w.block()
                        .addStatements(compileAdaptType(adaptType))
                        .add(w.returnStmnt(w.callExpr(returnExpression.toString()))));
            }
            result.add(0, methodWriter);
        } else {
            List<String> toBeContained = new ArrayList<>();
            for (Maybe<MapPatternKeyTerm> maybeKeyTerm : keyTerms) {
                if (maybeKeyTerm.isPresent()) {
                    MapPatternKeyTerm keyTerm = maybeKeyTerm.toNullable();
                    toBeContained.add(compileKeyTerm(keyTerm, lp));
                }
            }

            MethodWriter mainMethod = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                    .addParameter(w.param("java.lang.Object", "__objx"));


            StringBuilder returnExpression = new StringBuilder("true");


            toBeContained.forEach(compiledKey -> {
                returnExpression.append(" && (")
                        .append("__x.contains(")
                        .append(compiledKey)
                        .append("))");
            });

            if (!isPipe) {
                returnExpression.append(" && (__x.size() == ")
                        .append(toBeContained.size())
                        .append(")");
            } else {
                if (rest.isPresent()) {
                    String termBaseId = id + "_rest";
                    List<ClassMemberWriter> cmws = compilePatternTerm(
                            input,
                            expectedType,
                            rest.toNullable(),
                            termBaseId,
                            ptmVarName
                    );

                    cmws.stream()
                            .flatMap(filterAndCast(MethodWriter.class))
                            .findFirst()
                            .ifPresent(mw -> {
                                returnExpression.append(" && ")
                                        .append(mw.getName())
                                        .append("(jadescript.util.JadescriptCollections.getRest(__x, ");
                                for (int i = 0; i < toBeContained.size(); i++) {
                                    if (i != 0) {
                                        returnExpression.append(", ");
                                    }
                                    returnExpression.append(toBeContained.get(i));
                                }
                                returnExpression.append("))");
                            });
                    result.addAll(cmws);
                }
            }

            mainMethod.getBody()
                    .addStatements(compileAdaptType(adaptType))
                    .addStatement(w.returnStmnt(w.expr(returnExpression.toString())));

            result.add(0, mainMethod);
        }


        return result;
    }

    private List<ClassMemberWriter> compileTuplePatternMatch(
            Maybe<PatternMatchRequest> input,
            TuplePattern tp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType
    ) {
        List<ClassMemberWriter> result = new ArrayList<>();
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        List<Maybe<PatternTerm>> terms = toListOfMaybes(of(tp.getTerms())).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());


        List<IJadescriptType> termTypes = new ArrayList<>();
        if (expectedType instanceof TupleType) {
            termTypes.addAll(((TupleType) expectedType).getElementTypes());
            while (termTypes.size() < terms.size()) {
                termTypes.add(typeHelper.ANY);
            }
        } else {
            for (int i = 0; i < terms.size(); i++) {
                termTypes.add(typeHelper.ANY);
            }
        }


        List<MethodWriter> toBeCalled = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            Maybe<PatternTerm> maybeTerm = terms.get(i);
            if (maybeTerm.isPresent()) {
                PatternTerm term = maybeTerm.toNullable();

                String termBaseId = id + "_" + i;
                List<ClassMemberWriter> cmws = compilePatternTerm(
                        input,
                        termTypes.get(i),
                        term,
                        termBaseId,
                        ptmVarName
                );

                cmws.stream()
                        .flatMap(filterAndCast(MethodWriter.class))
                        .findFirst()
                        .ifPresent(toBeCalled::add);

                result.addAll(cmws);
            }
        }

        final String adaptType = expectedType.compileToJavaTypeReference();
        MethodWriter mainMethod = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                .addParameter(w.param("java.lang.Object", "__objx"));

        StringBuilder returnExpression = new StringBuilder("__x.getLength() == " + terms.size());

        for (int i = 0; i < toBeCalled.size(); i++) {
            MethodWriter methodWriter = toBeCalled.get(i);
            final String get;
            if (expectedType instanceof TupleType) {
                get = ((TupleType) expectedType).compileGet("__x", i);
            } else {
                get = TupleType.compileStandardGet("__x", i);
            }
            returnExpression.append(" && ")
                    .append(methodWriter.getName())
                    .append("(").append(get).append(")");
        }

        mainMethod.getBody()
                .addStatements(compileAdaptType(adaptType))
                .addStatement(w.returnStmnt(w.expr(returnExpression.toString())));

        result.add(0, mainMethod);
        return result;
    }

    private List<ClassMemberWriter> compileListPatternMatch(
            Maybe<PatternMatchRequest> input,
            ListPattern lp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType
    ) {
        List<ClassMemberWriter> result = new ArrayList<>();
        List<Maybe<PatternTerm>> terms = toListOfMaybes(of(lp.getTerms()));

        final String adaptType = expectedType.compileToJavaTypeReference();
        if (terms.stream().noneMatch(Maybe::isPresent)) {
            MethodWriter methodWriter = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                    .addParameter(w.param("java.lang.Object", "__objx"));

            if (lp.isIsPipe() && lp.getRestPattern() != null) {
                String termBaseId = id + "_rest";
                List<ClassMemberWriter> cmws = compilePatternTerm(
                        input,
                        expectedType,
                        lp.getRestPattern(),
                        termBaseId,
                        ptmVarName
                );


                StringBuilder returnExpression = new StringBuilder("true");
                cmws.stream()
                        .flatMap(filterAndCast(MethodWriter.class))
                        .findFirst()
                        .ifPresent(mw -> {
                            returnExpression.append(" && ")
                                    .append(mw.getName())
                                    .append("(jadescript.util.JadescriptCollections.getRest(__x))");
                        });

                result.addAll(cmws);

                methodWriter.setBody(w.block()
                        .addStatements(compileAdaptType(adaptType))
                        .addStatement(w.returnStmnt(w.expr(returnExpression.toString()))));
            } else {
                methodWriter.setBody(w.block()
                        .addStatements(compileAdaptType(adaptType))
                        .addStatement(w.returnStmnt(w.callExpr("__x.isEmpty"))));
            }
            result.add(0, methodWriter);
        } else {
            JvmTypeReference termType = module.get(TypeHelper.class).getArrayListMapComponentType(expectedType.asJvmTypeReference());

            List<MethodWriter> toBeCalled = new ArrayList<>();
            for (int i = 0; i < terms.size(); i++) {
                Maybe<PatternTerm> maybeTerm = terms.get(i);
                if (maybeTerm.isPresent()) {
                    PatternTerm term = maybeTerm.toNullable();

                    String termBaseId = id + "_" + i;
                    List<ClassMemberWriter> cmws = compilePatternTerm(
                            input,
                            module.get(TypeHelper.class).jtFromJvmTypeRef(termType),
                            term,
                            termBaseId,
                            ptmVarName
                    );

                    cmws.stream()
                            .flatMap(filterAndCast(MethodWriter.class))
                            .findFirst()
                            .ifPresent(toBeCalled::add);

                    result.addAll(cmws);
                }
            }
            AtomicReference<Maybe<MethodWriter>> toBeCalledForRestMatch = new AtomicReference<>(nothing());
            if (lp.isIsPipe() && lp.getRestPattern() != null) {
                String termBaseId = id + "_rest";
                List<ClassMemberWriter> cmws = compilePatternTerm(
                        input,
                        expectedType,
                        lp.getRestPattern(),
                        termBaseId,
                        ptmVarName
                );


                cmws.stream()
                        .flatMap(filterAndCast(MethodWriter.class))
                        .findFirst()
                        .ifPresent(it -> toBeCalledForRestMatch.set(of(it)));

                result.addAll(cmws);
            }
            MethodWriter mainMethod = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                    .addParameter(w.param("java.lang.Object", "__objx"));

            String sizeOp = (toBeCalledForRestMatch.get().isPresent()) ? ">=" : "==";

            StringBuilder returnExpression = new StringBuilder("__x.size()" + sizeOp + terms.size());

            for (int i = 0; i < toBeCalled.size(); i++) {
                MethodWriter methodWriter = toBeCalled.get(i);
                returnExpression.append(" && ")
                        .append(methodWriter.getName())
                        .append("(__x.get(").append(i).append("))");
            }

            if (toBeCalledForRestMatch.get().isPresent()) {
                returnExpression.append(" && ")
                        .append(toBeCalledForRestMatch.get().toNullable().getName())
                        .append("(jadescript.util.JadescriptCollections.getRest(__x, ")
                        .append(terms.size())
                        .append("))");

            }

            mainMethod.getBody()
                    .addStatements(compileAdaptType(adaptType))
                    .addStatement(w.returnStmnt(w.expr(returnExpression.toString())));

            result.add(0, mainMethod);
        }


        return result;
    }

    private List<ClassMemberWriter> compileMapPatternMatch(
            Maybe<PatternMatchRequest> input,
            MapOrSetPattern lp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType
    ) {
        List<ClassMemberWriter> result = new ArrayList<>();

        boolean isPipe = lp.isIsPipe();
        Maybe<PatternTerm> rest = of(lp.getRestPattern());
        List<Maybe<MapPatternKeyTerm>> keyTerms = toListOfMaybes(of(lp.getTermsKey()));
        List<Maybe<PatternTerm>> terms = toListOfMaybes(of(lp.getTermsValue()));

        final String adaptType = expectedType.compileToJavaTypeReference();
        if (terms.stream().noneMatch(Maybe::isPresent)) {
            MethodWriter methodWriter = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                    .addParameter(w.param("java.lang.Object", "__objx"));
            if (!isPipe) {
                methodWriter.setBody(w.block()
                        .addStatements(compileAdaptType(adaptType))
                        .addStatement(w.returnStmnt(w.callExpr("__x.isEmpty"))));
            } else {
                String termBaseId = id + "_rest";
                List<ClassMemberWriter> cmws = compilePatternTerm(
                        input,
                        expectedType,
                        rest.toNullable(),
                        termBaseId,
                        ptmVarName
                );
                StringBuilder returnExpression = new StringBuilder("true");
                cmws.stream()
                        .flatMap(filterAndCast(MethodWriter.class))
                        .findFirst()
                        .ifPresent(mw -> {
                            returnExpression.append(" && ")
                                    .append(mw.getName())
                                    .append("(jadescript.util.JadescriptCollections.getRest(__x)) ");
                        });
                result.addAll(cmws);
                methodWriter.setBody(w.block()
                        .addStatements(compileAdaptType(adaptType))
                        .add(w.returnStmnt(w.expr(returnExpression.toString()))));
            }
            result.add(0, methodWriter);
        } else {
            JvmTypeReference termType = module.get(TypeHelper.class).getArrayListMapComponentType(expectedType.asJvmTypeReference());
            //JvmTypeReference keyTermType = getKeyTypeOfMap(expectedType); //no need

            Map<String, MethodWriter> toBeCalled = new HashMap<>();
            for (int i = 0; i < terms.size(); i++) {
                Maybe<PatternTerm> maybeTerm = terms.get(i);
                Maybe<MapPatternKeyTerm> maybeKeyTerm = keyTerms.get(i);

                if (maybeTerm.isPresent() && maybeKeyTerm.isPresent()) {
                    PatternTerm term = maybeTerm.toNullable();
                    MapPatternKeyTerm keyTerm = maybeKeyTerm.toNullable();

                    String termBaseId = id + "_" + i;
                    List<ClassMemberWriter> cmws = compilePatternTerm(
                            input,
                            module.get(TypeHelper.class).jtFromJvmTypeRef(termType),
                            term,
                            termBaseId,
                            ptmVarName
                    );

                    cmws.stream()
                            .flatMap(filterAndCast(MethodWriter.class))
                            .findFirst()
                            .ifPresent(mw -> toBeCalled.put(compileKeyTerm(keyTerm, lp), mw));

                    result.addAll(cmws);
                }
            }

            MethodWriter mainMethod = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                    .addParameter(w.param("java.lang.Object", "__objx"));


            StringBuilder returnExpression = new StringBuilder("true");


            toBeCalled.forEach((compiledKey, methodWriter) -> {
                returnExpression.append(" && (")
                        .append("__x.containsKey(")
                        .append(compiledKey)
                        .append(") &&")
                        .append(methodWriter.getName())
                        .append("(__x.get(").append(compiledKey).append(")))");
            });

            if (!isPipe) {
                returnExpression.append(" && (__x.size() == ")
                        .append(toBeCalled.size())
                        .append(")");
            } else {
                if (rest.isPresent()) {
                    String termBaseId = id + "_rest";
                    List<ClassMemberWriter> cmws = compilePatternTerm(
                            input,
                            expectedType,
                            rest.toNullable(),
                            termBaseId,
                            ptmVarName
                    );

                    cmws.stream()
                            .flatMap(filterAndCast(MethodWriter.class))
                            .findFirst()
                            .ifPresent(mw -> {
                                returnExpression.append(" && ")
                                        .append(mw.getName())
                                        .append("(jadescript.util.JadescriptCollections.getRest(__x, ");

                                final List<String> keys = new ArrayList<>(toBeCalled.keySet());
                                for (int i = 0; i < keys.size(); i++) {
                                    if (i != 0) {
                                        returnExpression.append(", ");
                                    }
                                    returnExpression.append(keys.get(i));
                                }
                                returnExpression.append("))");
                            });
                    result.addAll(cmws);
                }
            }

            mainMethod.getBody()
                    .addStatements(compileAdaptType(adaptType))
                    .addStatement(w.returnStmnt(w.expr(returnExpression.toString())));

            result.add(0, mainMethod);
        }


        return result;

    }

    private String compileKeyTerm(MapPatternKeyTerm keyTerm, EObject refObject) {
        if (keyTerm == null) {
            return "null";
        }
        if (keyTerm.getBoolean() != null) {
            return keyTerm.getBoolean();
        } else if (keyTerm.getNumber() != null) {
            return keyTerm.getNumber();
        } else if (keyTerm.getText() != null) {
            return module.get(StringLiteralSemantics.class).compile(of(keyTerm.getText()))
                    .orElse("\"/*missing text*/\"");
        } else if (keyTerm.getIdentifier() != null) {
            return module.get(SingleIdentifierExpressionSemantics.class).compile(
                    VirtualIdentifier.virtualIdentifier(
                            of(keyTerm.getIdentifier()),
                            of(refObject)
                    )
            ).orElse(keyTerm.getIdentifier());
        }

        return "null";
    }

    private List<ClassMemberWriter> compilePatternTerm(
            Maybe<PatternMatchRequest> input,
            IJadescriptType type,
            PatternTerm pt,
            String id,
            String ptmVarName
    ) {
        if (pt.isUnderscore()) {
            return compileUnderscorePatternMatch(id + "ph", pt);
        } else if (pt.getBoolean() != null) {
            return compileLiteralPatternMatch(type, pt.getBoolean(), id + "lt");
        } else if (pt.getNumber() != null) {
            return compileLiteralPatternMatch(type, pt.getNumber(), id + "lt");
        } else if (pt.getText() != null) {
            return compileLiteralPatternMatch(
                    type,
                    module.get(StringLiteralSemantics.class).compile(of(pt.getText()))
                            .orElse("\"/*missing text*/\""),
                    id + "lt"
            );
        } else if (pt.getSubPattern() != null) {
            return compilePatternMatchDetails(input, of(pt.getSubPattern()), id + "_", ptmVarName, type);
        } else {
            return Collections.emptyList();
        }
    }


    private List<ClassMemberWriter> compileIdentifierPatternMatch(
            Maybe<PatternMatchRequest> input,
            IJadescriptType type,
            String identifier,
            String id,
            EObject referenceObject,
            String ptmVarName
    ) {
        List<ClassMemberWriter> result = new ArrayList<>();

        Maybe<VirtualIdentifier> virtID = VirtualIdentifier.virtualIdentifier(of(identifier), of(referenceObject));


        final String adaptType = type.compileToJavaTypeReference();
        MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                .addParameter(w.param("java.lang.Object", "__objx"));
        if (module.get(SingleIdentifierExpressionSemantics.class).resolves(virtID)) {
            Maybe<String> va = module.get(SingleIdentifierExpressionSemantics.class).compile(virtID);
            String vas;

            if (va.__(s -> s.startsWith(ptmVarName)).extract(nullAsFalse)) {
                vas = identifier;
            } else {
                vas = va.orElse(identifier);
            }
            m.getBody()
                    .addStatements(compileAdaptType(adaptType))
                    .addStatement(w.returnStmnt(w.expr("java.util.Objects.equals(__x," + vas + ")")));

        } else {
            if (input.__(PatternMatchRequest::canDeconstruct).extract(nullAsFalse)) {
                String localClassName = "__PatternMatcher" + input.__(ProxyEObject::getProxyEObject)
                        .__(Objects::hashCode).orElse(0);

                module.get(ContextManager.class).currentScope().addNamedElement(
                        new PatternMatchAutoDeclaredVariable(
                                identifier,
                                type,
                                localClassName + "_obj."
                        )
                );
                FieldWriter f = w.field(Visibility.PUBLIC, false, false, adaptType, identifier);
                m.getBody()
                        .addStatements(compileAdaptType(adaptType))
                        .addStatement(w.assign(identifier, w.expr("__x")))
                        .addStatement(w.returnStmnt(w.expr("true")));
                result.add(f);
            }
        }
        result.add(m);
        return result;
    }

    private List<ClassMemberWriter> compileLiteralPatternMatch(
            IJadescriptType type,
            String boolLiteral,
            String id
    ) {

        final String adaptType = type.compileToJavaTypeReference();
        MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                .addParameter(w.param("java.lang.Object", "__objx"));
        m.getBody()
                .addStatements(compileAdaptType(adaptType))
                .addStatement(w.returnStmnt(w.expr("java.util.Objects.equals(__x," + boolLiteral + ")")));
        return Collections.singletonList(m);
    }

    private List<ClassMemberWriter> compileUnderscorePatternMatch(String id, PatternTerm pt) {
        MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                .addParameter(w.param("java.lang.Object", "__objx"));
        final Maybe<TypeExpression> expectedType = of(pt.getExpectedType());
        if (expectedType.isPresent()) {
            final String adaptType = module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(expectedType).compileToJavaTypeReference();
            m.getBody()
                    .addStatements(compileAdaptType(adaptType))
                    .addStatement(w.returnStmnt(w.expr("true")));
        } else {
            m.getBody()
                    .addStatement(w.returnStmnt(w.expr("true")));
        }

        return Collections.singletonList(m);
    }

    private List<ClassMemberWriter> compileStructPatternMatch(
            Maybe<PatternMatchRequest> input,
            StructurePattern p,
            String id,
            String ptmVarName,
            IJadescriptType type
    ) {
        String patternName = p.getName();


        if (!p.isWithParentheses()) {
            // match as single identifier
            IJadescriptType expectType = type;
            if (p.isWithAs() && p.getExpectedType() != null) {
                expectType = module.get(TypeExpressionSemantics.class).toJadescriptType(of(p.getExpectedType()));
            }
            return compileIdentifierPatternMatch(input, expectType, p.getName(), id + "id", p, ptmVarName);
        }

        Optional<? extends CallableSymbol> methods = module.get(ContextManager.class).currentContext().searchAs(
                CallableSymbol.Searcher.class,
                searcher -> searcher.searchCallable(
                        patternName,
                        null,
                        (s, n) -> s == p.getTerms().size(),
                        (s, t) -> s == p.getTerms().size()
                )
        ).findFirst();

        if (methods.isPresent()) {
            CallableSymbol m = methods.get();
            List<IJadescriptType> patternTermTypes = m.parameterTypes();
            List<ClassMemberWriter> result = new ArrayList<>();
            List<MethodWriter> toBeCalled = new ArrayList<>();
            EList<PatternTerm> terms = p.getTerms();
            for (int i = 0; i < terms.size(); i++) {
                PatternTerm term = terms.get(i);
                IJadescriptType destType = patternTermTypes.get(i);
                String termBaseId = id + "_" + i;
                List<ClassMemberWriter> cmws = compilePatternTerm(input, destType, term, termBaseId, ptmVarName);

                cmws.stream()
                        .flatMap(filterAndCast(MethodWriter.class))
                        .findFirst()
                        .ifPresent(toBeCalled::add);

                result.addAll(cmws);
            }


            MethodWriter mainMethod = w.method(Visibility.PUBLIC, false, false, "boolean", id)
                    .addParameter(w.param("java.lang.Object", "__objx"));


            StringBuilder returnExpression = new StringBuilder("true");

            for (int i = 0; i < toBeCalled.size(); i++) {
                MethodWriter methodWriter = toBeCalled.get(i);
                returnExpression.append(" && ");
                returnExpression.append(methodWriter.getName());
                returnExpression.append("(__x.get");
                returnExpression.append(Strings.toFirstUpper(m.parameterNames().get(i)));
                returnExpression.append("())");
            }

            mainMethod.getBody()
                    .addStatements(compileAdaptType(m.returnType().compileToJavaTypeReference()))
                    .addStatement(w.returnStmnt(w.expr(returnExpression.toString())));

            result.add(0, mainMethod);

            return result;
        }


        return Collections.emptyList();
    }

    private List<StatementWriter> compileAdaptType(String adaptType) {
        final ReturnStatementWriter returnFalse = w.returnStmnt(w.False);

        final VariableDeclarationWriter declareX = w.variable(adaptType, "__x");//initialized later
        final TryCatchWriter checkXType = w.tryCatch(w.block()
                        .addStatement(w.ifStmnt(
                                w.expr("__objx instanceof " + module.get(TypeHelper.class).noGenericsTypeName(adaptType)),
                                w.block().addStatement(w.assign("__x", w.expr("(" + adaptType + ") __objx")))
                        ).setElseBranch(w.block().addStatement(returnFalse))))
                .addCatchBranch("java.lang.ClassCastException", "ignored", w.block()
                        .addStatement(returnFalse));
        return Arrays.asList(declareX, checkXType);
    }

    private void validatePatternMatchDetails(
            Maybe<PatternMatchRequest> input,
            Maybe<Pattern> p,
            String id,
            String ptmVarName,
            IJadescriptType expectedType,
            ValidationMessageAcceptor acceptor
    ) {
        Maybe<StructurePattern> struct = p.__(Pattern::getStructPattern);
        Maybe<ListPattern> list = p.__(Pattern::getListPattern);
        Maybe<TuplePattern> tuple = p.__(Pattern::getTuplePattern);
        Maybe<MapOrSetPattern> map = p.__(Pattern::getMapOrSetPattern);
        if (list.isPresent()) {
            Maybe<IJadescriptType> listType = validateListPatternMatch(
                    input, list.toNullable(),
                    id + "list",
                    ptmVarName,
                    expectedType,
                    acceptor
            );
            listType.safeDo(listTypeSafe -> module.get(ValidationHelper.class).assertExpectedType(
                    expectedType,
                    listTypeSafe,
                    "UnexpectedTermType",
                    p,
                    acceptor
            ));
        } else if (map.isPresent()) {
            final boolean isMap = map.__(MapOrSetPattern::isIsMap).extract(Maybe.nullAsFalse);
            final List<Maybe<PatternTerm>> values = toListOfMaybes(map.__(MapOrSetPattern::getTermsValue));
            final List<Maybe<MapPatternKeyTerm>> keys = toListOfMaybes(map.__(MapOrSetPattern::getTermsKey));

            InterceptAcceptor stage1Validation = new InterceptAcceptor(acceptor);
            if (isMap) {
                module.get(ValidationHelper.class).assertion(
                        values.stream().filter(Maybe::isPresent).count()
                                == keys.stream().filter(Maybe::isPresent).count(),
                        "InvalidMapLiteral",
                        "Non-matching number of keys and values in the map pattern",
                        input,
                        stage1Validation
                );
            } else {
                module.get(ValidationHelper.class).assertion(
                        values.stream().noneMatch(Maybe::isPresent),
                        "InvalidSetLiteral",
                        "Unexpected pair separator ':' encountered in set pattern",
                        input,
                        stage1Validation
                );
            }
            if (!stage1Validation.thereAreErrors()) {
                if (isMap) {
                    Maybe<IJadescriptType> mapType = validateMapPatternMatch(
                            input, map.toNullable(),
                            id + "map",
                            ptmVarName,
                            expectedType,
                            acceptor
                    );
                    mapType.safeDo(mapTypeSafe -> module.get(ValidationHelper.class).assertExpectedType(
                            expectedType,
                            mapTypeSafe,
                            "UnexpectedTermType",
                            p,
                            acceptor
                    ));
                } else {
                    Maybe<IJadescriptType> setType = validateSetPatternMatch(
                            input, map.toNullable(),
                            id + "set",
                            ptmVarName,
                            expectedType,
                            acceptor
                    );
                    setType.safeDo(setTypeSafe -> module.get(ValidationHelper.class).assertExpectedType(
                            expectedType,
                            setTypeSafe,
                            "UnexpectedTermType",
                            p,
                            acceptor
                    ));
                }
            }
        } else if (struct.isPresent()) {
            Maybe<IJadescriptType> structType = validateStructPatternMatch(
                    input,
                    struct.toNullable(),
                    id + "struct",
                    ptmVarName,
                    expectedType,
                    acceptor
            );
            structType.safeDo(structTypeSafe -> module.get(ValidationHelper.class).assertExpectedType(
                    expectedType,
                    structTypeSafe,
                    "UnexpectedTermType",
                    p,
                    acceptor
            ));
        } else if (tuple.isPresent()) {
            Maybe<IJadescriptType> tupleType = validateTuplePatternMatch(
                    input,
                    tuple.toNullable(),
                    id + "tuple",
                    ptmVarName,
                    expectedType,
                    acceptor
            );
            tupleType.safeDo(tupleTypeSafe -> module.get(ValidationHelper.class).assertExpectedType(
                    expectedType,
                    tupleTypeSafe,
                    "UnexpectedTermType",
                    p,
                    acceptor
            ));
        }
    }

    private Maybe<IJadescriptType> validateStructPatternMatch(
            Maybe<PatternMatchRequest> input,
            StructurePattern p,
            String id,
            String ptmVarName,
            IJadescriptType expectedType,
            ValidationMessageAcceptor acceptor
    ) {
        String patternName = p.getName();

        if (!p.isWithParentheses()) {
            // match as single identifier
            IJadescriptType expectType = expectedType;
            InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
            module.get(TypeExpressionSemantics.class).validate(of(p.getExpectedType()), typeValidation);
            if (p.isWithAs() && p.getExpectedType() != null && !typeValidation.thereAreErrors()) {
                expectType = module.get(TypeExpressionSemantics.class).toJadescriptType(of(p.getExpectedType()));
            }

            return validateIdentifierPatternMatch(input, expectType, p.getName(), p, acceptor);
        }

        List<? extends CallableSymbol> methods = module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                                patternName,
                                null,
                                (s, n) -> s == p.getTerms().size(),
                                (s, t) -> s == p.getTerms().size()
                        )
                ).filter(Util.dinstinctBy(CallableSymbol::sourceLocation))
                .collect(Collectors.toList());

        if (methods.isEmpty()) {
            acceptor.acceptError(
                    "cannot resolve pattern: " + Util.getSignature(patternName, p.getTerms().size()),
                    p,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidMethodCall"
            );
            return nothing();
        } else if (methods.size() == 1) {
            CallableSymbol m = methods.get(0);

            List<IJadescriptType> patternTermTypes = m.parameterTypes();
            EList<PatternTerm> terms = p.getTerms();
            for (int i = 0; i < terms.size(); i++) {
                PatternTerm term = terms.get(i);
                IJadescriptType type = patternTermTypes.get(i);
                String termBaseId = id + "_" + i;
                validatePatternTerm(input, type, term, termBaseId, ptmVarName, acceptor);
            }
            return of(m.returnType());
        } else {//methods.size() > 1
            List<String> candidatesMessage = new ArrayList<>();
            candidatesMessage.add("candidates: ");
            for (CallableSymbol match : methods) {
                candidatesMessage.add(Util.getSignature(patternName, match.parameterTypes())
                        + " in " + match.sourceLocation() + ";");
            }

            acceptor.acceptError(
                    "Ambiguous pattern: " + Util.getSignature(patternName, p.getTerms().size()) +
                            ". Candidates:" +
                            "\n• " +
                            String.join("\n• ", candidatesMessage),
                    p,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidPattern"
            );
            return nothing();
        }


    }

    private Maybe<IJadescriptType> validateTuplePatternMatch(
            Maybe<PatternMatchRequest> input,
            TuplePattern tp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType,
            ValidationMessageAcceptor acceptor
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        List<Maybe<PatternTerm>> terms = toListOfMaybes(of(tp.getTerms()));

        module.get(ValidationHelper.class).assertion(
                terms.stream().filter(Maybe::isPresent).count() <= 20,
                "TooBigTuple",
                "Tuples with more than 20 elements are not supported.",
                input,
                acceptor
        );

        // if super pattern thinks we are a tuple, what types of element does it expect us to have?
        List<Maybe<IJadescriptType>> elementTypesSuperExpects = new ArrayList<>();
        if (expectedType instanceof TupleType) {
            for (int i = 0; i < ((TupleType) expectedType).getElementTypes().size(); i++) {
                Maybe.paddedSet(
                        elementTypesSuperExpects,
                        i,
                        of(((TupleType) expectedType).getElementTypes().get(i))
                );
            }
        }
        for (int i = 0; i < elementTypesSuperExpects.size(); i++) {
            if (elementTypesSuperExpects.get(i).isPresent() && elementTypesSuperExpects.get(i).toNullable().isErroneous()) {
                elementTypesSuperExpects.set(i, nothing());
            }
        }

        // what element types we have based only on the information provided by the sub patterns?
        List<Maybe<IJadescriptType>> elementTypesProvidedBySubs = new ArrayList<>();
        final Maybe<IJadescriptType> maybeTupleType = inferTuplePatternType(tp);
        if (maybeTupleType.isPresent()) {
            final IJadescriptType tupleType = maybeTupleType.toNullable();
            if (tupleType instanceof TupleType) {
                for (int i = 0; i < ((TupleType) tupleType).getElementTypes().size(); i++) {
                    Maybe.paddedSet(
                            elementTypesProvidedBySubs,
                            i,
                            of(((TupleType) tupleType).getElementTypes().get(i))
                    );
                }
            }
        }
        for (int i = 0; i < elementTypesProvidedBySubs.size(); i++) {
            if (elementTypesProvidedBySubs.get(i).isPresent() && elementTypesProvidedBySubs.get(i).toNullable().isErroneous()) {
                elementTypesProvidedBySubs.set(i, nothing());
            }
        }

        // what is the elements type information eventually forced by the user with specifiers?
        List<Maybe<IJadescriptType>> elementTypesImposedByUser = new ArrayList<>();
        final TypeExpressionSemantics typeExpressionSemantics = module.get(TypeExpressionSemantics.class);
        if (tp.isWithTypeSpecifiers()) {
            InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
            for (TypeExpression typeSpecifier : tp.getTypeSpecifiers()) {
                module.get(TypeExpressionSemantics.class).validate(of(typeSpecifier), typeValidation);
            }
            if (!typeValidation.thereAreErrors()) {
                for (int i = 0; i < tp.getTypeSpecifiers().size(); i++) {

                    Maybe.paddedSet(
                            elementTypesImposedByUser,
                            i,
                            of(typeExpressionSemantics.toJadescriptType(of(tp.getTypeSpecifiers().get(i))))
                    );
                }
            }
        }

        // what type of elements we conclude we have
        List<Maybe<IJadescriptType>> computedElementTypes = new ArrayList<>();
        int size = Math.max(Math.max(elementTypesImposedByUser.size(), elementTypesProvidedBySubs.size()), elementTypesSuperExpects.size());
        for (int i = 0; i < size; i++) {
            Maybe<IJadescriptType> elementTypeImposedByUser = Maybe.paddedGet(
                    elementTypesImposedByUser,
                    i
            );
            if (elementTypeImposedByUser.isPresent()) {
                // the user specification overrides everything.
                Maybe.paddedSet(
                        computedElementTypes,
                        i,
                        elementTypeImposedByUser
                );
            } else {
                Maybe<IJadescriptType> elementTypeSuperExpects = Maybe.paddedGet(
                        elementTypesSuperExpects,
                        i
                );
                Maybe<IJadescriptType> elementTypeProvidedBySubs = Maybe.paddedGet(
                        elementTypesProvidedBySubs,
                        i
                );
                if (elementTypeSuperExpects.isPresent() && elementTypeProvidedBySubs.isPresent()) {
                    // we have both info from super pattern and from sub patterns.
                    //  they have to be the same.
                    if (elementTypeSuperExpects.toNullable().typeEquals(elementTypeProvidedBySubs.toNullable())) {
                        Maybe.paddedSet(computedElementTypes, i, elementTypeSuperExpects);
                    }
                } else if (elementTypeSuperExpects.isPresent()) {
                    // we have only information from super patterns, we use that
                    Maybe.paddedSet(computedElementTypes, i, elementTypeSuperExpects);
                } else if (elementTypeProvidedBySubs.isPresent()) {
                    // we have only information from sub patterns, we use that
                    Maybe.paddedSet(computedElementTypes, i, elementTypeProvidedBySubs);
                } else {
                    // -> we leave computedElementType null.
                    Maybe.paddedSet(computedElementTypes, i, nothing());
                }
            }
        }

        // what type we conclude we are
        IJadescriptType computedType = typeHelper.TUPLE.apply(
                computedElementTypes.stream()
                        .map(mt -> mt.orElse(typeHelper.ANY))
                        .collect(Collectors.toList())
        );

        InterceptAcceptor globalIntercept = new InterceptAcceptor(acceptor);
        for (int j = 0; j < computedElementTypes.size(); j++) {
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(globalIntercept);
            Maybe<IJadescriptType> elementTypeImposedByUser = Maybe.paddedGet(
                    elementTypesImposedByUser,
                    j
            );
            Maybe<IJadescriptType> computedElementType = Maybe.paddedGet(
                    computedElementTypes,
                    j
            );
            Maybe<IJadescriptType> elementTypeSuperExpects = Maybe.paddedGet(
                    elementTypesSuperExpects,
                    j
            );
            Maybe<IJadescriptType> elementTypeProvidedBySubs = Maybe.paddedGet(
                    elementTypesProvidedBySubs,
                    j
            );
            module.get(ValidationHelper.class).assertion(
                    Util.implication(
                            elementTypeImposedByUser.isNothing(),
                            computedElementType.isPresent() && !computedElementType.toNullable().isErroneous()
                    ),
                    "InvalidPatternType",
                    "Could not compute a valid type for element in tuple at position " + j + "; " +
                            "Specify the type of the tuple pattern using the 'as' clause"
                            + " (expected element type = '" + elementTypeSuperExpects
                            .__(IJadescriptType::getJadescriptName)
                            .extract(nullAsDefaultString("?")) + "'; "
                            + "sub-pattern-provided element type = '" + elementTypeProvidedBySubs
                            .__(IJadescriptType::getJadescriptName)
                            .extract(nullAsDefaultString("?")) + "'; "
                            + "computed element type = '" + computedElementType
                            .__(IJadescriptType::getJadescriptName)
                            .extract(nullAsDefaultString("?")) + "').",
                    of(tp),
                    JadescriptPackage.eINSTANCE.getTuplePattern_Terms(),
                    j < terms.stream().filter(Maybe::isPresent).count() ? j : ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    interceptAcceptor
            );

            if (!interceptAcceptor.thereAreErrors() && computedElementType.isPresent()) {
                computedElementType.toNullable().validateType(of(tp), interceptAcceptor);
            }
            if (!interceptAcceptor.thereAreErrors() && computedElementType.isPresent()) {

                Maybe<PatternTerm> term = terms.get(j);
                if (term.isPresent()) {
                    PatternTerm termSafe = term.toNullable();
                    String termBaseId = id + "_" + j;
                    validatePatternTerm(
                            input,
                            computedElementType.toNullable(),
                            termSafe,
                            termBaseId,
                            ptmVarName,
                            acceptor
                    );
                }

            }

        }

        if (globalIntercept.thereAreErrors()) {
            return of(expectedType); // to mute validations on the type on the super-patterns
        } else {
            return of(computedType);
        }

    }


    private Maybe<IJadescriptType> validateListPatternMatch(
            Maybe<PatternMatchRequest> input,
            ListPattern lp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType,
            ValidationMessageAcceptor acceptor
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        List<Maybe<PatternTerm>> terms = toListOfMaybes(of(lp.getTerms()));

        //if super pattern thinks we are a list, what type of element does it expect we have?
        IJadescriptType elementTypeSuperExpects = null;
        if (expectedType instanceof ListType) {
            elementTypeSuperExpects = ((ListType) expectedType).getElementType();
        }
        if (elementTypeSuperExpects != null && elementTypeSuperExpects.isErroneous()) {
            elementTypeSuperExpects = null;
        }

        // what element type we have based only on the information provided by the sub patterns?
        IJadescriptType elementTypeProvidedBySubs = null;
        final Maybe<IJadescriptType> maybeListType = inferListPatternType(lp);
        if (maybeListType.isPresent()) {
            final IJadescriptType listType = maybeListType.toNullable();
            if (listType instanceof ListType) {
                elementTypeProvidedBySubs = ((ListType) listType).getElementType();
            }
        }
        if (elementTypeProvidedBySubs != null && elementTypeProvidedBySubs.isErroneous()) {
            //drop it on the floor
            elementTypeProvidedBySubs = null;
        }

        // what is the element type information eventually forced by the user with specifiers?
        IJadescriptType elementTypeImposedByUser = null;
        if (lp.isWithTypeSpecifier()) {
            InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
            module.get(TypeExpressionSemantics.class).validate(of(lp.getTypeParameter()), typeValidation);
            if (!typeValidation.thereAreErrors()) {
                elementTypeImposedByUser = module.get(TypeExpressionSemantics.class)
                        .toJadescriptType(of(lp.getTypeParameter()));
            }
        }


        // what type of element we conclude we have
        IJadescriptType computedElementType = null;
        if (elementTypeImposedByUser != null) {
            // the user specification overrides everything.
            computedElementType = elementTypeImposedByUser;
        } else {
            if (elementTypeSuperExpects != null && elementTypeProvidedBySubs != null) {
                // we have both info from super pattern and from sub patterns.
                //  they have to be the same.
                if (elementTypeSuperExpects.typeEquals(elementTypeProvidedBySubs)) {
                    computedElementType = elementTypeSuperExpects;
                }
            } else if (elementTypeSuperExpects != null) {
                // we have only information from super patterns, we use that
                computedElementType = elementTypeSuperExpects;
            } else if (elementTypeProvidedBySubs != null) {
                // we have only information from sub patterns, we use that
                computedElementType = elementTypeProvidedBySubs;
            } // else -> we leave computedElementType null.
        }

        // what type we conclude we are
        IJadescriptType computedType = typeHelper.LIST.apply(Arrays.asList(
                computedElementType == null ? typeHelper.ANY : computedElementType
        ));

        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(ValidationHelper.class).assertion(
                Util.implication(
                        elementTypeImposedByUser == null,
                        computedElementType != null && !computedElementType.isErroneous()
                ),
                "InvalidPatternType",
                "Could not compute a valid list element type; " +
                        "Specify the type of the list pattern using the 'of' clause"
                        + " (expected element type = '" + elementTypeSuperExpects + "'; "
                        + "sub-pattern-inferred element type = '" + elementTypeProvidedBySubs + "'; "
                        + "computed element type = '" + computedElementType + "').",
                of(lp),
                interceptAcceptor
        );

        if (!interceptAcceptor.thereAreErrors() && computedElementType != null) {
            computedElementType.validateType(of(lp), interceptAcceptor);
        }

        if (!interceptAcceptor.thereAreErrors() && computedElementType != null) {
            for (int i = 0; i < terms.size(); i++) {
                Maybe<PatternTerm> term = terms.get(i);
                if (term.isPresent()) {
                    PatternTerm termSafe = term.toNullable();
                    String termBaseId = id + "_" + i;
                    validatePatternTerm(
                            input,
                            computedElementType,
                            termSafe,
                            termBaseId,
                            ptmVarName,
                            acceptor
                    );
                }
            }

            if (lp.isIsPipe() && lp.getRestPattern() != null) {
                String termBaseId = id + "_rest";
                validatePatternTerm(
                        input,
                        computedType,
                        lp.getRestPattern(),
                        termBaseId,
                        ptmVarName,
                        acceptor
                );
            }
        }


        if (interceptAcceptor.thereAreErrors()) {
            return of(expectedType); // to mute validations on the type on the super-patterns
        } else {
            return of(computedType);
        }
    }


    private Maybe<IJadescriptType> validateMapPatternMatch(
            Maybe<PatternMatchRequest> input,
            MapOrSetPattern lp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType,
            ValidationMessageAcceptor acceptor
    ) {

        List<Maybe<PatternTerm>> terms = toListOfMaybes(of(lp.getTermsValue()));
        List<Maybe<MapPatternKeyTerm>> keyTerms = toListOfMaybes(of(lp.getTermsKey()));

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        //if super pattern thinks we are a map, what type of elements does it expect we have?
        IJadescriptType valueTypeSuperExpects = null;
        IJadescriptType keyTypeSuperExpects = null;
        if (expectedType instanceof MapType) {
            valueTypeSuperExpects = ((MapType) expectedType).getValueType();
            keyTypeSuperExpects = ((MapType) expectedType).getKeyType();
        }
        if (valueTypeSuperExpects != null && valueTypeSuperExpects.isErroneous()) {
            valueTypeSuperExpects = null;
        }
        if (keyTypeSuperExpects != null && keyTypeSuperExpects.isErroneous()) {
            keyTypeSuperExpects = null;
        }

        // what element type we have based only on the information provided by the sub patterns?
        IJadescriptType valueTypeProvidedBySubs = null;
        IJadescriptType keyTypeProvidedBySubs = null;
        final Maybe<IJadescriptType> maybeMapType = inferMapPatternType(lp);
        if (maybeMapType.isPresent()) {
            final IJadescriptType mapType = maybeMapType.toNullable();
            if (mapType instanceof MapType) {
                valueTypeProvidedBySubs = ((MapType) mapType).getValueType();
                keyTypeProvidedBySubs = ((MapType) mapType).getKeyType();
            }
        }
        if (valueTypeProvidedBySubs != null && valueTypeProvidedBySubs.isErroneous()) {
            //drop it on the floor
            valueTypeProvidedBySubs = null;
        }
        if (keyTypeProvidedBySubs != null && keyTypeProvidedBySubs.isErroneous()) {
            //drop it on the floor
            keyTypeProvidedBySubs = null;
        }


        // what is the element type information eventually forced by the user with specifiers?
        IJadescriptType valueTypeImposedByUser = null;
        IJadescriptType keyTypeImposedByUser = null;
        if (lp.isWithTypeSpecifiers()) {
            InterceptAcceptor valueTypeValidation = new InterceptAcceptor(acceptor);
            module.get(TypeExpressionSemantics.class).validate(of(lp.getValueTypeParameter()), valueTypeValidation);
            valueTypeImposedByUser = module.get(TypeExpressionSemantics.class).toJadescriptType(of(lp.getValueTypeParameter()));
            InterceptAcceptor keyTypeValidation = new InterceptAcceptor(acceptor);
            module.get(TypeExpressionSemantics.class).validate(of(lp.getKeyTypeParameter()), keyTypeValidation);
            keyTypeImposedByUser = module.get(TypeExpressionSemantics.class).toJadescriptType(of(lp.getKeyTypeParameter()));
        }

        // what type of value we conclude we have
        IJadescriptType computedValueType = null;
        if (valueTypeImposedByUser != null) {
            // the user specification overrides everything.
            computedValueType = valueTypeImposedByUser;
        } else {
            if (valueTypeSuperExpects != null && valueTypeProvidedBySubs != null) {
                // we have both info from super pattern and from sub patterns.
                //  they have to be the same.
                if (valueTypeSuperExpects.typeEquals(valueTypeProvidedBySubs)) {
                    computedValueType = valueTypeSuperExpects;
                }
            } else if (valueTypeSuperExpects != null) {
                // we have only information from super patterns, we use that
                computedValueType = valueTypeSuperExpects;
            } else if (valueTypeProvidedBySubs != null) {
                // we have only information from sub patterns, we use that
                computedValueType = valueTypeProvidedBySubs;
            } // else -> we leave computedElementType null.
        }

        // what type of key we conclude we have
        IJadescriptType computedKeyType = null;
        if (keyTypeImposedByUser != null) {
            // the user specification overrides everything.
            computedKeyType = keyTypeImposedByUser;
        } else {
            if (keyTypeSuperExpects != null && keyTypeProvidedBySubs != null) {
                // we have both info from super pattern and from sub patterns.
                //  they have to be the same.
                if (keyTypeSuperExpects.typeEquals(keyTypeProvidedBySubs)) {
                    computedKeyType = keyTypeSuperExpects;
                }
            } else if (keyTypeSuperExpects != null) {
                // we have only information from super patterns, we use that
                computedKeyType = keyTypeSuperExpects;
            } else if (keyTypeProvidedBySubs != null) {
                // we have only information from sub patterns, we use that
                computedKeyType = keyTypeProvidedBySubs;
            } // else -> we leave computedElementType null.
        }

        // what type we conclude we are
        IJadescriptType computedType = typeHelper.MAP.apply(Arrays.asList(
                computedKeyType == null ? typeHelper.ANY : computedKeyType,
                computedValueType == null ? typeHelper.ANY : computedValueType
        ));

        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(ValidationHelper.class).assertion(
                Util.implication(
                        valueTypeImposedByUser == null,
                        computedValueType != null && !computedValueType.isErroneous()
                ),
                "InvalidPatternType",
                "Could not compute a valid map value type; " +
                        "Specify the type of the map pattern using the 'of' clause"
                        + " (expected value type = '" + valueTypeSuperExpects + "'; "
                        + "sub-pattern-inferred value type = '" + valueTypeProvidedBySubs + "'; "
                        + "computed value type = '" + computedValueType + "').",
                of(lp),
                interceptAcceptor
        );

        module.get(ValidationHelper.class).assertion(
                Util.implication(
                        keyTypeImposedByUser == null,
                        computedKeyType != null && !computedKeyType.isErroneous()
                ),
                "InvalidPatternType",
                "Could not compute a valid map key type; " +
                        "Specify the type of the map pattern using the 'of' clause"
                        + " (expected key type = '" + keyTypeSuperExpects + "'; "
                        + "sub-pattern-inferred key type = '" + keyTypeProvidedBySubs + "'; "
                        + "computed key type = '" + computedKeyType + "').",
                of(lp),
                interceptAcceptor
        );

        if (!interceptAcceptor.thereAreErrors() && computedKeyType != null) {
            computedKeyType.validateType(of(lp), interceptAcceptor);
        }

        if (!interceptAcceptor.thereAreErrors() && computedValueType != null) {
            computedValueType.validateType(of(lp), interceptAcceptor);
        }

        if (!interceptAcceptor.thereAreErrors() && computedKeyType != null && computedValueType != null) {
            for (int i = 0; i < terms.size(); i++) {
                Maybe<PatternTerm> term = terms.get(i);
                if (term.isPresent()) {
                    PatternTerm termSafe = term.toNullable();
                    String termBaseId = id + "_" + i;
                    validatePatternTerm(
                            input,
                            computedValueType,
                            termSafe,
                            termBaseId,
                            ptmVarName,
                            acceptor
                    );
                }
            }

            for (Maybe<MapPatternKeyTerm> term : keyTerms) {
                if (term.isPresent()) {
                    MapPatternKeyTerm keyTerm = term.toNullable();
                    validateMapPatterKeyTerm(
                            computedKeyType,
                            keyTerm,
                            acceptor
                    );
                }
            }

            if (lp.isIsPipe() && lp.getRestPattern() != null) {
                String termBaseId = id + "_rest";
                validatePatternTerm(
                        input,
                        computedType,
                        lp.getRestPattern(),
                        termBaseId,
                        ptmVarName,
                        acceptor
                );
            }
        }

        if (interceptAcceptor.thereAreErrors()) {
            return of(expectedType); // to mute validations on the type on the super-patterns
        } else {
            return of(computedType);
        }
    }

    private Maybe<IJadescriptType> validateSetPatternMatch(
            Maybe<PatternMatchRequest> input,
            MapOrSetPattern lp,
            String id,
            String ptmVarName,
            IJadescriptType expectedType,
            ValidationMessageAcceptor acceptor
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        List<Maybe<MapPatternKeyTerm>> keyTerms = toListOfMaybes(of(lp.getTermsKey()));

        //if super pattern thinks we are a set, what type of element does it expect we have?
        IJadescriptType elementTypeSuperExpects = null;
        if (expectedType instanceof SetType) {
            elementTypeSuperExpects = ((SetType) expectedType).getElementType();
        }
        if (elementTypeSuperExpects != null && elementTypeSuperExpects.isErroneous()) {
            elementTypeSuperExpects = null;
        }

        // what element type we have based only on the information provided by the sub patterns?
        IJadescriptType elementTypeProvidedBySubs = null;
        final Maybe<IJadescriptType> maybeSetType = inferSetPatternType(lp);
        if (maybeSetType.isPresent()) {
            final IJadescriptType setType = maybeSetType.toNullable();
            if (setType instanceof SetType) {
                elementTypeProvidedBySubs = ((SetType) setType).getElementType();
            }
        }
        if (elementTypeProvidedBySubs != null && elementTypeProvidedBySubs.isErroneous()) {
            //drop it on the floor
            elementTypeProvidedBySubs = null;
        }

        // what is the element type information eventually forced by the user with specifiers?
        IJadescriptType elementTypeImposedByUser = null;
        if (lp.isWithTypeSpecifiers()) {
            InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
            module.get(TypeExpressionSemantics.class).validate(of(lp.getKeyTypeParameter()), typeValidation);
            if (!typeValidation.thereAreErrors()) {
                elementTypeImposedByUser = module.get(TypeExpressionSemantics.class)
                        .toJadescriptType(of(lp.getKeyTypeParameter()));
            }
        }

        // what type of element we conclude we have
        IJadescriptType computedElementType = null;
        if (elementTypeImposedByUser != null) {
            // the user specification overrides everything.
            computedElementType = elementTypeImposedByUser;
        } else {
            if (elementTypeSuperExpects != null && elementTypeProvidedBySubs != null) {
                // we have both info from super pattern and from sub patterns.
                //  they have to be the same.
                if (elementTypeSuperExpects.typeEquals(elementTypeProvidedBySubs)) {
                    computedElementType = elementTypeSuperExpects;
                }
            } else if (elementTypeSuperExpects != null) {
                // we have only information from super patterns, we use that
                computedElementType = elementTypeSuperExpects;
            } else if (elementTypeProvidedBySubs != null) {
                // we have only information from sub patterns, we use that
                computedElementType = elementTypeProvidedBySubs;
            } // else -> we leave computedElementType null.
        }

        // what type we conclude we are
        IJadescriptType computedType = typeHelper.SET.apply(Arrays.asList(
                computedElementType == null ? typeHelper.ANY : computedElementType
        ));


        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(ValidationHelper.class).assertion(
                Util.implication(
                        elementTypeImposedByUser == null,
                        computedElementType != null && !computedElementType.isErroneous()
                ),
                "InvalidPatternType",
                "Could not compute a valid set element type; " +
                        "Specify the type of the set pattern by means of the 'of' clause"
                        + " (expected element type = '" + elementTypeSuperExpects + "'; "
                        + "sub-pattern-inferred element type = '" + elementTypeProvidedBySubs + "'; "
                        + "computed element type = '" + computedElementType + "').",
                of(lp),
                interceptAcceptor
        );

        if (!interceptAcceptor.thereAreErrors() && computedElementType != null) {
            computedElementType.validateType(of(lp), interceptAcceptor);
        }

        if (!interceptAcceptor.thereAreErrors() && computedElementType != null) {
            for (Maybe<MapPatternKeyTerm> term : keyTerms) {
                if (term.isPresent()) {
                    MapPatternKeyTerm keyTerm = term.toNullable();
                    validateMapPatterKeyTerm(
                            computedElementType,
                            keyTerm,
                            acceptor
                    );
                }
            }

            if (lp.isIsPipe() && lp.getRestPattern() != null) {
                String termBaseId = id + "_rest";
                validatePatternTerm(
                        input,
                        computedType,
                        lp.getRestPattern(),
                        termBaseId,
                        ptmVarName,
                        acceptor
                );
            }
        }

        if (interceptAcceptor.thereAreErrors()) {
            return of(expectedType); // to mute validations on the type on the super-patterns
        } else {
            return of(computedType);
        }
    }

    private void validateMapPatterKeyTerm(
            IJadescriptType expectedKeyType,
            MapPatternKeyTerm keyTerm,
            ValidationMessageAcceptor acceptor
    ) {
        if (keyTerm.getText() != null) {
            module.get(ValidationHelper.class).assertExpectedType(
                    expectedKeyType,
                    module.get(TypeHelper.class).TEXT,
                    "UnexpectedKeyTermType",
                    of(keyTerm),
                    acceptor
            );
        } else if (keyTerm.getNumber() != null) {
            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).jtFromClass(Number.class),
                    expectedKeyType,
                    "UnexpectedKeyTermType",
                    of(keyTerm),
                    acceptor
            );
        } else if (keyTerm.getBoolean() != null) {
            module.get(ValidationHelper.class).assertExpectedType(
                    expectedKeyType,
                    module.get(TypeHelper.class).BOOLEAN,
                    "UnexpectedKeyTermType",
                    of(keyTerm),
                    acceptor
            );
        } else if (keyTerm.getIdentifier() != null) {
            Maybe<VirtualIdentifier> virtID = of(new VirtualIdentifier(keyTerm.getIdentifier(), keyTerm));
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(SingleIdentifierExpressionSemantics.class).validate(virtID, interceptAcceptor);
            if (!interceptAcceptor.thereAreErrors()) {
                IJadescriptType idType = module.get(SingleIdentifierExpressionSemantics.class).inferType(virtID);

                module.get(ValidationHelper.class).assertExpectedType(
                        expectedKeyType,
                        idType,
                        "UnexpectedKeyTermType",
                        of(keyTerm),
                        acceptor
                );

            }
        }
    }

    private void validatePatternTerm(
            Maybe<PatternMatchRequest> input,
            IJadescriptType expectedType,
            PatternTerm pt,
            String id,
            String ptmVarName,
            ValidationMessageAcceptor acceptor
    ) {
        if (pt.getBoolean() != null) {
            module.get(ValidationHelper.class).assertExpectedType(
                    expectedType,
                    module.get(TypeHelper.class).BOOLEAN,
                    "UnexpectedTermType",
                    of(pt),
                    acceptor
            );
        } else if (pt.getNumber() != null) {
            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).jtFromClass(Number.class),
                    expectedType,
                    "UnexpectedTermType",
                    of(pt),
                    acceptor
            );
        } else if (pt.getText() != null) {
            module.get(ValidationHelper.class).assertExpectedType(
                    expectedType,
                    module.get(TypeHelper.class).TEXT,
                    "UnexpectedTermType",
                    of(pt),
                    acceptor
            );
        } else if (pt.getSubPattern() != null) {
            validatePatternMatchDetails(
                    input,
                    of(pt.getSubPattern()),
                    id + "_",
                    ptmVarName,
                    expectedType,
                    acceptor
            );
        }// else if (pt.isUnderscore()) ==> ok
    }

    private Maybe<IJadescriptType> validateIdentifierPatternMatch(
            Maybe<PatternMatchRequest> input,
            IJadescriptType expectedType,
            String identifier,
            EObject pt,
            ValidationMessageAcceptor acceptor
    ) {
        Maybe<VirtualIdentifier> virtID = of(new VirtualIdentifier(identifier, pt));

        if (module.get(SingleIdentifierExpressionSemantics.class).resolves(virtID)) {
            IJadescriptType varType = module.get(SingleIdentifierExpressionSemantics.class).inferType(virtID);
            module.get(ValidationHelper.class).assertExpectedType(expectedType, varType, "UnexpectedTermType", of(pt), acceptor);
            return of(varType);

        } else {
            module.get(ValidationHelper.class).assertion(
                    input.__(PatternMatchRequest::canDeconstruct).extract(nullAsFalse),
                    "InvalidReference",
                    "cannot resolve name: " + identifier,
                    of(pt),
                    acceptor
            );

            if (input.__(PatternMatchRequest::canDeconstruct).extract(nullAsFalse)) {

                String localClassName = "__PatternMatcher" + input
                        .__(ProxyEObject::getProxyEObject)
                        .__(Object::hashCode).orElse(0);

                module.get(ContextManager.class).currentScope().addNamedElement(
                        new PatternMatchAutoDeclaredVariable(
                                identifier,
                                expectedType,
                                localClassName + "_obj."
                        )
                );

                if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
                    acceptor.acceptInfo(
                            "Inferred declaration; type: " + expectedType.getJadescriptName(),
                            pt,
                            null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            ISSUE_CODE_PREFIX + "Info"
                    );
                }
            }
            return of(expectedType);
        }

    }


    public Maybe<IJadescriptType> inferPatternType(Pattern p) {
        if (p.getListPattern() != null) {
            return inferListPatternType(p.getListPattern());
        } else if (p.getMapOrSetPattern() != null) {
            if (p.getMapOrSetPattern().isIsMap() || p.getMapOrSetPattern().isIsMapT()) {
                return inferMapPatternType(p.getMapOrSetPattern());
            } else {
                return inferSetPatternType(p.getMapOrSetPattern());
            }
        } else if (p.getStructPattern() != null) {
            return inferStructPatternType(p.getStructPattern());
        } else if (p.getTuplePattern() != null) {
            return inferTuplePatternType(p.getTuplePattern());
        } else {
            return nothing();
        }
    }

    private Maybe<IJadescriptType> inferStructPatternType(StructurePattern p) {
        if (p == null || p.getName() == null) {
            return nothing();
        }

        String patternName = p.getName();

        if (!p.isWithParentheses()) {
            if (p.isWithAs() && p.getExpectedType() != null) {
                return of(module.get(TypeExpressionSemantics.class).toJadescriptType(of(p.getExpectedType())));
            } else {
                return inferTypeIdentifierPattern(patternName, p);
            }
        }

        Optional<? extends CallableSymbol> methods = module.get(ContextManager.class).currentContext().searchAs(
                CallableSymbol.Searcher.class,
                searcher -> searcher.searchCallable(
                        patternName,
                        null,
                        (s, n) -> s == p.getTerms().size(),
                        (s, t) -> s == p.getTerms().size()
                )
        ).findFirst();

        if (methods.isPresent()) {
            CallableSymbol m = methods.get();
            return of(m.returnType());
        }
        return nothing();
    }

    private Maybe<IJadescriptType> inferTuplePatternType(TuplePattern p) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (p.isWithTypeSpecifiers()) {
            return of(typeHelper.TUPLE.apply(
                    p.getTypeSpecifiers().stream()
                            .map(Maybe::of)
                            .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                            .collect(Collectors.toList())
            ));
        }

        List<Maybe<PatternTerm>> ps = toListOfMaybes(of(p.getTerms()));
        List<Maybe<IJadescriptType>> termTypes = new ArrayList<>();

        for (Maybe<PatternTerm> term : ps) {
            Maybe<IJadescriptType> termType = term.isPresent() ? inferTypePatternTerm(term.toNullable()) : nothing();
            termTypes.add(termType);
        }

        if (termTypes.stream().allMatch(Maybe::isPresent)) {
            return of(typeHelper.TUPLE.apply(termTypes.stream().map(Maybe::toNullable).collect(Collectors.toList())));
        } else {
            return nothing();
        }
    }

    private Maybe<IJadescriptType> inferListPatternType(ListPattern p) {
        if (p.isWithTypeSpecifier()) {
            return of(module.get(TypeHelper.class).LIST.apply(Arrays.asList(
                    module.get(TypeExpressionSemantics.class).toJadescriptType(of(p.getTypeParameter()))
            )));
        }
        Maybe<IJadescriptType> result = nothing();

        if (p.isIsPipe() && p.getRestPattern() != null) {
            Maybe<IJadescriptType> listType = inferTypePatternTerm(p.getRestPattern());
            if (listType.isPresent() && listType.isInstanceOf(ListType.class)) {
                result = listType.__(lt -> (ListType) lt).__(ListType::getElementType);
            }
        }

        List<Maybe<PatternTerm>> ps = toListOfMaybes(of(p.getTerms()));
        for (Maybe<PatternTerm> term : ps) {
            if (term.isPresent()) {
                Maybe<IJadescriptType> termType = inferTypePatternTerm(term.toNullable());
                if (termType.isPresent()) {
                    if (result.isPresent()) {
                        result = of(module.get(TypeHelper.class).getLUB(result.toNullable(), termType.toNullable()));
                    } else {
                        result = termType;
                    }
                }
            }
        }

        if (result.isPresent()) {
            return of(module.get(TypeHelper.class).LIST.apply(
                    Arrays.asList(result.toNullable())
            ));
        } else {
            return nothing();
        }

    }

    private Maybe<IJadescriptType> inferMapPatternType(MapOrSetPattern p) {
        if (p.isWithTypeSpecifiers()) {
            return of(module.get(TypeHelper.class).MAP.apply(Arrays.asList(
                    module.get(TypeExpressionSemantics.class).inferType(of(p.getKeyTypeParameter())),
                    module.get(TypeExpressionSemantics.class).inferType(of(p.getValueTypeParameter()))
            )));
        }

        Maybe<IJadescriptType> valueType = nothing();
        Maybe<IJadescriptType> keyType = nothing();
        List<Maybe<PatternTerm>> ps = toListOfMaybes(of(p.getTermsValue()));
        List<Maybe<MapPatternKeyTerm>> keys = toListOfMaybes(of(p.getTermsKey()));

        if (p.isIsPipe() && p.getRestPattern() != null) {
            Maybe<IJadescriptType> mapType = inferTypePatternTerm(p.getRestPattern());
            if (mapType.isPresent() && mapType.isInstanceOf(MapType.class)) {
                valueType = mapType.__(mt -> (MapType) mt).__(MapType::getValueType);
                keyType = mapType.__(mt -> (MapType) mt).__(MapType::getKeyType);
            }
        }

        for (int i = 0; i < ps.size(); i++) {
            Maybe<PatternTerm> term = ps.get(i);
            Maybe<MapPatternKeyTerm> key = keys.get(i);
            if (term.isPresent()) {
                Maybe<IJadescriptType> termType = inferTypePatternTerm(term.toNullable());
                if (termType.isPresent()) {
                    if (valueType.isPresent()) {
                        valueType = of(module.get(TypeHelper.class).getLUB(valueType.toNullable(), termType.toNullable()));
                    } else {
                        valueType = termType;
                    }
                }
            }
            if (key.isPresent()) {
                Maybe<IJadescriptType> keyTermType = inferTypeMapPatternKeyTerm(key.toNullable());
                if (keyTermType.isPresent()) {
                    if (keyType.isPresent()) {
                        keyType = of(module.get(TypeHelper.class).getLUB(keyType.toNullable(), keyTermType.toNullable()));
                    } else {
                        keyType = keyTermType;
                    }
                }
            }
        }


        if (keyType.isPresent() && valueType.isPresent()) {
            return of(module.get(TypeHelper.class).MAP.apply(Arrays.asList(
                    keyType.toNullable(), valueType.toNullable()
            )));
        } else {
            return nothing();
        }
    }

    private Maybe<IJadescriptType> inferSetPatternType(MapOrSetPattern p) {
        if (p.isWithTypeSpecifiers()) {
            return of(module.get(TypeHelper.class).SET.apply(Arrays.asList(
                    module.get(TypeExpressionSemantics.class).inferType(of(p.getKeyTypeParameter()))
            )));
        }
        Maybe<IJadescriptType> elementType = nothing();
        List<Maybe<MapPatternKeyTerm>> ps = toListOfMaybes(of(p.getTermsKey()));

        if (p.isIsPipe() && p.getRestPattern() != null) {
            Maybe<IJadescriptType> setType = inferTypePatternTerm(p.getRestPattern());
            if (setType.isPresent() && elementType.isInstanceOf(SetType.class)) {
                elementType = setType.__(mt -> (SetType) mt).__(SetType::getElementType);
            }
        }
        for (Maybe<MapPatternKeyTerm> term : ps) {
            if (term.isPresent()) {
                Maybe<IJadescriptType> termType = inferTypeMapPatternKeyTerm(term.toNullable());
                if (termType.isPresent()) {
                    if (elementType.isPresent()) {
                        elementType = of(module.get(TypeHelper.class).getLUB(
                                elementType.toNullable(),
                                termType.toNullable()
                        ));
                    } else {
                        elementType = termType;
                    }
                }
            }

        }
        if (elementType.isPresent()) {
            return of(module.get(TypeHelper.class).SET.apply(Arrays.asList(
                    elementType.toNullable()
            )));
        } else {
            return nothing();
        }
    }

    private Maybe<IJadescriptType> inferTypeMapPatternKeyTerm(MapPatternKeyTerm pt) {
        if (pt == null) {
            return nothing();
        }
        if (pt.getBoolean() != null) {
            return of(module.get(TypeHelper.class).BOOLEAN);
        } else if (pt.getNumber() != null) {
            return of(module.get(TypeHelper.class).jtFromClass(
                    LiteralExpressionSemantics.getTypeOfNumberLiteral(module, of(pt.getNumber()))
            ));
        } else if (pt.getText() != null) {
            return of(module.get(TypeHelper.class).TEXT);
        } else if (pt.getIdentifier() != null) {
            return inferTypeIdentifierPattern(pt.getIdentifier(), pt);
        } else {
            return nothing();
        }
    }

    private Maybe<IJadescriptType> inferTypePatternTerm(PatternTerm pt) {
        if (pt == null) {
            return nothing();
        }
        if (pt.getBoolean() != null) {
            return of(module.get(TypeHelper.class).BOOLEAN);
        } else if (pt.getNumber() != null) {
            return of(module.get(TypeHelper.class).jtFromClass(
                    LiteralExpressionSemantics.getTypeOfNumberLiteral(module, of(pt.getNumber()))
            ));
        } else if (pt.getText() != null) {
            return of(module.get(TypeHelper.class).TEXT);
        } else if (pt.getSubPattern() != null) {
            return inferPatternType(pt.getSubPattern());
        } else { // if (pt.isUnderscore()) or else...
            if (pt.getExpectedType() != null) {
                return of(module.get(TypeExpressionSemantics.class)
                        .toJadescriptType(of(pt.getExpectedType())));
            } else {
                return nothing();
            }
        }
    }

    private Maybe<IJadescriptType> inferTypeIdentifierPattern(String identifier, EObject pt) {
        if (identifier == null) {
            return nothing();
        }

        Maybe<VirtualIdentifier> virtID = of(new VirtualIdentifier(identifier, pt));

        if (module.get(SingleIdentifierExpressionSemantics.class).resolves(virtID)) {
            return of(module.get(SingleIdentifierExpressionSemantics.class).inferType(virtID));
        } else {
            return of(module.get(TypeHelper.class).NOTHING);
        }
    }

}
