package it.unipr.ailab.jadescript.semantics;


public class PatternMatchingSemantics
//        extends Semantics<PatternMatchRequest>
{
    private final SemanticsModule module;


    public PatternMatchingSemantics(SemanticsModule semanticsModule) {
        this.module = semanticsModule;
    }




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
