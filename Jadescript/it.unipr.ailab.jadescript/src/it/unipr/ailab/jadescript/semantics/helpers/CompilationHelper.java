package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.CodeBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.Statement;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.jvmmodel.JadescriptCompilerUtils;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.comment.MultilineCommentWriter;
import it.unipr.ailab.sonneteer.expression.LambdaWithBlockWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.common.types.JvmExecutable;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.THIS;
import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class CompilationHelper implements IQualifiedNameProvider {

    private final static WriterFactory w = WriterFactory.getInstance();

    private final SemanticsModule module;


    public CompilationHelper(SemanticsModule module) {
        this.module = module;
    }


    @NotNull
    public static String extractOntologyVarName(
        JvmTypeReference jvmTypeReference
    ) {
        return SemanticsConsts.ONTOLOGY_VAR_NAME + "__" +
            jvmTypeReference.getQualifiedName('.').replaceAll("\\.", "_");
    }


    public static String extractOntologyVarName(
        IJadescriptType usedOntologyType
    ) {
        return extractOntologyVarName(usedOntologyType.asJvmTypeReference());
    }


    public static String compileDefaultValueForType(
        final IJadescriptType jadescriptType
    ) {
        if (jadescriptType instanceof EmptyCreatable) {
            return ((EmptyCreatable) jadescriptType).compileNewEmptyInstance();
        } else {
            return "null";
        }
    }


    public static Maybe<String> sourceToLocationText(
        Maybe<? extends EObject> input
    ) {
        return Util.extractEObject(input).__(eObject -> {
            final ICompositeNode node = NodeModelUtils.getNode(eObject);
            int startLine = node.getStartLine();
            int endLine = node.getEndLine();
            if (startLine == endLine) {
                return "at line " + startLine;
            } else {
                return "from line " + startLine + " to line " + endLine;
            }
        });
    }


    public static Maybe<String> sourceToTextAny(Maybe<?> input) {
        if (input.isNothing()) {
            return nothing();
        }

        final Object o = input.toNullable();
        if (o instanceof EObject) {
            return sourceToText(some(((EObject) o)));
        } else {
            return nothing();
        }
    }


    public static Maybe<String> sourceToText(Maybe<? extends EObject> input) {
        return Util.extractEObject(input).__(eObject -> {
            final ICompositeNode node = NodeModelUtils.getNode(eObject);
            return node.getText();
        });
    }


    public static MultilineCommentWriter inputStatementComment(
        Maybe<Statement> input,
        String prefix
    ) {
        final MultilineCommentWriter multiComment = w.multiComment(
            prefix + " " + sourceToLocationText(input).orElse("[Unknown " +
                "location in source]")
        );
        String source = sourceToText(input).orElse("[unknown source]");
        for (String line : source.split("\\R")) {
            multiComment.addLine(line);
        }
        return multiComment;
    }


    public static Function<List<String>, String>
    addEnvParameterByArity(Function<List<String>, String> callByArity) {
        return (argsByArity) -> {
            List<String> l = new ArrayList<>(argsByArity);
            l.add(0, CompilationHelper.compileEnvArgument());
            return callByArity.apply(l);
        };
    }


    public static Function<Map<String, String>, String>
    addEnvParameterByName(
        Function<Map<String, String>, String> callByNames
    ) {
        return (argsByNames) -> {
            Map<String, String> m = new HashMap<>(argsByNames);
            m.put(
                SemanticsConsts.AGENT_ENV,
                CompilationHelper.compileEnvArgument()
            );
            return callByNames.apply(m);
        };
    }


    public static BiFunction<String, List<String>, String>
    addEnvParameterByArity(
        BiFunction<String, List<String>, String> callByArity
    ) {
        return (rec, argsByArity) -> {
            List<String> l = new ArrayList<>(argsByArity);
            l.add(0, CompilationHelper.compileEnvArgument());
            return callByArity.apply(rec, l);
        };
    }


    public static BiFunction<String, Map<String, String>, String>
    addEnvParameterByName(
        BiFunction<String, Map<String, String>, String> callByNames
    ) {
        return (rec, argsByNames) -> {
            Map<String, String> m = new HashMap<>(argsByNames);
            m.put(
                SemanticsConsts.AGENT_ENV,
                CompilationHelper.compileEnvArgument()
            );
            return callByNames.apply(rec, m);
        };
    }


    public static String compileAgentReference(
        Maybe<? extends EObject> container
    ) {
        return Util.getOuterClassThisReference(container).orElse(THIS)
            + "." + compileAgentReference();
    }


    public static String compileAgentReference() {
        return SemanticsConsts.AGENT_ENV + ".getAgent()";
    }


    public static String compileEnvArgument(
        Maybe<? extends EObject> container
    ) {
        return compileAgentReference(container) + ".toEnv()";
    }


    public static String compileEnvArgument() {
        return compileAgentReference() + ".toEnv()";
    }


    public void createAndSetBody(
        JvmExecutable container,
        Consumer<SourceCodeBuilder> initializer
    ) {
        module.get(JvmTypesBuilder.class).setBody(
            container,
            new StringConcatenationClient() {
                @Override
                protected void appendTo(TargetStringConcatenation target) {
                    SourceCodeBuilder sourceCodeBuilder =
                        new SourceCodeBuilder("");
                    initializer.accept(sourceCodeBuilder);
                    target.append(sourceCodeBuilder);
                    target.newLineIfNotEmpty();
                }
            }
        );
    }


    public void createAndSetInitializer(
        JvmField field,
        Consumer<SourceCodeBuilder> init
    ) {
        module.get(JvmTypesBuilder.class).setInitializer(
            field,
            new StringConcatenationClient() {
                @Override
                protected void appendTo(TargetStringConcatenation target) {
                    SourceCodeBuilder sourceCodeBuilder =
                        new SourceCodeBuilder("");
                    init.accept(sourceCodeBuilder);
                    target.append(sourceCodeBuilder);
                }
            }
        );
    }


    public List<String> adaptAndCompileRValueList(
        List<String> compiledArgs,
        List<IJadescriptType> argTypes,
        List<IJadescriptType> destinationTypes
    ) {
        List<String> result = new ArrayList<>();
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        for (int i = 0; i < Math.min(
            compiledArgs.size(),
            Math.min(argTypes.size(), destinationTypes.size())
        ); i++) {
            result.add(
                typeHelper.compileWithEventualImplicitConversions(
                    compiledArgs.get(i),
                    argTypes.get(i),
                    destinationTypes.get(i)
                )
            );
        }
        return result;
    }


    /**
     * Compiles the expression into a Supplier lambda, which contains all the
     * expression's generated auxiliary statements, and it returns with the
     * value which is used to provide the value.
     * <p></p>
     * For example, let's say we need to pass a super-argument with the
     * string "Hello".
     * Using this to compile the arguments of the super constructor generates:
     * <p></p>
     * {@code super(((Supplier<String>)()->{return "Hello";}).get());}.
     * <p></p>
     * This is important in order to compile expressions as super-arguments
     * that may need to generate auxiliary statements in order to work
     * correctly, or to initialize fields in the same way.
     *
     * @param expr            the expression
     * @param beforeExpr      the state before the expression evaluation
     * @param inferredType    the type inferred from the expression, or null to
     *                        let this method infer it
     * @param destinationType the desired type for the result (to apply
     *                        eventual implicit conversions), or null, to use
     *                        {@code inferredType} and to apply no conversion
     * @return the overall compiled lambda expression
     */
    public String compileRValueAsLambdaSupplier(
        Maybe<RValueExpression> expr,
        StaticState beforeExpr,
        @Nullable IJadescriptType inferredType,
        @Nullable IJadescriptType destinationType
    ) {
        final LambdaWithBlockWriter lambda = w.blockLambda();
        final IJadescriptType type;
        final String returnExpr;

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final IJadescriptType startType;
        if (inferredType == null) {
            startType = rves.inferType(expr, beforeExpr);
        } else {
            startType = inferredType;
        }

        final String compiled = rves.compile(
            expr,
            beforeExpr,
            lambda.getBody()::add
        );

        if (destinationType == null) {
            returnExpr = compiled;
            type = startType;
        } else {
            returnExpr =
                module.get(TypeHelper.class)
                    .compileWithEventualImplicitConversions(
                        compiled,
                        startType,
                        destinationType
                    );
            type = destinationType;
        }

        lambda.getBody().addStatement(w.returnStmnt(w.expr(returnExpr)));
        SourceCodeBuilder scb = new SourceCodeBuilder();
        scb.add("((java.util.function.Supplier<")
            .add(type.compileToJavaTypeReference())
            .add(">)")
            .add(lambda)
            .add(").get()");
        return scb.toString();
    }


    public PSR<SourceCodeBuilder> compileBlockToNewSCB(
        StaticState initialState,
        Maybe<CodeBlock> cb
    ) {
        SourceCodeBuilder ssb = new SourceCodeBuilder("");
        final PSR<BlockWriter> blockPSR =
            module.get(BlockSemantics.class).compile(cb, initialState);
        final StaticState afterBlock = blockPSR.state();
        final BlockWriter result = blockPSR.result();
        result.writeSonnet(ssb);
        return PSR.psr(ssb, afterBlock);
    }


    public String compileDefaultValueForType(
        Maybe<TypeExpression> typeExpr
    ) {
        return compileDefaultValueForType(
            module.get(TypeExpressionSemantics.class)
                .toJadescriptType(typeExpr)
        );
    }


    public LightweightTypeReference toLightweightTypeReference(
        IJadescriptType type,
        EObject container
    ) {
        return module.get(JadescriptCompilerUtils.class)
            .toLightweightTypeReference(type.asJvmTypeReference(), container);
    }


    @Override
    public QualifiedName getFullyQualifiedName(EObject eObject) {
        if (eObject == null)
            //noinspection ReturnOfNull
            return null;
        return module.get(IQualifiedNameProvider.class)
            .getFullyQualifiedName(eObject);
    }


    @SuppressWarnings("unused")
    public QualifiedName apply(EObject arg0) {
        return module.get(IQualifiedNameProvider.class).getFullyQualifiedName(
            arg0);
    }


}
